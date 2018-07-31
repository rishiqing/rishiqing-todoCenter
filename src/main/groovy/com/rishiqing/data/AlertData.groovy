package com.rishiqing.data

import com.rishiqing.Alert
import com.rishiqing.ds.AlertDs
import com.rishiqing.util.ResourceUtil
import groovy.sql.Sql

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by Thinkpad on 2017/6/2.
 * 提醒查询及处理方法
 */
class AlertData {

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
     */
    AlertData (Sql sql){
        this.sql = sql;
    }

    // =========================== fetch job start ================================ //

    def fetch(Map<Long,Long> oldClockIdAndNewClockIdMap){
        def needCreateAlerts = [];

        // 开始查询的时间
        Date startFetchDate = new Date();

        // 进行提醒的查询操作
        oldClockIdAndNewClockIdMap.each { oldClockId,newClockId ->
            // 查询原来的时间下的所有提醒
            def alerts = Alert.findAllByClockId(oldClockId.toString().toLong());
            // 装入需要创建的提醒列表
            needCreateAlerts.addAll(alerts);
        }

        // 开始查询的时间
        Date endFetchDate = new Date();
        println("Alert 检索耗时 : " + (endFetchDate.getTime() - startFetchDate.getTime()) + "ms" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}");
        println("需要生成的 Alert :" + needCreateAlerts.size() + "个");
        // 返回查询结果
        return needCreateAlerts;
    }

    // =========================== fetch job end ================================ //

    // =========================== generator job start ================================ //

    /**
     * 执行提醒的创建操作
     * @param needCreateAlerts 需要创建的提醒的个数
     * @param oldClockIdAndNewClockIdMap 新旧时间的 id 组成的映射
     * @return 批量创建的提醒的数量
     */
    def generator(List<Alert> needCreateAlerts, Map<Long,Long> oldClockIdAndNewClockIdMap){
        // 执行批量插入的操作，并且返回创建提醒的数量
        return batchInsertAlert(needCreateAlerts,oldClockIdAndNewClockIdMap);
    }

    /**
     * 批量创建提醒
     * @param needCreateAlerts 需要创建的提醒的个数
     * @param oldClockIdAndNewClockIdMap 新旧时间的 id 组成的映射
     * @return 批量创建的提醒的数量
     */
    def batchInsertAlert(List<Alert> needCreateAlerts,Map<Long,Long> oldClockIdAndNewClockIdMap){
        try {
            // 开始执行处理的时间 (把日程装入预编译对象)
            Date startHandle = new Date ();

            // 执行批量创建操作
            conn = sql.getDataSource().getConnection();
            // 关闭自动提交
            conn.setAutoCommit(false);
            // sql
            String query = "INSERT INTO `alert` (`alert_time`, `clock_id`, `date_created`, `is_user_defined`, `schedule` ) VALUES (?, ?, ?, ?, ?)";
            // 预编译
            pstmt = conn.prepareStatement(query);
            // 插入的提醒的数量计算
            Integer i = 0;
            // 设置参数
            needCreateAlerts.each{ alert ->
                AlertDs.prepareInsert(alert,pstmt,oldClockIdAndNewClockIdMap);
                i ++ ;
            }

            // 结束处理
            Date  endHandle = new Date ()
            println('Alert 插入预处理耗时 : ' + (endHandle.getTime() - startHandle.getTime()) + "ms" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}")

            // 执行
            pstmt.executeBatch();
            // 提交
            conn.commit();

            // 结束插入
            Date endInsert = new Date()
            println('Alert 执行插入耗时 : ' + (endInsert.getTime() - endHandle.getTime()) + "ms" + "     ${new Date().format("yyyy-MM-dd HH:mm:ss")}")

            // 返回插入数量
            return i;
        } catch (SQLException e){
            e.printStackTrace();
        } finally{
            ResourceUtil.resourceClose(conn,pstmt,rs);
        }
    }

    // =========================== generator job end ================================ //
}
