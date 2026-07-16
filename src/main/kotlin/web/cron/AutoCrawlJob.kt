package web.cron

import kotlinx.coroutines.runBlocking
import org.noear.solon.annotation.Inject
import org.noear.solon.scheduling.annotation.Scheduled
import org.slf4j.LoggerFactory
import web.mapper.SourceCrawlTaskMapper
import web.service.AutoCrawlService
import web.util.mapper.mapper

/**
 * 定时恢复未完成的采集任务（每 10 分钟检查一次）
 */
@Scheduled(fixedRate = 1000 * 60 * 10)
class AutoCrawlJob : Runnable {
    private val logger = LoggerFactory.getLogger(AutoCrawlJob::class.java)

    @Inject(value = "\${admin.cron:true}", autoRefreshed = true)
    var cron: Boolean = true

    @Inject(value = "\${auto.crawl:true}", autoRefreshed = true)
    var autoCrawl: Boolean = true

    override fun run() = runBlocking {
        if (!cron || !autoCrawl) return@runBlocking

        runCatching {
            val tasks = mapper.get().sourceCrawlTaskMapper.getPendingTasks()
            if (tasks.isEmpty()) return@runBlocking

            logger.info("发现${tasks.size}个未完成的采集任务，恢复采集")

            tasks.forEach { task ->
                if (task.userid != null && task.sourceUrl != null) {
                    val user = mapper.get().usersMapper.getUser(task.userid!!)
                    if (user != null) {
                        logger.info("恢复采集任务: ${task.sourceName}(${task.sourceUrl})")
                        AutoCrawlService.startFullCrawl(task.sourceUrl!!, user)
                    }
                }
            }
        }.onFailure { e ->
            logger.error("恢复采集任务失败", e)
        }
    }
}
