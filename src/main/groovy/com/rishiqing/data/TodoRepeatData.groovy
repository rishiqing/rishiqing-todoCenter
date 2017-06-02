package com.rishiqing.data

import com.rishiqing.Todo
import com.rishiqing.TodoRepeatTag
import com.rishiqing.ds.TodoRepeatDs
import com.rishiqing.util.CommonUtil
import com.rishiqing.util.ResourceUtil
import groovy.sql.Sql
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException;


/**
 * Created by solax on 2017-1-6.
 *
 */
class TodoRepeatData {

    // groovy sql 对象
    private Sql sql = null;
    // 数据库连接对象
    private Connection conn = null;
    // 预编译语言对象
    private PreparedStatement pstmt = null;
    // 数据库结果集对象
    private ResultSet rs = null;
    // 构造
    TodoRepeatData (Sql sql) {
        this.sql = sql
    }

    // ================================== fetch start =======================================//

    /**
     * 查询器，用来查询需要创建重复日程的日程信息。
     * @return
     */
    def fetch() {
        // 开始查询的日期和时间
        Date startSearch =  new Date();

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

        Date endSearch = new Date();  // 用来记录重复日程标记结束查询的时间
        // 搜索时长
        println("search repeatTag time: " + (endSearch.getTime() - startSearch. getTime()));

        // 获取list的长度
        Integer listSize = repeatTagList ? repeatTagList.size() : 0;
        // 重复标记的数量，每个标记对应一个重复的日程。
        println('repeatTagList size : ' + listSize);

        // 阀值，用来计算查询结果的百分比使用
        Integer i = 0;

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

                // 如果日程存在，那么组成 map 添加到需要创建的日程列表里。
                if(todo){
                    // 把基本信息装入 map ，添加到需要创建的日程的列表中
                    Map map = [todo: todo,repeatTag: repeatTag,date: searchDate];
                    needCreateTodos.add(map);
                }
                // 阀值 + 1
                i ++;
                // 计算百分比并输出，检测查询什么时候完成。
                CommonUtil.percent(listSize, i);

            }catch(Exception e) {
                e.printStackTrace();
                println "${startSearch.format("yyyy-MM-dd")}这天对应的重复id为${repeatTag.id}生成失败。";
            }
        }
        // 完成所有需要创建的重复日程的查询
        Date endFetch = new Date();
        println("fetch finish : " + (endFetch.getTime() - endSearch.getTime()));
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

        StringBuffer todoIdsSb = new StringBuffer()
        list.each { it ->
            // 经过各种判断，如果确定需要保存到数据库，  则存入resultList中
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
        println('todo insert list size: ' + todoResultList.size())

        // 进行重置 id 值的操作，把当前需要创建重复的日程的空间预留出来
        Long oldAutoIncrement = handleTodoAutoIncrement(todoResultList);

        // 执行日程批量插入，返回日程新老 id 映射。
        Map oldTodoIdAndNewTodoIdMap = batchInsertTodo(todoResultList,oldAutoIncrement);

        // 打开重复日程的创建开关 (isRepeatTodo)，当 isRepeatTod0 = 1是，第一天的日程将不显示延期。
        String todoIds = todoIdsSb.toString();
        if(todoIds && !"".equals(todoIds)){
            println "todo update isRepeatTodo start"
            sql.executeUpdate("UPDATE  todo set is_repeat_todo=1 where id in ("+(todoIds.endsWith(",")?todoIds.substring(0,todoIds.length()-1):todoIds)+")")
            println "todo update isRepeatTodo end"
        }
        // 返回日程新老 Id 映射
        return oldTodoIdAndNewTodoIdMap;
    }

    /**
     * 处理日程自增长的值
     * @param todoResultList
     * @return
     */
    private def handleTodoAutoIncrement(List todoResultList){
        try{
            // 获取数据库连接
            conn = sql.getDataSource().getConnection();
            // 设置自动提交为false，在添加完所有要插入的数据之后，批量进行插入。
            conn.setAutoCommit(false);
            // 获取要生成的日程列表的长度
            String query1 = "select max(id) from `todo` as t for update";
            // 预编译
            pstmt = conn.prepareStatement(query1);
            // 执行查询，获取结果集
            rs = pstmt.executeQuery();
            // 获取结果集
            Long oldAutoIncrement = rs.getLong(1);
            // 获取要插入的日程的数量
            Long size = todoResultList.size();
            // 获取新的自增长值 = 老的自增长 + 插入日程的长度 + 1;
            Long newAutoIncrement = oldAutoIncrement + size + 1;
            // 更新表
            String query2 = "alter table `todo` AUTO_INCREMENT = ?";
            // 预编译
            pstmt = conn.prepareStatement(query2);
            // 设置参数，改为新的自增长值
            pstmt.setLong(1,newAutoIncrement);
            // 运行 sql
            pstmt.execute(query2);
            // 进行提交
            conn.commit();
            // 返回老的自增长的值
            return oldAutoIncrement;
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            ResourceUtil.resourceClose(conn,pstmt,rs);
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
                // 要插入的日程的 id
                Long todoId = oldAutoIncrement ++;
                // 向 预编译对象中添加要插入的日程信息
                TodoRepeatDs.prepareInsert(it, pstmt,oldTodoIdAndNewTodoIdMap,todoId)
            }

            // 结束处理
            Date  endHandle = new Date ()
            println('insert prepare complete time : ' + (endHandle.getTime() - startHandle.getTime()))

            // 执行批量插入
            pstmt.executeBatch()
            // 提交
            conn.commit();

            // 结束插入
            Date endInsert = new Date()
            println('executeBatch:' + (endInsert.getTime() - endHandle.getTime()))
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
