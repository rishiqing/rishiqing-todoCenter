import com.rishiqing.Alert
import com.rishiqing.Clock
import com.rishiqing.data.AlertData
import com.rishiqing.data.ClockData
import com.rishiqing.data.TodoRepeatData
import groovy.sql.Sql

import javax.sql.DataSource

/**
 * Created by solax on 2017-1-5.
 * Update by codingR on 2017-06-03
 * 重复日程生成器，每天 00:05 后开始生成日程
 */
class TodoRepeatGeneratorJob {

    /** 注入 */
    DataSource dataSource;
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

        // 开始进行生成操作
        println ("----------------- repeat todo job start --------------------");
        // 创建重复日程生成对象
        TodoRepeatData todoRepeatData = new TodoRepeatData(sql);
        // 开始查询，获取到所有需要进行创建的重复日程的信息.
        List<Map> needCreateTodos = todoRepeatData.fetch();
        // 启动日程创建
        Map<Long,Long> oldTodoIdAndNewTodoIdMap = todoRepeatData.generator(needCreateTodos);
        // 创建结束
        println("----------------- repeat todo job end --------------------");


        println("----------------- clock job start --------------------");
        ClockData clockData = new ClockData(sql);
        // 查询需要创建的时间
        List<Clock> needCreateClock = clockData.fetch(oldTodoIdAndNewTodoIdMap);
        // 进行时间的创建操作
        Map<Long,Long> oldClockIdAndNewClockIdMap = clockData.generator(needCreateClock,oldTodoIdAndNewTodoIdMap);
        // 创建结束
        println("----------------- clock job end --------------------");


        println("----------------- alter job start --------------------");
        AlertData alertData = new AlertData(sql);
        // 查询需要创建的提醒
        List<Alert> needCreateAlerts = alertData.fetch(oldClockIdAndNewClockIdMap);
        // 进行提醒的创建操作
        Integer alertNum = alertData.generator(needCreateAlerts,oldClockIdAndNewClockIdMap);
        // 输出插入提醒数量
        println("插入 Alert 总数 : ${alertNum}");
        println("----------------- alert job end --------------------");

        // 结束
        return;
    }
}
