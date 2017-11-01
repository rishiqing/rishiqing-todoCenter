package com.rishiqing.data

import com.rishiqing.Todo
import com.rishiqing.TodoRepeatTag
import com.rishiqing.ds.TodoRepeatDs
import com.rishiqing.util.CommonUtil
import com.rishiqing.util.ResourceUtil
import grails.core.GrailsApplication
import groovy.sql.Sql
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types;


/**
 * Created by solax on 2017-1-6.
 * Update by codingR on 2017-06-03.
 */
class TodoRepeatData {

    private def grailsApplication;
    // groovy sql 对象
    private Sql sql = null;
    // 数据库连接对象
    private Connection conn = null;
    // 预编译语言对象
    private PreparedStatement pstmt = null;
    // 数据库结果集对象
    private ResultSet rs = null;
    // 构造
    TodoRepeatData (Sql sql,def grailsApplication) {
        this.sql = sql
        this.grailsApplication = grailsApplication
    }

    // ================================== fetch start =======================================//

    /**
     * 查询器，用来查询需要创建重复日程的日程信息。
     * @return
     */
    def fetch() {

        println "查询 TodoRepeat 开始 : ${new Date().format("yyyy-MM-dd HH:mm:ss")}"
        // 获取一个检索时间
        Date searchDate = new Date().clearTime();
        // 查询 repeatTag
        List<TodoRepeatTag> repeatTagList = (List<TodoRepeatTag>)TodoRepeatTag.createCriteria().list {
            and {
                //一直重复或者重复截止于的时间大于等于今天
                or {
                    eq("alwaysRepeat", true)
                    ge("repeatOverDate",searchDate)
                }
                // 没有关闭重复
                eq("isCloseRepeat",false)
            }
        }

        // 获取list的长度
        Integer listSize = repeatTagList ? repeatTagList.size() : 0;
        // 重复标记的数量，每个标记对应一个重复的日程。
        println("检索到 TodoRepeatTag 数量 : " + listSize + "个" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}");
        println "检索 TodoRepeat 结束 : ${new Date().format("yyyy-MM-dd HH:mm:ss")}";

        // 需要进行创建的日程组成的list
        List<Map> needCreateTodos = [];
        // 遍历查询到的日程的list
        for(TodoRepeatTag repeatTag: repeatTagList){
            try{
                // 查询当前重复标记所标记的日程，是否需要进行创建操作
                Boolean need = CommonUtil.needCreateForToday(repeatTag,searchDate);
                // 如果不需要
                if(!need){
                    continue;
                }
                // 获取 repeatTag 的id
                Long id = repeatTag.id;
                // 查找到 repeatTag 对应的最后一条日程
                Todo todo  = (Todo)Todo.createCriteria().get{
                    eq('repeatTagId', id);
                    // 通过 id 排序查找到最大的那一条
                    sqlRestriction('1=1 order by this_.id desc limit 1');
                }

                // 如果日程存在，组成基本信息 map 添加到需要创建的日程列表里。
                if(todo){
                    // 把基本信息装入 map ，添加到需要创建的日程的列表中
                    Map map = [todo: todo,repeatTag: repeatTag,date: searchDate];
                    needCreateTodos.add(map);
                }
            }catch(Exception e) {
                e.printStackTrace();
                println "${searchDate.format("yyyy-MM-dd")}这天对应的重复id为${repeatTag.id}生成失败。";
            }
        }
        return needCreateTodos;
    }

    // ================================== fetch end =======================================//

    // ================================ generate start ====================================//

    /**
     * 日程重复生成处理逻辑
     * @param list 存放日程的 list，此list 元素的结构 [todo: todo,repeatTag: repeatTag,date: searchDate]
     */
    def generator (def list) {
        // 需要创建的日程结果
        def todoResultList = [];
        // 用于记录日程的 id 组成的字符串，以 “,” 分割
        StringBuffer todoIdsSb = new StringBuffer()
        for(def it : list) {
            // 获取日程
            Todo todo = it.todo;
            // 获取日程重复标记
            TodoRepeatTag tag = it.repeatTag;
            // 获取需要创建日程的日期
            Date date = it.date;
            // 判断是否应该生成重复
            if(CommonUtil.shouldBeGenerated(todo,tag,date)){
                // 需要生成的日程，添加到结果集中
                todoResultList.add(todo);
                // 保存需要创建重复的日程的id
                todoIdsSb.append("${todo.id},");
            }
        }
        // 需要进行重复日程创建的日程的数量
        println('需要生成 Repeat Todo 数量 : ' + todoResultList.size() + "个" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}");

        Map<Long,Long> oldTodoIdAndNewTodoIdMap = [:];
        if(todoResultList.size() > 0){
            // 进行重置 id 自增长的操作，把当前需要创建重复的日程的空间预留出来
            Long oldAutoIncrement = sysInsertTodo(todoResultList);

            // 执行日程批量插入，返回日程新老 id 映射。
            oldTodoIdAndNewTodoIdMap = batchInsertTodo(todoResultList,oldAutoIncrement);

            // 打开重复日程的创建开关 (isRepeatTodo)，当 isRepeatTod0 = 1是，第一天的日程将不显示延期。
            String todoIds = todoIdsSb.toString();
            if(todoIds && !"".equals(todoIds)){
                println "todo update isRepeatTodo start" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}"
                sql.executeUpdate("UPDATE  todo set is_repeat_todo=1 where id in ("+(todoIds.endsWith(",")?todoIds.substring(0,todoIds.length()-1):todoIds)+")")
                println "todo update isRepeatTodo end" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}"
            }
        }
        return oldTodoIdAndNewTodoIdMap;
    }

    /**
     * 处理日程自增长的值
     * @param todoResultList
     * @return
     */
    @Deprecated
    private def handleTodoAutoIncrement(List todoResultList){
        try{

            println "处理 Todo id 自增长开始" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}";
            Date handleStart = new Date();

            // 获取数据库连接
            conn = sql.getDataSource().getConnection();
            // 设置自动提交为false，在添加完所有要插入的数据之后，批量进行插入。
            conn.setAutoCommit(false);
            // 执行写锁定
            String lock = "lock table `todo` write;";
            // 预编译
            pstmt = conn.prepareStatement(lock);
            // 执行
            pstmt.execute();
            // 获取要生成的日程列表的长度
            String query1 = "select max(id) from `todo`;";
            // 预编译
            pstmt = conn.prepareStatement(query1);
            // 执行查询，获取结果集
            rs = pstmt.executeQuery();
            // 获取结果集
            Long oldAutoIncrement = null;
            // 取值
            while(rs.next()){
                oldAutoIncrement = rs.getLong(1) + 1;
            }
            if(oldAutoIncrement){
                // 获取要插入的日程的数量
                Long size = todoResultList.size();
                // 获取新的自增长值 = 老的自增长 + 插入日程的长度;
                Long newAutoIncrement = oldAutoIncrement + size;
                // 更新表
                String query2 = "alter table `todo` AUTO_INCREMENT = ?";
                // 预编译
                pstmt = conn.prepareStatement(query2);
                // 设置参数，改为新的自增长值
                pstmt.setLong(1,newAutoIncrement);
                // 运行 sql
                pstmt.execute();
            } else {
                throw new Exception("自增长设置失败!");
            }

            // 执行解锁
            String unlock = "unlock table;";
            //　预编译
            pstmt = conn.prepareStatement(unlock);
            //　执行
            pstmt.execute();

            // 进行提交
            conn.commit();
            // 重新设置提交
            conn.setAutoCommit(true);


            Date handleEnd = new Date();
            println "处理Todo id 自增长结束，耗时 : " + (handleEnd.getTime() - handleStart.getTime()) + "ms" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}";

            // 返回老的自增长的值
            return oldAutoIncrement;
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            ResourceUtil.resourceClose(conn,pstmt,rs);
        }
    }

    /**
     * 系统插入日程
     * @param todoResultList
     * @return
     */
    private def sysInsertTodo(List todoResultList){
        try {
            // 获取要插入的日程的数量
            Long size = todoResultList.size();
            println "处理 Todo id 自增长开始 : ${new Date().format("yyyy-MM-dd HH:mm:ss")}";

            conn = sql.getDataSource().getConnection();
            conn.setAutoCommit(false);
            Long userId
            if("pro" == grailsApplication.config.systemEnvironment){
                userId = grailsApplication.config.PRO_SYS_USER_ID;
            } else if("beta" == grailsApplication.config.systemEnvironment){
                userId = grailsApplication.config.BETA_SYS_USER_ID;
            } else {
                userId = grailsApplication.config.DEV_SYS_USER_ID;
            }
            String sysInsert = "INSERT INTO todo ( id, version, date_created, last_updated, p_container, p_display_order, p_finished_time, p_is_done, p_note, p_parent_id, p_planed_time, p_title, p_user_id, created_by_client, receiver_ids, receiver_names, sender_id, is_deleted, cid, repeat_tag_id, sender_todo_id, team_todo_read, clock_alert, kanban_item_id, is_revoke, closing_date_finished, end_date, start_date, todo_deploy_id, is_from_sub_todo, is_change_date, is_repeat_todo, alert_every_day, check_authority, dates, edit_authority, is_archived, inboxpcontainer, is_system ) VALUES (((select MAX(t.id) FROM `todo` AS t) + 1 ) + ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
            pstmt = conn.prepareStatement(sysInsert);
            pstmt.setLong(1,size);
            pstmt.setLong(2,1);
            pstmt.setTimestamp(3,new Timestamp(new Date().getTime()));
            pstmt.setTimestamp(4,new Timestamp(new Date().getTime()));
            pstmt.setString(5,"IE");
            pstmt.setLong(6,65535);
            pstmt.setTimestamp(7,null);
            pstmt.setBoolean(8,false);
            pstmt.setString(9,"");
            pstmt.setNull(10, Types.BIGINT);
            pstmt.setTimestamp(11,null);
            pstmt.setString(12,"${new Date().format("yyyy-MM-dd HH:mm:ss")}系统插入日程，用于重置日程自增长");
            pstmt.setLong(13,userId);
            pstmt.setString(14,"");
            pstmt.setString(15,"");
            pstmt.setString(16,"");
            pstmt.setNull(17,Types.BIGINT);
            pstmt.setBoolean(18,true);
            pstmt.setLong(19,-1);
            pstmt.setNull(20,Types.BIGINT);
            pstmt.setNull(21,Types.BIGINT);
            pstmt.setBoolean(22,false);
            pstmt.setString(23,null);
            pstmt.setNull(24,Types.BIGINT);
            pstmt.setBoolean(25,false);
            pstmt.setString(26,"");
            pstmt.setTimestamp(27,new Timestamp(new Date().clearTime().getTime()));
            pstmt.setTimestamp(28,new Timestamp(new Date().clearTime().getTime()));
            pstmt.setNull(29,Types.BIGINT);
            pstmt.setBoolean(30,false);
            pstmt.setBoolean(31,false);
            pstmt.setBoolean(32,false);
            pstmt.setBoolean(33,false);
            pstmt.setString(34,"public");
            pstmt.setString(35,"");
            pstmt.setString(36,"member");
            pstmt.setBoolean(37,false);
            pstmt.setString(38,"IE");
            pstmt.setBoolean(39,true);
            println "系统插入一条日程重置自增长";
            pstmt.executeUpdate();
            println "处理Todo id 自增长结束 : ${new Date().format("yyyy-MM-dd HH:mm:ss")}";

            String queryInsertId = "select t.id from `todo` as t where t.p_user_id = ? order by t.id desc limit 0,1;";
            pstmt = conn.prepareStatement(queryInsertId);
            pstmt.setLong(1,userId);
            rs = pstmt.executeQuery();
            Long oldAutoIncrement = null
            while(rs.next()){
                Todo.SYS_INSERT_TODO_ID = rs.getLong(1);
                println "查询系统插入的日程 id = ${Todo.SYS_INSERT_TODO_ID} : ${new Date().format("yyyy-MM-dd HH:mm:ss")}";
                oldAutoIncrement = rs.getLong(1) - size;
            }
            println "获取到老自增长值 old auto increment = ${oldAutoIncrement} : ${new Date().format("yyyy-MM-dd HH:mm:ss")}";

            // 进行提交
            conn.commit();
            // 重新设置提交
            conn.setAutoCommit(true);

            // 返回老的自增长的值
            return oldAutoIncrement;
        } catch (SQLException sqlE){
            sqlE.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            ResourceUtil.resourceClose(conn,pstmt,null);
        }
    }

    /**
     * 批量插入日程
     * @param list
     * @return
     */
    private def batchInsertTodo (def list = [],Long oldAutoIncrement) {
        // 老 id 和 新 id 的映射
        Map oldTodoIdAndNewTodoIdMap = [:];
        try{
            // 开始执行处理的时间 (把日程装入预编译对象)
            Date startHandle = new Date ();

            // 获取数据库连接
            conn = sql.getDataSource().getConnection();
            // 设置自动提交为false，在添加完所有要插入的数据之后，批量进行插入。
            conn.setAutoCommit(false);
            // sql
            String query = "INSERT INTO todo (id,version ,  date_created ,  last_updated ,  p_container,  p_display_order,  p_finished_time,  p_is_done,  p_note,  p_parent_id,  p_planed_time,  p_title,  p_user_id,  created_by_client,  receiver_ids,  receiver_names,  sender_id,  is_deleted,  cid,  repeat_tag_id,  sender_todo_id,  team_todo_read,  clock_alert,  kanban_item_id,  is_revoke,  closing_date_finished,  end_date,  start_date,  todo_deploy_id,  is_from_sub_todo,  is_change_date,  is_repeat_todo,  alert_every_day,  check_authority,  dates,  edit_authority,  is_archived,  inboxpcontainer, is_system)  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            // 预编译
            pstmt = conn.prepareStatement(query);
            list.each {Todo it ->
                // 向 预编译对象中添加要插入的日程信息
                TodoRepeatDs.prepareInsert(it, pstmt,oldTodoIdAndNewTodoIdMap,oldAutoIncrement);
                oldAutoIncrement ++ ;
            }

            // 结束处理
            Date  endHandle = new Date ()
            println('日程插入预处理时间（ms） : ' + (endHandle.getTime() - startHandle.getTime()) + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}")

            // 执行批量插入
            pstmt.executeBatch()
            // 提交
            conn.commit();

            // 结束插入
            Date endInsert = new Date()
            println('执行插入处理时间（ms）:' + (endInsert.getTime() - endHandle.getTime()) + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}")
            // 返回日程老 id 和新 id 的 Map
            return oldTodoIdAndNewTodoIdMap;
        } catch(SQLException e){
            e.printStackTrace();
        } finally {
            ResourceUtil.resourceClose(conn,pstmt,null);
        }
    }
    // ================================ generate end ====================================//
}
