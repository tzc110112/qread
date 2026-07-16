package web.service

import book.model.BookSource
import book.model.SearchBook
import book.util.GSON
import book.webBook.WBook
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory
import web.mapper.BooklistMapper
import web.model.BaseSource
import web.model.Booklist
import web.model.SourceCrawlTask
import web.model.Users
import web.util.mapper.mapper
import web.util.read.updatebook
import kotlin.concurrent.thread

object AutoCrawlService {
    private val logger = LoggerFactory.getLogger(AutoCrawlService::class.java)

    private const val CRAWL_DELAY_MS = 500L
    private const val PAGE_DELAY_MS = 1000L
    private const val MAX_CONCURRENT = 3
    private const val MAX_PAGES_PER_CRAWL = 50

    private val semaphore = Semaphore(MAX_CONCURRENT)

    private val runningTasks = mutableSetOf<String>()

    fun startFullCrawl(sourceUrl: String, user: Users) {
        val key = "${user.id}::${sourceUrl}"
        if (runningTasks.contains(key)) {
            logger.info("书源[$sourceUrl]采集任务已在运行")
            return
        }
        runningTasks.add(key)
        thread {
            runBlocking {
                try {
                    doFullCrawl(sourceUrl, user)
                } catch (e: Exception) {
                    logger.error("书源[$sourceUrl]采集异常", e)
                } finally {
                    runningTasks.remove(key)
                }
            }
        }
    }

    private suspend fun doFullCrawl(sourceUrl: String, user: Users) {
        val map = mapper.get()
        val taskMapper = map.sourceCrawlTaskMapper
        val booklistMapper = map.booklistMapper

        val source = getSource(sourceUrl, user) ?: run {
            logger.warn("书源[$sourceUrl]不存在，跳过采集")
            return
        }

        val bs = BookSource.fromJson(source.json).getOrNull() ?: run {
            logger.warn("书源[$sourceUrl]JSON解析失败")
            return
        }

        // 创建/获取采集任务记录
        var task = taskMapper.getByUserAndSource(user.id!!, sourceUrl)
        if (task != null && task.status == 2) {
            logger.info("书源[$sourceUrl]已完成采集，跳过")
            return
        }

        val isNewTask = task == null
        if (isNewTask) {
            task = SourceCrawlTask().create().apply {
                this.userid = user.id
                this.sourceUrl = sourceUrl
                this.sourceName = bs.bookSourceName
                this.status = 1
            }
        } else {
            task.status = 1
            task.errorMsg = null
        }
        task.lastUpdateTime = System.currentTimeMillis()
        if (isNewTask) taskMapper.insert(task) else taskMapper.updateById(task)

        logger.info("开始采集书源[${bs.bookSourceName}]($sourceUrl)")

        try {
            // 解析所有分类/发现URL
            val exploreUrls = resolveExploreUrls(task, bs, user)
            if (exploreUrls.isEmpty()) {
                logger.warn("书源[${bs.bookSourceName}]没有配置发现规则，无法采集")
                task.status = -1
                task.errorMsg = "书源未配置发现规则(ruleExplore)"
                taskMapper.updateById(task)
                return
            }

            task.totalExplore = exploreUrls.size
            task.exploreUrls = GSON.toJson(exploreUrls)
            taskMapper.updateById(task)

            var totalAdded = task.totalBooks
            var hasMore = true

            // 逐类遍历
            for (idx in task.currentExploreIdx until exploreUrls.size) {
                val exploreUrl = exploreUrls[idx]
                // 如果是恢复任务，从之前保存的页码继续；新分类从0开始
                val startPage = if (idx == task.currentExploreIdx) task.currentPage else 0
                task.currentExploreIdx = idx
                taskMapper.updateById(task)

                var page = startPage

                while (hasMore && page < MAX_PAGES_PER_CRAWL) {
                    val books = explorePage(bs, exploreUrl, page, user) ?: break
                    if (books.isEmpty()) break

                    for (searchBook in books) {
                        if (searchBook.bookUrl.isBlank() || searchBook.name.isBlank()) continue

                        // 检查是否已在书架
                        val existing = booklistMapper.getbook(user.id!!, searchBook.bookUrl)
                        if (existing != null) continue

                        // 添加到书架
                        runCatching {
                            addBookToShelf(searchBook, source, user, map.booklistMapper)
                            totalAdded++
                            task.totalBooks = totalAdded
                            taskMapper.updateById(task)
                        }.onFailure { e ->
                            logger.warn("添加书籍[${searchBook.name}]失败: ${e.message}")
                        }

                        delay(CRAWL_DELAY_MS)
                    }

                    page++
                    task.currentPage = page
                    taskMapper.updateById(task)
                    delay(PAGE_DELAY_MS)
                }

                // 重置当前页，为下一个分类做准备
                task.currentPage = 0
            }

            task.status = 2
            task.lastUpdateTime = System.currentTimeMillis()
            taskMapper.updateById(task)
            logger.info("书源[${bs.bookSourceName}]采集完成，共添加${totalAdded}本书")
        } catch (e: Exception) {
            logger.error("书源[${bs.bookSourceName}]采集失败", e)
            task.status = -1
            task.errorMsg = e.message ?: "未知错误"
            task.lastUpdateTime = System.currentTimeMillis()
            taskMapper.updateById(task)
        }
    }

    /**
     * 解析书源的发现规则，获取所有分类页URL
     */
    private fun resolveExploreUrls(task: SourceCrawlTask, bs: BookSource, user: Users): List<String> {
        // 如果已有缓存的URL列表，直接使用
        if (!task.exploreUrls.isNullOrBlank()) {
            val cached = runCatching {
                GSON.fromJson<List<String>>(task.exploreUrls, object : TypeToken<List<String>>() {}.type)
            }.getOrNull()
            if (cached != null && cached.isNotEmpty()) return cached
        }

        // 如果书源配置了固定的发现规则URL
        val urls = mutableListOf<String>()

        // 情况1: 直接有 ruleExplore 的 exploreUrl
        if (!bs.exploreUrl.isNullOrBlank()) {
            // 尝试分割多个探索URL
            val parts = bs.exploreUrl!!.split("\n")
            urls.addAll(parts.filter { it.isNotBlank() })
        }

        // 回退: 使用书源URL作为探索入口
        if (urls.isEmpty()) {
            urls.add(bs.bookSourceUrl)
        }

        return urls
    }

    /**
     * 探索某一页，获取书籍列表
     */
    private suspend fun explorePage(
        bs: BookSource,
        exploreUrl: String,
        page: Int,
        user: Users
    ): List<SearchBook>? {
        return withTimeoutOrNull(30000) {
            runCatching {
                val wBook = WBook(bs, debugLog = false, userid = user.id!!)
                wBook.exploreBook(exploreUrl, page)
            }.onFailure { e ->
                logger.warn("探索页[$exploreUrl] page=$page 失败: ${e.message}")
            }.getOrNull()
        }
    }

    /**
     * 添加书籍到书架
     */
    private fun addBookToShelf(
        searchBook: SearchBook,
        source: BaseSource,
        user: Users,
        booklistMapper: BooklistMapper
    ) {
        val booklist = Booklist.tobooklist(searchBook, user.id!!)
        // 获取详细信息
        val bookInfo = runBlocking {
            withTimeoutOrNull(15000) {
                runCatching {
                    val wBook = WBook(BookSource.fromJson(source.json).getOrNull() ?: return@runCatching null, debugLog = false, userid = user.id)
                    wBook.getBookInfo(searchBook.bookUrl, canReName = false)
                }.getOrNull()
            }
        }

        if (bookInfo != null) {
            booklist.bookto(bookInfo, canchangeindex = true)
            booklist.origin = source.bookSourceUrl
            booklist.originName = source.bookSourceName

            // 异步获取章节信息
            thread {
                runBlocking {
                    runCatching {
                        val s = BaseSource(source.bookSourceUrl, source.bookSourceName, json = source.json)
                        updatebook(booklist, s, user)
                    }
                }
            }
        } else {
            // 降级：只使用搜索到的基本信息
            booklist.apply {
                origin = source.bookSourceUrl
                originName = source.bookSourceName
                latestChapterTitle = searchBook.latestChapterTitle
            }
        }

        booklistMapper.insert(booklist)
    }

    private fun getSource(sourceUrl: String, user: Users): BaseSource? {
        val map = mapper.get()
        return if (user.source == 2) {
            map.userBookSourceMapper.getBookSource(sourceUrl, user.id!!)?.toBaseSource()
        } else {
            map.bookSourceMapper.getBookSource(sourceUrl)?.toBaseSource()
        }
    }
}
