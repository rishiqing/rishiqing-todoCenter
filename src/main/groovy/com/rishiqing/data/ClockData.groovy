package com.rishiqing.data

import com.rishiqing.Clock
import com.rishiqing.ds.ClockDs
import com.rishiqing.util.DateUtil
import com.rishiqing.util.ResourceUtil
import groovy.sql.Sql

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by Thinkpad on 2017/6/2.
 * 时间的查询及处理方法
 */
class ClockData {

    /** groovy sql 对象 */
    private Sql sql = null;
    /**  数据库连接对象 */
    private Connection conn = null;
    /** 预编译语言对象 */
    private PreparedStatement pstmt = null;
    /** 数据库结果集对象 */
    private ResultSet rs = null;

    /**
     * 构造方法
     * @param sql
     */
    ClockData (Sql sql){
        this.sql = sql;
    }

    // ================================  fetch job ======================================= //

    /**
     * 查询
     * @param oldTodoIdAndNewTodoIdMap 日程新旧 id 的映射
     * @return
     */
    def fetchRepeatTodoClock(List<Clock> repeatTodoNeedCreateClock,Map<Long,Long> oldTodoIdAndNewTodoIdMap) {

        // 开始查询的时间
        Date startFetchDate = new Date();

        // 在进行检索操作时使用的日期
        Date searchDate = new Date().clearTime();
        // 遍历日程新旧id 的Map，通过日程 id 值来查询其下面的提醒
        oldTodoIdAndNewTodoIdMap.each { oldTodoId, newTodoId ->
            // 查询此日程在查询的日期下是否被安排了时间和提醒
            def currentClock = Clock.createCriteria().get {
                and {
                    eq("todoId", oldTodoId);
                    eq("taskDate", searchDate);
                    eq("isDeleted",false);
                }
            }
            def clock = null;
            // 如果没有安排时间和提醒，则需要进行生成处理
            if (!currentClock) {
                // 获取这个日程最后一条提醒类型是“一直提醒”的时间，获取到
                clock = (Clock) Clock.createCriteria().get {
                    and {
                        eq("todoId", oldTodoId);
                        eq("alwaysAlert", true);
                        eq("isDeleted",false);
                    }
                    sqlRestriction("1=1 order by this_.id desc limit 1");
                }
                // 如果可以查询到
                if (clock) {
                    // 添加到需要创建的提醒队列中
                    repeatTodoNeedCreateClock.add(clock);
                }
            }
        }
        // 开始查询的时间
        Date endFetchDate = new Date();
        println("重复日程 Clock 查询耗时 : " + (startFetchDate.getTime() - endFetchDate.getTime()) + "ms");
        println("查询结果 :" + repeatTodoNeedCreateClock.size() +"个 Clock");
    }

    /**
     * 查询普通日程
     * @param needCreateClock 需要创建的日程列表
     */
    def fetchBaseTodoClock(List<Map> baseTodoNeedCreateClock,Sql sql) {
        // 开始查询的时间
        Date startFetchDate = new Date();
        // 在进行检索操作时使用的日期
        Date searchDate = new Date().clearTime();

        try {
            // 获取数据库连接
            conn = sql.getDataSource().getConnection();
            // sql 查询：日程是普通的日程或者重复日程但是关闭了重复的没有被删除的是一直提醒的 clock
            String query = "SELECT c.id AS id, c.clock_user_id AS clockUserId, c.start_time AS startTime, c.end_time AS endTime, c.always_alert AS alwaysAlert, t.id AS todoId, t.dates AS dates FROM clock AS c INNER JOIN todo AS t ON c.todo_id = t.id LEFT JOIN todo_repeat_tag AS trt ON t.repeat_tag_id = trt.id WHERE t.is_deleted = 0 AND ( trt.is_close_repeat = 1 OR ISNULL(t.repeat_tag_id) ) AND c.always_alert = 1 AND c.is_deleted = 0 AND ( ( t.dates LIKE ? OR ( t.start_date < ? AND t.end_date >= ? ) ) OR ( t.p_is_done = 0 AND ( t.dates NOT LIKE ? OR t.end_date < ? ) ) );";
            // 执行
            pstmt = conn.prepareStatement(query);
            // 设置参数
            pstmt.setString(1,"%${searchDate.format("yyyyMMdd")}%");
            pstmt.setString(2,searchDate.format("yyyy-MM-dd HH:mm:ss"));
            pstmt.setString(3,searchDate.format("yyyy-MM-dd HH:mm:ss"));
            pstmt.setString(4,"%${searchDate.format("yyyyMMdd")}%");
            pstmt.setString(5,searchDate.format("yyyy-MM-dd HH:mm:ss"));
            // 获取结果
            rs = pstmt.executeQuery();
            // 获取结果
            while(rs.next()){
                Map clock = [:];
                clock.put("id",rs.getLong("id"));
                clock.put("clockUserId",rs.getLong("clockUserId"))
                clock.put("startTime",rs.getString("startTime"));
                clock.put("endTime",rs.getString("endTime"));
                clock.put("todoId",rs.getLong("todoId"));
                clock.put("alwaysAlert",rs.getBoolean("alwaysAlert"));
                clock.put("todoId",rs.getLong("todoId"));

                String dates = rs.getString("dates");
                if(dates){
                    String datesFormat = DateUtil.datesFormat(dates,"yyyyMMdd");
                    Date maxDate = DateUtil.parseDate(datesFormat.split(",").last(),"yyyyMMdd");
                    // dates中包含当前搜索日期 或者 搜索日期大于最大日期
                    if(datesFormat.contains(searchDate.format("yyyyMMdd")) || searchDate > maxDate ){
                        // 执行添加
                        baseTodoNeedCreateClock.add(clock);
                    }
                } else {
                    // 执行添加
                    baseTodoNeedCreateClock.add(clock);
                }

            }

        } catch(SQLException e){
            e.printStackTrace();
            println ("查询失败")
        }

        // 开始查询的时间
        Date endFetchDate = new Date();
        println("普通日程 Clock 查询耗时 : " + (startFetchDate.getTime() - endFetchDate.getTime()) + "ms");
        println("查询结果 : "+ baseTodoNeedCreateClock.size() + "个 Clock");
    }


    // ============================ fetch job end ======================================//

    // ============================ generator job start ================================//

    /**
     * 执行创建操作
     * @param needCreateClock 需要创建的时间的列表
     * @param oldTodoIdAndNewTodoIdMap 日程新旧 id 组成的映射
     */
    def generator(List<Clock> repeatTodoNeedCreateClock,List<Map> baseTodoNeedCreateClock, Map<Long,Long> oldTodoIdAndNewTodoIdMap){
        Map<Long,Long> oldClockIdAndNewClockId = [:];
        // 处理自增长值并且获取到原来的自增长值
        Long oldAutoIncrement = handleClockAutoIncrement(repeatTodoNeedCreateClock,baseTodoNeedCreateClock);
        // 执行创建操作
        oldClockIdAndNewClockId = batchInsertClock(repeatTodoNeedCreateClock,baseTodoNeedCreateClock,oldTodoIdAndNewTodoIdMap,oldAutoIncrement);
        // 处理旧的一直提醒，把他们的 alwaysRepeat 关掉
        if(oldClockIdAndNewClockId){
            handleOldClockAlwaysAlert(oldClockIdAndNewClockId);
        }
        // 返回时间的 新旧 ID 映射
        return oldClockIdAndNewClockId;
    }

    /**
     * 处理时间的id自增长
     * @param needCreateClock 需要创建的时间的列表
     * @return
     */
    def handleClockAutoIncrement(List repeatTodoNeedCreateClock,List baseTodoNeedCreateClock){
        try{

            println "处理Clock id 自增长开始";
            Date handleStart = new Date();

            // 获取数据库连接对象
            conn = sql.getDataSource().getConnection();
            // 关闭自动提交
            conn.setAutoCommit(false);
            // 查询数据库中时间的数量(id 最大值)
            String query1 = "select max(id) from `clock` for update";
            // 预编译
            pstmt = conn.prepareStatement(query1);
            // 获取结果集
            rs = pstmt.executeQuery();
            // 获取时间的数量
            Long oldAutoIncrement = -1;
            while(rs.next()){
                oldAutoIncrement = rs.getLong(1) + 1;
            }

            // 获取要创建的 clock 的数量
            Long size = repeatTodoNeedCreateClock.size() + baseTodoNeedCreateClock.size();
            // 计算新的自增长的值
            Long newAutoIncrement = oldAutoIncrement + size
            // 更改自增长的值
            String query2 = "alter table `clock` AUTO_INCREMENT = ?";
            // 预编译
            pstmt = conn.prepareStatement(query2);
            // 设置参数
            pstmt.setLong(1,newAutoIncrement);
            // 执行
            pstmt.execute();
            // 提交
            conn.commit();

            Date handleEnd = new Date();
            println "处理Clock id 自增长结束，耗时 : " + (handleEnd.getTime() - handleStart.getTime()) + "ms";

            // 返回自增长的值
            return oldAutoIncrement;

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            ResourceUtil.resourceClose(conn,pstmt,rs);
        }
    }

    /**
     * 批量插入提醒
     * @param needCreateClock 需要创建的时间
     * @param oldTodoIdAndNewTodoIdMap 新旧日程 id 组成的映射
     * @param oldAutoIncrement 原来自增长的值
     * @return oldClockIdAndNewClockIdMap 新旧提醒的映射
     */
    private def batchInsertClock(List<Clock> repeatTodoNeedCreateClock,List<Map> baseTodoNeedCreateClock,Map<Long,Long> oldTodoIdAndNewTodoIdMap,Long oldAutoIncrement){
        // 新旧 clock id　组成的Map
        Map<Long,Long> oldClockIdAndNewClockIdMap = [:];
        try{
            // 开始执行处理的时间 (把日程装入预编译对象)
            Date startHandle = new Date ();

            // 获取数据库连接
            conn = sql.getDataSource().getConnection();
            // 设置自动提交为false，在添加完所有要插入的数据之后，批量进行插入。
            conn.setAutoCommit(false);
            // sql
            String query = "INSERT INTO `clock` ( `id`, `clock_user_id`, `date_created`, `end_time`, `is_deleted`, `start_time`, `task_date`, `todo_id`, `always_alert` ) VALUES (?,?,?,?,?,?,?,?,?);";
            // 预编译
            pstmt = conn.prepareStatement(query);
            // 数据组装
            for(int i = 0;i<repeatTodoNeedCreateClock.size();i++){
                Clock clock = repeatTodoNeedCreateClock.get(i);
                ClockDs.prepareInsert(clock,pstmt,oldClockIdAndNewClockIdMap,oldAutoIncrement,oldTodoIdAndNewTodoIdMap);
                oldAutoIncrement ++ ;
            }

            for(int i = 0;i<baseTodoNeedCreateClock.size();i++){
                Map clock = baseTodoNeedCreateClock.get(i);
                ClockDs.prepareInsert(clock,pstmt,oldClockIdAndNewClockIdMap,oldAutoIncrement);
                oldAutoIncrement ++ ;
            }

            // 结束处理
            Date  endHandle = new Date ()
            println('Clock 插入预处理耗时 : ' + (endHandle.getTime() - startHandle.getTime()) + "ms")

            // 执行批量插入操作
            pstmt.executeBatch();
            // 提交
            conn.commit();

            // 结束插入
            Date endInsert = new Date()
            println('Clock 执行插入耗时 :' + (endInsert.getTime() - endHandle.getTime()) + "ms");

            // 返回clock 新旧 id 的映射
            return oldClockIdAndNewClockIdMap;
        } catch(SQLException e){
            e.printStackTrace();
        } finally {
            ResourceUtil.resourceClose(conn,pstmt,rs);
        }
    }

    /**
     * 处理旧的clock 的 alwaysAlert 值，置为 false
     * @param oldClockIdAndNewClockId
     * @return
     */
    def handleOldClockAlwaysAlert(Map oldClockIdAndNewClockId){
        List oldClockIds = [];
        oldClockIdAndNewClockId.entrySet().each { es ->
            oldClockIds.add(es.key);
        }
        try{
            conn = sql.getDataSource().getConnection();
            conn.setAutoCommit(false);
            String query = "update `clock` as c set c.always_alert = 0 where c.id in ("+oldClockIds.join(",")+")";
            pstmt = conn.prepareStatement(query);
            pstmt.executeUpdate();
            conn.commit();
        } catch (SQLException e){
            e.printStackTrace();
            println "alwaysAlert 刷新失败!"
        } finally{
            ResourceUtil.resourceClose(conn,pstmt,null);
        }
    }

    // ============================ generator job end ================================//

}
