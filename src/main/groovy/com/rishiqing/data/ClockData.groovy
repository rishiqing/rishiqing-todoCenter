package com.rishiqing.data

import com.rishiqing.Alert
import com.rishiqing.Clock
import com.rishiqing.ds.ClockDs
import com.rishiqing.util.ResourceUtil
import groovy.sql.Sql

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by Thinkpad on 2017/6/2.
 * 时间的查询方法
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
     * @return
     */
    def fetch(Map<Long,Long> oldTodoIdAndNewTodoIdMap) {

        // 时间列表
        List<Clock> needCreateClock = [];

        // 开始查询的时间
        Date startFetchDate = new Date();

        // 在进行检索操作时使用的日期
        Date searchDate = new Date().clearTime();
        // 遍历日程新旧id 的Map
        oldTodoIdAndNewTodoIdMap.entrySet().each { oldTodoId, newTodoId ->
            // 查询此日程在查询的日期下是否被安排了时间和提醒
            def currentClock = Clock.createCriteria().get {
                and {
                    eq("todoId", oldTodoId);
                    eq("taskDate", searchDate);
                    eq("isDeleted",false);
                }
            }
            def clock = null;
            // 如果没有安排时间和提醒
            if (!currentClock) {
                // 获取这个日程最后一条提醒类型是“一直提醒”的时间，获取到
                clock = (Clock) Clock.createCriteria().get {
                    and {
                        eq("todoId", oldTodoId);
                        eq("alwaysAlert", true);
                        eq("isDelete",false);
                    }
                    sqlRestriction("1=1 order by this_.id desc limit 1");
                }
                // 如果可以查询到
                if (clock) {
                    needCreateClock.add(clock);
                }
            }
        }
        // 开始查询的时间
        Date endFetchDate = new Date();
        println("fetch finish : " + (startFetchDate.getTime() - endFetchDate.getTime()));

        // 返回查询结果
        return needCreateClock;
    }


    // ============================ fetch job end ======================================//

    // ============================ generator job start ================================//

    /**
     * 执行
     * @param needCreateClock
     * @param oldTodoIdAndNewTodoIdMap
     */
    def generator(List<Clock> needCreateClock,Map<Long,Long> oldTodoIdAndNewTodoIdMap){
        // 处理自增长值并且获取到原来的自增长值
        Long oldAutoIncrement = handleClockAutoIncrement(needCreateClock);
        // 执行创建操作
        Map<Long,Long> oldClockIdAndNewClockId = batchInsertClock(needCreateClock,oldTodoIdAndNewTodoIdMap,oldAutoIncrement);
        // 返回时间的 新旧 ID 映射
        return oldClockIdAndNewClockId;
    }

    def handleClockAutoIncrement(List needCreateClock){
        try{
            // 获取数据库连接对象
            conn = sql.getDataSource().getConnection();
            // 关闭自动提交
            conn.setAutoCommit(false);
            // 查询数据库中时间的数量
            String query1 = "select max(id) from `clock` for update";
            // 预编译
            pstmt = conn.prepareStatement(query1);
            // 获取结果集
            rs = pstmt.executeQuery();
            // 获取时间的数量
            Long oldAutoIncrement = rs.getLong(1) + 1;
            // 获取要创建的 clock 的数量
            Long size = needCreateClock.size();
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
            // 返回自增长的值
            return oldAutoIncrement;

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            ResourceUtil.resourceClose(conn,pstmt,rs);
        }
    }

    /**
     *  批量插入提醒
     */
    private def batchInsertClock(List<Clock> needCreateClock,Map<Long,Long> oldTodoIdAndNewTodoIdMap,Long oldAutoIncrement){
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
            String query = "INSERT INTO `clock` ( id, clock_user_id, date_created, end_time, is_deleted, start_time, task_date, todo_id, always_alert ) VALUES (?,?,?,?,?,?,?,?,?);";
            // 预编译
            pstmt = conn.prepareStatement(query);
            // 数据组装
            needCreateClock.each { clock ->
                ClockDs.prepareInsert(clock,pstmt,oldClockIdAndNewClockIdMap,oldAutoIncrement,oldTodoIdAndNewTodoIdMap);
                oldAutoIncrement ++ ;
            }

            // 结束处理
            Date  endHandle = new Date ()
            println('insert prepare complete time : ' + (endHandle.getTime() - startHandle.getTime()))

            // 执行批量插入操作
            pstmt.executeBatch();
            // 提交
            conn.commit();

            // 结束插入
            Date endInsert = new Date()
            println('executeBatch:' + (endInsert.getTime() - endHandle.getTime()))

            // 返回clock 新旧 id 的映射
            return oldClockIdAndNewClockIdMap;
        } catch(SQLException e){
            e.printStackTrace();
        } finally {
            ResourceUtil.resourceClose(conn,pstmt,rs);
        }
    }

    // ============================ generator job end ================================//

}
