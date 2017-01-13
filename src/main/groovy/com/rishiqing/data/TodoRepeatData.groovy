package com.rishiqing.data

import com.rishiqing.Todo
import com.rishiqing.TodoRepeatTag
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
        this.sql = sql
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
     * 设置分页信息
     * @param offset
     * @param maxOffset
     */
    def setMaxOffset (int maxOffset) {
        this.maxOffset = maxOffset
    }



    def fetch() {
        Date date1 =  new Date ()
        def list = TodoRepeatTag.createCriteria().list{
            eq("isCloseRepeat", false)
        }
        Date date2 = new Date()
        println('search : ' + (date2.getTime() - date1. getTime()))
        println('list size : ' + list.size())
        int i = 0
        int listSize = list ? list.size() : 0
        list.each { it->
            long id = it.id
            Todo todo  = Todo.createCriteria().get{
                eq('repeatTagId', id)
                sqlRestriction('1=1 order by this_.id desc limit 1')
            }
            if (todo) {
                it.todo = todo
            }
            i ++
            // 计算百分比
            this.percent(listSize, i)
        }
        Date date3 = new Date()
        println('fetch finish : ' + (date3.getTime() - date2.getTime()))
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
            if (it.todo) {
                resultList.add(it.todo)
            }
        }
        println('todo insert list size: ' + resultList.size())
        // 执行批量插入
        this.batchInsert(resultList)
    }

    def batchInsert (def list = []) {
        Date  date = new Date ()
        Connection conn = sql.getDataSource().getConnection()
        conn.setAutoCommit(false);
        String query = "INSERT INTO toto_test (version ,  date_created ,  last_updated ,  p_container,  p_display_order,  p_finished_time,  p_is_done,  p_note,  p_parent_id,  p_planed_time,  p_title,  p_user_id,  created_by_client,  receiver_ids,  receiver_names,  sender_id,  is_deleted,  cid,  repeat_tag_id,  sender_todo_id,  team_todo_read,  clock_alert,  kanban_item_id,  is_revoke,  closing_date_finished,  end_date,  start_date,  todo_deploy_id,  is_from_sub_todo,  is_change_date,  is_repeat_todo,  alert_every_day,  check_authority,  dates,  edit_authority,  is_archived,  inboxpcontainer, is_system)  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(query)
        list.each { it ->
            TodoRepeatDs.prepareInsert(it, pstmt)
        }
        Date  date2 = new Date ()
        println('insert prepare complate time : ' + (date2.getTime() - date.getTime()))
        pstmt.executeBatch()
        conn.commit();
        Date date1 = new Date()
        println('executeBatch:' + (date1.getTime() - date2.getTime()))
    }

    private void percent (int size, int index) {
        int a = size / 100
        if (index % a  == 0) {
            println('fetch todo (' + index / a + '%)' )
        }
    }
}
