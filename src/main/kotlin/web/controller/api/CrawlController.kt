package web.controller.api

import org.noear.solon.annotation.Controller
import org.noear.solon.annotation.Inject
import org.noear.solon.annotation.Mapping
import org.noear.solon.core.util.DataThrowable
import org.noear.solon.web.cors.annotation.CrossOrigin
import web.mapper.SourceCrawlTaskMapper
import web.response.*
import web.service.AutoCrawlService

@Controller
@Mapping(routepath)
@CrossOrigin(origins = "*")
open class CrawlController : BaseController() {

    @Inject
    lateinit var sourceCrawlTaskMapper: SourceCrawlTaskMapper

    /**
     * 获取用户的所有采集任务状态
     */
    @Mapping("/getCrawlTasks")
    open fun getCrawlTasks(accessToken: String?) = run {
        val user = getuserbytocken(accessToken)
        val tasks = sourceCrawlTaskMapper.getByUser(user.id!!)
        JsonResponse(true).Data(tasks?.map { task ->
            mapOf(
                "id" to task.id,
                "sourceUrl" to task.sourceUrl,
                "sourceName" to task.sourceName,
                "status" to task.status,
                "totalBooks" to task.totalBooks,
                "totalExplore" to task.totalExplore,
                "currentExploreIdx" to task.currentExploreIdx,
                "currentPage" to task.currentPage,
                "errorMsg" to task.errorMsg,
                "createTime" to task.createTime,
                "lastUpdateTime" to task.lastUpdateTime
            )
        })
    }

    /**
     * 手动触发指定书源的全量采集
     */
    @Mapping("/startCrawl")
    open fun startCrawl(accessToken: String?, sourceUrl: String?) = run {
        if (sourceUrl.isNullOrBlank()) {
            throw DataThrowable().data(JsonResponse(false, NOT_BANK))
        }
        val user = getuserbytocken(accessToken)
        AutoCrawlService.startFullCrawl(sourceUrl, user)
        JsonResponse(true, "采集任务已启动")
    }

    /**
     * 删除采集任务记录
     */
    @Mapping("/delCrawlTask")
    open fun delCrawlTask(accessToken: String?, id: String?) = run {
        if (id.isNullOrBlank()) {
            throw DataThrowable().data(JsonResponse(false, NOT_BANK))
        }
        val user = getuserbytocken(accessToken)
        val task = sourceCrawlTaskMapper.selectById(id)
        if (task == null || task.userid != user.id) {
            throw DataThrowable().data(JsonResponse(false, NOT_IS))
        }
        sourceCrawlTaskMapper.deleteById(id)
        JsonResponse(true)
    }
}
