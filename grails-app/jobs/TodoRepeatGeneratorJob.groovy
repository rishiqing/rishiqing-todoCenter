import com.rishiqing.Alert
import com.rishiqing.Clock
import com.rishiqing.data.AlertData
import com.rishiqing.data.ClockData
import com.rishiqing.data.TodoRepeatData
import grails.core.GrailsApplication
import groovy.sql.Sql

import javax.sql.DataSource

/**
 * Created by solax on 2017-1-5.
 * Update by codingR on 2017-06-03
 * 重复日程生成器，每天 00:05 后开始生成日程
 *
 * 关于 triggers 的说明：
 * 具体可见博客
 * @link http://blog.csdn.net/wangyongwyk/article/details/5534966
 *      class MyJob {
 *          static triggers = {
 *              // 定义一个触发器，name是 mySimpleTrigger，在服务器启动 60000 mill 后开始运行，并每隔 1000 mill 运行一次
 *              // 可以不给定 startDelay 和 repeatInterval ：
 *              // 如果这两个属性不指定，则使用默认值（repeatInterval为1分钟，startDelay为30秒）
 *              simple name: 'mySimpleTrigger', startDelay: 60000, repeatInterval: 1000
 *          }
 *          // 触发器会默认每隔  1000  mill 运行一次 execute 方法
 *          def execute(){
 *              print "Job run!"
 *          }
 *      }
 *
 * 通过 cron 表达式调度任务:
 *
 *      class MyJob  {
 *            static triggers = {
 *                // 每天 6 点调度任务
 *                cron name: 'myTrigger', cronExpression: "0 0 6 * * ?"
 *            }
 *            def execute(){ print "Job run!" }
 *       }
 *
 * 注意！simple 和 cron 不能一起使用，如果一起使用，就会查询两次。
 *
 * 关于 cron 表达式:
 *
 * 具体可见博客:
 * @link http://www.cnblogs.com/junrong624/p/4239517.html#undefined
 * cron 表达式在线生成网址：
 * @link http://cron.qqe2.com/
 *
 * 两种格式：
 *      秒       分       时   月中的哪一天   月     周中的哪一天   年
 *      Seconds Minutes Hours DayofMonth   Month  DayofWeek   Year 或
 *      Seconds Minutes Hours DayofMonth   Month  DayofWeek
 *
 * 几个例子：
 * 每隔5秒执行一次：0/5 * * * * ?  （第一个 0 也可以使用 * 表示，意思相同）
 * 每隔1分钟执行一次：0 0/1 * * * ?
 * 每天23点执行一次：0 0 23 * * ?
 * 每天凌晨1点执行一次：0 0 1 * * ?
 * 每月1号凌晨1点执行一次：0 0 1 1 * ?
 * 每月最后一天23点执行一次：0 0 23 L * ?
 * 每周星期天凌晨1点实行一次：0 0 1 ? * L
 * 在26分、29分、33分执行一次：0 26,29,33 * * * ?
 * 每天的0点、13点、18点、21点都执行一次：0 0 0,13,18,21 * * ?
 *
 *
 */
class TodoRepeatGeneratorJob {

    /** 注入 */
    GrailsApplication grailsApplication;
    DataSource dataSource;
    /** 当前时间为空 */
    static Date currentDate = null;
    /** 触发器 */
    static triggers = {
        println "触发器启动 : " + new Date().format("yyyy-MM-dd HH:mm:ss");
        // 使用 cron 表达式进行控制：每天凌晨 00:05 进行生成
        cron name: "repeatTodoGenerator",startDelay: 60000, cronExpression: "0 5 0 * * ? *" ;
        //simple startDelay:1000,repeatInterval: 1000*60*60
    }


    def execute(){
        // 设置当前运行时间
        currentDate = new Date();
        // 创建 sql 对象
        Sql sql  = new Sql(dataSource);
        println ("============================================================");
        println ("触发时间 : " + new Date().format("yyyy-MM-dd HH:mm:ss"));
        // 重复日程生成器
        Map oldTodoIdAndNewTodoIdMap = todoBuilder(sql,grailsApplication);
        // 时间生成器
        Map oldClockIdAndNewClockIdMap = clockBuilder(oldTodoIdAndNewTodoIdMap?oldTodoIdAndNewTodoIdMap:[:],sql,grailsApplication);
        // 提醒生成器
        alertBuilder(oldClockIdAndNewClockIdMap?oldClockIdAndNewClockIdMap:[:],sql);
        println ("============================================================");
        println ("操作结束 : " + new Date().format("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 日程生成器
     * @return
     */
    def todoBuilder(Sql sql,def grailsApplication){
        // 开始进行生成操作
        println("----------------- repeat todo job start --------------------");
        // 创建重复日程生成对象
        TodoRepeatData todoRepeatData = new TodoRepeatData(sql,grailsApplication);
        // 开始查询，获取到所有需要进行创建的重复日程的信息.
        List<Map> needCreateTodos = todoRepeatData.fetch();
        // 启动日程创建
        Map<Long,Long> oldTodoIdAndNewTodoIdMap = todoRepeatData.generator(needCreateTodos);
        // 创建结束
        println("----------------- repeat todo job end ----------------------");
        // 返回
        return oldTodoIdAndNewTodoIdMap;
    }

    /**
     * 时间生成器
     * @param oldTodoIdAndNewTodoIdMap
     * @return
     */
    def clockBuilder(Map oldTodoIdAndNewTodoIdMap,Sql sql,def grailsApplication){
        Map<Long,Long> oldClockIdAndNewClockIdMap = [:];
        println("----------------- clock job start --------------------------");
        ClockData clockData = new ClockData(sql,grailsApplication);
        // 查询需要创建的时间
        List<Clock> repeatTodoNeedCreateClock = [];
        List<Map> baseTodoNeedCreateClock = [];
        // 查询日程需要生成的时间
        clockData.fetchRepeatTodoClock(repeatTodoNeedCreateClock,oldTodoIdAndNewTodoIdMap);
        // 查询普通日程需要生成的时间
        clockData.fetchBaseTodoClock(baseTodoNeedCreateClock,sql);
        // 进行时间的创建操作
        if(repeatTodoNeedCreateClock.size() > 0 || baseTodoNeedCreateClock.size() > 0){
            oldClockIdAndNewClockIdMap = clockData.generator(repeatTodoNeedCreateClock,baseTodoNeedCreateClock,oldTodoIdAndNewTodoIdMap);
        }
        // 创建结束
        println("----------------- clock job end ----------------------------");
        // 返回
        return oldClockIdAndNewClockIdMap;
    }

    /**
     * 提醒生成器
     * @param oldClockIdAndNewClockIdMap
     * @return
     */
    def alertBuilder(Map oldClockIdAndNewClockIdMap,Sql sql){
        println("----------------- alter job start --------------------------");
        AlertData alertData = new AlertData(sql);
        // 查询需要创建的提醒
        List<Alert> needCreateAlerts = alertData.fetch(oldClockIdAndNewClockIdMap);
        // 进行提醒的创建操作
        if(needCreateAlerts.size() >0){
            Integer alertNum = alertData.generator(needCreateAlerts,oldClockIdAndNewClockIdMap);
            // 输出插入提醒数量
            println("插入 Alert 总数 : ${alertNum}");
        }
        println("插入 Alert 总数 ： 0")
        println("----------------- alert job end ----------------------------");

    }
}
