package com.rishiqing.data

import com.rishiqing.Todo
import com.rishiqing.ds.TodoRepeatDs
import groovy.sql.Sql
import javax.sql.DataSource
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement;


/**
 * Created by solax on 2017-1-6.
 */
class TodoRepeatData {

    Sql sql

    // 总条数
    int count

    // 分页
    int offset = 1

    // 分页
    int maxOffset

    // 循环次数
    int doQueryCount

    // 查询语句
    String query

    TodoRepeatData (Sql sql) {
        this.sql =  sql

    }

    /**
     * 查询方法
     * @param query
     * @param intMaxOffset
     * @return
     */
    def fetch (String query, int intMaxOffset) {
        this.fetch(query, intMaxOffset) {}
    }
    /**
     * 查询方法
     * @return
     */
    def fetch (String query, int intMaxOffset, Closure closure) {
        // 设置查询语句
        this.query = query
        // 设置每页数量
        this.setMaxOffset(intMaxOffset)
        // 获得总条数
        this.initCount()
        // 查询数据
        return this.fetchData(closure);
    }

    /**
     * 设置分页信息
     * @param offset
     * @param maxOffset
     */
    def setMaxOffset (int maxOffset) {
        this.maxOffset = maxOffset
    }


    def initCount () {
        def query = "select count(1) as count from (" + this.query + ")t"
        println(query)
        def result = sql.firstRow(query)
        println(result)
        this.count = result.get("count")
        if (this.count > this.maxOffset) {
            doQueryCount = (this.count / this.maxOffset) + 1
        } else {
            doQueryCount = 1
        }
    }

    def fetchData(Closure closure) {
        def list = []
        for (int i = 0 ; i < this.doQueryCount; i++) {
            int offset = this.offset +  (this.maxOffset * i)
            def resultMap   =  sql.rows(this.query, offset, this.maxOffset)
            resultMap.each { it->
                Todo todo = Todo.toTodoDomain(it)
                closure.call(todo)
                list.add(todo)
            }
        }
        return list
    }

    /**
     * 日程重复生成处理逻辑
     *
     * List< T o d o >
     */
    def generator (def list) {
        def resultList = []
        list.each { it ->
            // 经过各种判断，如果确定需要保存到数据库，  则存入resultList中
            resultList.add(it)
        }
        // 执行批量插入
        this.batchInsert(resultList)
    }

    /**
     * 批量插入方法
     * @param list
     * @return
     */
    def batchInsert2 (def list = []) {
        Date date1 = new Date()
        String sqlStr = "INSERT INTO toto_test (version ,  date_created ,  last_updated ,  p_container,  p_display_order,  p_finished_time,  p_is_done,  p_note,  p_parent_id,  p_planed_time,  p_title,  p_user_id,  created_by_client,  receiver_ids,  receiver_names,  sender_id,  is_deleted,  cid,  repeat_tag_id,  sender_todo_id,  team_todo_read,  clock_alert,  kanban_item_id,  is_revoke,  closing_date_finished,  end_date,  start_date,  todo_deploy_id,  is_from_sub_todo,  is_change_date,  is_repeat_todo,  alert_every_day,  check_authority,  dates,  edit_authority,  is_archived,  inboxpcontainer) VALUES "
      //  String one = "('0', '2013-08-08 21:45:19', '2013-12-07 10:37:30', 'UE', '100000', '2013-08-08 21:45:19', 1, NULL, NULL, '2012-07-04 00:00:00', '读小强升职记', '2', 'web', NULL, NULL, NULL, 0, '-1', NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'private', NULL, 'member', 0, 'UE')"
        StringBuffer query = new StringBuffer()
        int i = 0
        list.each { it ->
            i ++
            String insertOne = TodoRepeatDs.toInsertString(it)
            if (i != list.size() - 1) {
                insertOne += ','
            }
            println(insertOne)
            query.append(insertOne)
        }
        sqlStr += query
        Date date2 = new Date()
        sql.executeInsert(sqlStr)

        Date date3 = new Date()
        println("generate success after :" + (date3.getTime() - date2.getTime()) + "---before: " + (date2.getTime() - date1.getTime()))
    }

    def batchInsert (def list = []) {
        Connection conn = sql.getDataSource().getConnection()
        conn.setAutoCommit(false);
        String query = "INSERT INTO toto_test (version ,  date_created ,  last_updated ,  p_container,  p_display_order,  p_finished_time,  p_is_done,  p_note,  p_parent_id,  p_planed_time,  p_title,  p_user_id,  created_by_client,  receiver_ids,  receiver_names,  sender_id,  is_deleted,  cid,  repeat_tag_id,  sender_todo_id,  team_todo_read,  clock_alert,  kanban_item_id,  is_revoke,  closing_date_finished,  end_date,  start_date,  todo_deploy_id,  is_from_sub_todo,  is_change_date,  is_repeat_todo,  alert_every_day,  check_authority,  dates,  edit_authority,  is_archived,  inboxpcontainer)  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(query)
        list.each { it ->
            TodoRepeatDs.prepareInsert(it, pstmt)
        }
        Date  date = new Date ()
        pstmt.executeBatch()
        conn.commit();
        Date date1 = new Date()
        println('executeBatch:' + (date1.getTime() - date.getTime()))
    }

    def myTest () {

        Connection conn = sql.getDataSource().getConnection()
        String strsql = "INSERT INTO mytest (id ,  name ,  content)  VALUES (?,?,?)";
        println('1')
        PreparedStatement pstmt = conn.prepareStatement(strsql)
        String content = "select * from todo where p_title='来了来了'"
        for (int i =0 ; i< 10; i++) {
            pstmt.setString(1, String.valueOf(i))
            pstmt.setString(2, null)
            pstmt.setString(3, null)
            pstmt.addBatch()
        }
        pstmt.executeBatch()
    }
}
