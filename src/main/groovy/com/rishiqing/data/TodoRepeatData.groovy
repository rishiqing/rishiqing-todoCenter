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

    /**
     * 查询器，用来查询需要创建重复日程的日程信息。
     * @return
     */
    def fetch() {
        // 获取当前日期
        Date startSearch =  new Date ().clearTime()
        // 查询 repeatTag
        def repeatTagList = TodoRepeatTag.createCriteria().repeatTagList {
            and {
                //一直重复或者重复截止于的时间大于等于今天
                or {
                    eq("alwaysRepeat", true)
                    ge("repeatOverDate",startSearch)
                }
                // 没有关闭重复
                eq("isCloseRepeat",false)
            }
        }
        Date endSearch = new Date()
        // 搜索时长
        println('search : ' + (endSearch.getTime() - startSearch. getTime()))
        // 获取list的长度
        int listSize = repeatTagList ? repeatTagList.size() : 0
        // 重复标记的数量，每个标记对应一个重复的日程。
        println('repeatTagList size : ' + listSize)
        // 阀值，用来计算百分比使用
        int i = 0
        // 需要进行创建的日程组成的list
        List needCreateTodos = []
        // 遍历查询到的日程的list
        for(def repeatTag: repeatTagList){
            try{
                // 查询当前重复标记所标记的日程，是否需要进行创建操作
                Boolean need = needCreateForToday(repeatTag,startSearch);
                // 如果不需要进行创建
                if(!need){
                    continue;
                }
                // 获取 repeatTag 的id
                long id = repeatTag.id;
                // 查找到 repeatTag 对应的日程
                Todo todo  = Todo.createCriteria().get{
                    eq('repeatTagId', id)
                    sqlRestriction('1=1 order by this_.id desc limit 1')
                }
                // 如果日程存在，那么组成 map 添加到需要创建的日程列表里。
                if(todo){
                    Map map = [todo: todo,repeatTag: repeatTag,date: startSearch];
                    needCreateTodos.add(map);
                }
                // 阀值 + 1
                i ++
                // 计算百分比
                this.percent(listSize, i);
            }catch(Exception e) {
                println "${startSearch.format("yyyy-MM-dd")}这天对应的重复id为${it.id}生成失败。";
            }
        }
        // 完成所有需要创建的重复日程的查询
        Date endFetch = new Date();
        println('fetch finish : ' + (endFetch.getTime() - endSearch.getTime()));
        return needCreateTodos;
    }

    /**
     * 通过一个重复找需要生成的那天
     * @param tag          重复
     * @param secondDay    需要生成新日程的那一天
     */
    def Boolean needCreateForToday(TodoRepeatTag tag,Date secondDay){
        // 参数错误
        if(!tag || !secondDay){
            return false
        }
        // 如果重复标记基本时间没有，或者已经被关闭，或者没有重复类型（每天每周每月每年）
        if(!tag.repeatBaseTime || tag.isCloseRepeat || !tag.repeatType){
            return false
        }
        // 获取重复类型（每天每周每月每年）
        String repeatType = tag.repeatType
        //每天重复
        if("everyDay".equals(repeatType)){

            // 获取重复基本日期
            Date repeatBaseTime = Date.parse("yyyyMMdd",tag.repeatBaseTime)
            // 重复的基本日期是否 < 生成重复的日期
            return repeatBaseTime.getTime()<secondDay.getTime()

        }// 每周重复
        else if("everyWeek".equals(repeatType)){

            return isSameDayInOtherWeek(tag.repeatBaseTime.split(","),secondDay);

        }// 如果是每月重复
        else if("everyMonth".equals(repeatType)){

            /*
             * 如果是最后一天重复则查看secondDay是否为最后一天
             * 如果不是最后一天重复则查看baseTime是否有需要创建日程的
             */
            return tag.isLastDate?getLastDay(secondDay).getTime()==secondDay.getTime():isSameDayInOtherMonth(tag.repeatBaseTime.split(","),secondDay);
        }// 如果是每年重复
        else if("everyYear".equals(repeatType)){

            return isSameDayInOtherYear(tag.repeatBaseTime,secondDay);

        }
        return false;
    }

    private Boolean shouldBeGenerated(Todo todo,TodoRepeatTag it,Date date1){
        // 参数验证
        if(!todo || !it || !date1){
            return false
        }
        // 查看当前日程是否在收纳箱，如果在，那么不为它创建
        if("inbox".equals(todo.pContainer)){
            return false
        }
        // 查看日程是否没有 pPlanedTime ，没有则不创建
        if(!todo.pPlanedTime){
            return false
        }
        // 日程没有被完成
        if(!todo.pIsDone){
            // 查看是否有开始和结束时间
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
     * @param list 存放日程的 list
     */
    def generator (def list) {
        // 开始启动创建
        def resultList = []
        StringBuffer sb = new StringBuffer()
        list.each { it ->
            // 经过各种判断，如果确定需要保存到数据库，  则存入resultList中
            if (it) {
                // 获取日程
                Todo todo = it.todo;
                // 获取日程重复标记
                TodoRepeatTag tag = it.repeatTag;
                // 获取需要创建日程的日期
                Date date = it.date;
                // 判断是否应该生成重复
                if(shouldBeGenerated(todo,tag,date)){
                    // 需要生成的日程，添加到结果集中
                    resultList.add(todo)
                    // 保存需要创建重复的日程的id
                    sb.append("${todo.id},")
//                    todo.isRepeatTodo = true
//                    todo.save(flush: true)
                }
            }
        }
        // 需要进行重复日程创建的日程的数量
        println('todo insert list size: ' + resultList.size())
        // 执行批量插入
        this.batchInsert(resultList)
        String todoIds = sb.toString()
        if(todoIds && !"".equals(todoIds)){
            println "todo update isRepeatTodo start"
//            println "UPDATE  todo set is_repeat_todo=1 where id in ("+(todoIds.endsWith(",")?todoIds.substring(0,todoIds.length()-1):todoIds)+")"
            sql.executeUpdate("UPDATE  todo set is_repeat_todo=1 where id in ("+(todoIds.endsWith(",")?todoIds.substring(0,todoIds.length()-1):todoIds)+")")
            println "todo update isRepeatTodo end"
        }
    }

    /**
     * 披上插入方法
     * @param list
     * @return
     */
    def batchInsert (def list = []) {
        // 获取当前日期
        Date  date = new Date ();
        //
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

    /**
     * 用来坚挺重复日程创建百分比。当为 100%　时，证明所有重复日程已经查询完毕
     * @param size
     * @param index
     */
    private void percent (int size, int index) {
        int a = size
        if (index % a  == 0) {
            println('fetch todo (' + index / a/100 + '%)' )
        }
    }

    /**
     * 数组中的某个日期对应的星期，是否与 targetDate 对应的星期是相同的
     * @param baseDates
     * @param targetDate
     * @return
     */
    private Boolean isSameDayInOtherWeek(String[] baseDates,Date targetDate){
        if(!baseDates || !targetDate){
            return false
        }
        // 获取当前日期的一个日历实体
        Calendar cTarget = targetDate.toCalendar()
        for(int i=0;i<baseDates.length;i++){
            // 获取第一个基本日期 (repeatBaseTime 中的一个)
            Date baseDate = Date.parse("yyyyMMdd",baseDates[i])
            if(baseDate == null){
                continue
            }
            // 转换成日历格式
            Calendar cByI = baseDate.toCalendar()
            // 如果这个基本日期的星期和 targetDate 对应的星期是相同的，返回true
            if(cTarget.get(Calendar.DAY_OF_WEEK)==cByI.get(Calendar.DAY_OF_WEEK)){
                return true
            }
        }
        // 否则
        return false
    }
    /**
     * 数组中的某一天是否和 targetDate 所对应的那一天是同一天（日期相同）
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
    private Boolean isSameDayInOtherYear(String baseDate,Date targetDate){
        // 参数验证
        if(!baseDate || !targetDate){
            return false
        }
        // 把 targetDate 转换成日历格式
        Calendar cTarget = targetDate.toCalendar()
        // 把 repeatBaseTime 中的日期转换成日期格式
        Date b = Date.parse("yyyyMMdd",baseDate)
        if(b == null){
            return false
        }
        // 再次转换成日历格式
        Calendar cByI = b.toCalendar()
        // 比较是否是下一年的今天，相同则为 true
        if(cTarget.get(Calendar.DAY_OF_YEAR)==cByI.get(Calendar.DAY_OF_YEAR)){
            return true
        }
        // 否则
        return false
    }

    /**
     * 谋取某天的月份中最后一天
     * @param date
     * @return
     */
    private Date getLastDay(Date date){
        def cal = date.toCalendar()
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, 1, 0, 0, 0)
        cal.add(Calendar.DAY_OF_MONTH,-1)
        return cal.getTime()
    }
}
