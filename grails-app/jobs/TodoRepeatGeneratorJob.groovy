import com.rishiqing.Todo
import com.rishiqing.TodoRepeatTag
import com.rishiqing.data.TodoRepeatData
import groovy.sql.Sql

/**
 * Created by solax on 2017-1-5.
 *
 * 重复日程生成器，每天 24：00后开始生成日程
 *
 */
class TodoRepeatGeneratorJob {

    def dataSource

    static boolean isRunning = false

    static Date currentDate = new Date ()

    static triggers = {
        simple startDelay:1000*20, repeatInterval: 1000 * 60
    }

    def execute () {
        // 运行条件：没有正在运行 + 新的一天
        if (isRunning) {
            return
        } else {
            println('repeat job execute...')
            if (!this.isNewDay()) {
                return
            }
        }
        println('----------------- repeat  job start --------------------')
        isRunning = true
        Sql sql  = new Sql(dataSource)
        TodoRepeatData todoRepeatData = new TodoRepeatData(sql)
        def list = todoRepeatData.fetch()
        todoRepeatData.generator(list)
        println('----------------- repeat  job end --------------------')
        // 初始化状态数据
        currentDate = new Date()
        isRunning = false
    }


    private boolean isNewDay () {
        // 新的日期
        Calendar calendar = Calendar.getInstance()
        int newDay = calendar.get(Calendar.DAY_OF_YEAR)
        // 旧的日期
        Calendar oldCalendar = Calendar.getInstance()
        oldCalendar.setTime(currentDate)
        int oldDay = oldCalendar.get(Calendar.DAY_OF_YEAR)
        if (oldDay != newDay) return true
        return false
    }
}
