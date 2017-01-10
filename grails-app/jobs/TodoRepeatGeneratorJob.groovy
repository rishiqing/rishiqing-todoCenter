import com.rishiqing.data.TodoRepeatData
import groovy.sql.Sql

import java.sql.Connection

/**
 * Created by solax on 2017-1-5.
 *
 * 重复日程生成器，每天 24：00后开始生成日程
 *
 */
class TodoRepeatGeneratorJob {

    def dataSource

    static triggers = {
        simple startDelay:1000*20, repeatInterval: 1000*60
    }

    def execute() {
        Sql sql = new Sql(dataSource)
        String query  = 'SELECT' +
                ' trt.next_repeat_time,' +
                ' trt.repeat_base_time,' +
                'trt.repeat_dates,' +
                'trt.repeat_type,' +
                'trt.user_id,' +
                'trt.is_close_repeat,' +
                'trt.always_repeat,' +
                'trt.deleted_date,' +
                'trt.is_last_date,' +
                'trt.repeat_over_date,' +
                't.id,' +
                't.p_container,' +
                't.p_note,' +
                't.p_parent_id,' +
                't.p_title,' +
                't.p_user_id,' +
                't.repeat_tag_id,' +
                't.clock_alert,' +
                't.end_date,' +
                't.start_date,' +
                't.todo_deploy_id,' +
                't.is_repeat_todo,' +
                't.alert_every_day,' +
                't.check_authority,' +
                't.dates,' +
                't.edit_authority,' +
                't.inboxpcontainer ' +
                'FROM ' +
                'todo_repeat_tag trt, todo t ' +
                'WHERE ' +
                'trt.is_close_repeat = 0 ' +
                'and t.repeat_tag_id = trt.id ' +
                'and t.is_deleted = 0 ' +
                'and t.is_archived = 0 ' +
                'GROUP BY trt.id ' +
                'HAVING t.id = ( SELECT MAX(id) from todo t1 where t1.id = t.id) '
        TodoRepeatData todoRepeatData = new TodoRepeatData(sql)
        Date date1 = new Date ()
        // 查询重复及日程数据
        def list = todoRepeatData.fetch(query, 100)
        Date date2 = new Date()
        println('search time --------------- :' + (date2.getTime() - date1.getTime()))
        //  执行逻辑，并且生成日程
        todoRepeatData.generator(list)
        Date date3 = new Date()
        println('generator time --------------- :' + (date3.getTime() - date2.getTime()))
    }
}
