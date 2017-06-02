import com.rishiqing.Todo
import com.rishiqing.TodoRepeatTag
import com.rishiqing.data.TodoRepeatData
import groovy.sql.Sql

/**
 * Created by solax on 2017-1-5.
 *
 * 重复日程生成器，每天 00:05 后开始生成日程
 *
 */
class TodoRepeatGeneratorJob {

//    def dataSource
//
//    static boolean isRunning = false
//
//    static Date currentDate = new Date ()
//
//    static triggers = {
//        simple startDelay:1000*20, repeatInterval: 1000 * 60
//    }
//
//    def execute () {
//        // 运行条件：没有正在运行 + 新的一天
//        if (isRunning) {
//            return
//        } else {
//            println('repeat job execute...')
//            if (!this.isNewDay()) {
//                return
//            }
//        }
//        println('----------------- repeat  job start --------------------')
//        isRunning = true
//        Sql sql  = new Sql(dataSource)
//        TodoRepeatData todoRepeatData = new TodoRepeatData(sql)
//        def list = todoRepeatData.fetch()
//        todoRepeatData.generator(list)
//        println('----------------- repeat  job end --------------------')
//        // 初始化状态数据
//        currentDate = new Date()
//        isRunning = false
//    }
//
//
//    private boolean isNewDay () {
//        // 新的日期
//        Calendar calendar = Calendar.getInstance()
//        int newDay = calendar.get(Calendar.DAY_OF_YEAR)
//        // 旧的日期
//        Calendar oldCalendar = Calendar.getInstance()
//        oldCalendar.setTime(currentDate)
//        int oldDay = oldCalendar.get(Calendar.DAY_OF_YEAR)
//        if (oldDay != newDay) return true
//        return false
//    }

    /** 注入 */
    def dataSource;
    /** 当前时间为空 */
    static Date currentDate = null;
    /** 触发器 */
    static triggers = {
        // 服务器启动 1 分钟后开始运行
        simple name: 'mySimpleTrigger', startDelay: 60000;
        // 使用 cron 表达式进行控制：每天凌晨 00:05 进行生成
        cron(name: "todoSendJob", cronExpression: "0 5 0 * * ?");
    }

    def execute(){
        // 设置当前运行时间
        currentDate = new Date();
        // 创建 sql 对象
        Sql sql  = new Sql(dataSource);
        // 创建重复日程生成对象
        TodoRepeatData todoRepeatData = new TodoRepeatData(sql);
        // 开始进行生成操作
        println ("----------------- repeat todo job start --------------------");
        // 开始查询，获取到所有需要进行创建的重复日程的信息.
        List<Map> needCreateTodos = todoRepeatData.fetch();
        // 启动日程创建
        Map<Long,Long> oldIdAndNewIdMap = todoRepeatData.generator(needCreateTodos);
        // 创建结束
        println("----------------- repeat todo job end --------------------");
        println("----------------- clock job start --------------------");


        println("----------------- clock job end --------------------");
        println("----------------- alter job start --------------------");


        println("----------------- alert job end --------------------");
        // 结束
        return;
    }
}
