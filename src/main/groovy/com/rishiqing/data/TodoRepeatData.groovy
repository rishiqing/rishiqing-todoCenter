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
        Date date1 =  new Date ().clearTime()
        def list = TodoRepeatTag.createCriteria().list {
            and {
                //一直重复或者重复截止于的时间大于等于今天
                or {
                    eq("alwaysRepeat", true)
                    ge("repeatOverDate",date1)
                }
                eq("isCloseRepeat",false)
            }
        }
        Date date2 = new Date()
        println('search : ' + (date2.getTime() - date1. getTime()))
        println('list size : ' + list.size())
        int i = 0
        int listSize = list ? list.size() : 0
        List needCreateTodos = []
        for(def it: list){
//            try{
                //获取
                Boolean need = needCreateForToday(it,date1)
                if(!need){
                    continue
                }
                long id = it.id
                Todo todo  = Todo.createCriteria().get{
                    eq('repeatTagId', id)
                    sqlRestriction('1=1 order by this_.id desc limit 1')
                }
                println todo.id
                Map map = [todo: todo,repeatTag: it,date: date1]

                needCreateTodos.add(map)

                i ++
                // 计算百分比
                this.percent(listSize, i)
//            }catch(Exception e) {
//                println "${date1.format("yyyy-MM-dd")}这天对应的重复id为${it.id}生成失败。"
//            }
        }
        Date date3 = new Date()
        println('fetch finish : ' + (date3.getTime() - date2.getTime()))
        return needCreateTodos
    }

    /**
     * 通过一个重复找需要生成的那天
     * @param tag          重复
     * @param secondDay    需要生成新日程的那一天
     */
    def Boolean needCreateForToday(TodoRepeatTag tag,Date secondDay){
        if(!tag || !secondDay){
            return false
        }
        if(!tag.repeatBaseTime || tag.isCloseRepeat || !tag.repeatType){
            return false
        }
        String repeatType = tag.repeatType
        //每天重复
        if("everyDay".equals(repeatType)){
            Date repeatBaseTime = Date.parse("yyyyMMdd",tag.repeatBaseTime)
            return repeatBaseTime.getTime()<=secondDay.getTime()
        }else if("everyWeek".equals(repeatType)){
            return isSameDayInOtherWeek(tag.repeatBaseTime.split(","),secondDay)
        }else if("everyMonth".equals(repeatType)){
            return isSameDayInOtherMonth(tag.repeatBaseTime.split(","),secondDay)
        }else if("everyYear".equals(repeatType)){
            return isSameDayInOtherYear(tag.repeatBaseTime.split(","),secondDay)
        }
        return false
    }

    private Boolean shouldBeGenerated(Todo todo,TodoRepeatTag it,Date date1){
        if(!todo || !it || !date1){
            return false
        }
        if(!todo.pPlanedTime){
            return false
        }
        if(!todo.pIsDone){
            if(todo.startDate && todo.endDate){
                //如果date1的时间在上一条日程的截止时间内，则不生成
                if(todo.startDate.getTime()<=date1.getTime()
                        && todo.endDate.getTime()>=date1.getTime()) return false
            }else{
                //如果上一条日程的时间和要生成的日期相等则不生成
                if(todo.pPlanedTime.getTime()>=date1.getTime()) return false
            }
        }
        //如果重复里已被删除的日期里包含了date1，则不生成
        if(it.deletedDate && it.deletedDate.split(",").contains(date1.format("yyyyMMdd"))) return false
        return true
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
            if (it) {
                Todo todo = it.todo
                TodoRepeatTag tag = it.repeatTag
                Date date = it.date
                if(shouldBeGenerated(todo,tag,date)){
                    println "this:${true}"
                    resultList.add(todo)
                    todo.isRepeatTodo = true
                    todo.save(flush: true)
                }
                println "this:${true}"
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
        String query = "INSERT INTO todo (version ,  date_created ,  last_updated ,  p_container,  p_display_order,  p_finished_time,  p_is_done,  p_note,  p_parent_id,  p_planed_time,  p_title,  p_user_id,  created_by_client,  receiver_ids,  receiver_names,  sender_id,  is_deleted,  cid,  repeat_tag_id,  sender_todo_id,  team_todo_read,  clock_alert,  kanban_item_id,  is_revoke,  closing_date_finished,  end_date,  start_date,  todo_deploy_id,  is_from_sub_todo,  is_change_date,  is_repeat_todo,  alert_every_day,  check_authority,  dates,  edit_authority,  is_archived,  inboxpcontainer, is_system)  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
        int a = size
        if (index % a  == 0) {
            println('fetch todo (' + index / a/100 + '%)' )
        }
    }

    /**
     * 数组中的某一天是否和targetDate的星期是相同的
     * @param baseDates
     * @param targetDate
     * @return
     */
    private Boolean isSameDayInOtherWeek(String[] baseDates,Date targetDate){
        if(!baseDates || !targetDate){
            return false
        }
        Calendar cTarget = targetDate.toCalendar()
        for(int i=0;i<baseDates.length;i++){
            Date baseDate = Date.parse("yyyyMMdd",baseDates[i])
            if(baseDate == null){
                continue
            }
            Calendar cByI = baseDate.toCalendar()
            if(cTarget.get(Calendar.DAY_OF_WEEK)==cByI.get(Calendar.DAY_OF_WEEK)){
                return true
            }

        }
        return false
    }
    /**
     * 数组中的某一天是否和targetDate的日是相同的
     * @param baseDates
     * @param targetDate
     * @return
     */
    private Boolean isSameDayInOtherMonth(String[] baseDates,Date targetDate){
        if(!baseDates || !targetDate){
            return false
        }
        Calendar cTarget = targetDate.toCalendar()
        for(int i=0;i<baseDates.length;i++){
            Date baseDate = Date.parse("yyyyMMdd",baseDates[i])
            if(baseDate == null){
                continue
            }
            Calendar cByI = baseDate.toCalendar()
            if(cTarget.get(Calendar.DAY_OF_MONTH)==cByI.get(Calendar.DAY_OF_MONTH)){
                return true
            }
        }
        return false
    }
    /**
     * 数组中的某一天是否和targetDate的日是相同的
     * @param baseDates
     * @param targetDate
     * @return
     */
    private Boolean isSameDayInOtherYear(String[] baseDates,Date targetDate){
        if(!baseDates || !targetDate){
            return false
        }
        Calendar cTarget = targetDate.toCalendar()
        for(int i=0;i<baseDates.length;i++){
            Date baseDate = Date.parse("yyyyMMdd",baseDates[i])
            if(baseDate == null){
                continue
            }
            Calendar cByI = baseDate.toCalendar()
            if(cTarget.get(Calendar.DAY_OF_YEAR)==cByI.get(Calendar.DAY_OF_YEAR)){
                return true
            }
        }
        return false
    }
}
