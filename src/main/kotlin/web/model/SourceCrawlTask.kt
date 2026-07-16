package web.model

import com.baomidou.mybatisplus.annotation.TableId
import org.dromara.autotable.annotation.AutoTable
import org.dromara.autotable.annotation.ColumnType
import org.dromara.autotable.annotation.Index
import org.dromara.autotable.annotation.PrimaryKey
import java.util.*

@AutoTable(value = "source_crawl_task")
class SourceCrawlTask {
    @TableId
    @PrimaryKey
    var id: String? = null
    @Index
    var userid: String? = null
    var sourceUrl: String? = null
    var sourceName: String? = null

    @ColumnType("MEDIUMTEXT")
    var exploreUrls: String? = null

    var currentPage: Int = 0
    var currentExploreIdx: Int = 0
    var status: Int = 0             // 0=等待, 1=进行中, 2=完成, -1=失败
    var totalBooks: Int = 0
    var totalExplore: Int = 0
    var errorMsg: String? = null

    @ColumnType("LONGTEXT")
    var books: String? = null

    var createTime: Long = 0
    var lastUpdateTime: Long = 0

    fun create(): SourceCrawlTask {
        this.id = UUID.randomUUID().toString()
        this.createTime = System.currentTimeMillis()
        this.lastUpdateTime = System.currentTimeMillis()
        return this
    }
}
