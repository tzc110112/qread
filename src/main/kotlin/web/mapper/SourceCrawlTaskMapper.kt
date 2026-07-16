package web.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import web.model.SourceCrawlTask

interface SourceCrawlTaskMapper : BaseMapper<SourceCrawlTask> {

    @Select("SELECT * FROM source_crawl_task WHERE userid = #{userid} AND source_url = #{sourceUrl} LIMIT 1")
    fun getByUserAndSource(@Param("userid") userid: String, @Param("sourceUrl") sourceUrl: String): SourceCrawlTask?

    @Select("SELECT * FROM source_crawl_task WHERE userid = #{userid} AND status = 1")
    fun getRunningByUser(@Param("userid") userid: String): List<SourceCrawlTask>

    @Select("SELECT * FROM source_crawl_task WHERE status = 0")
    fun getPendingTasks(): List<SourceCrawlTask>

    @Select("SELECT * FROM source_crawl_task WHERE userid = #{userid} ORDER BY last_update_time DESC")
    fun getByUser(@Param("userid") userid: String): List<SourceCrawlTask>

    @Update("UPDATE source_crawl_task SET status = #{status} WHERE id = #{id}")
    fun updateStatus(@Param("id") id: String, @Param("status") status: Int): Int
}
