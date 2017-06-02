package com.rishiqing.data

import com.rishiqing.Clock
import com.rishiqing.util.ResourceUtil
import groovy.sql.Sql

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * Created by Thinkpad on 2017/6/2.
 */
class ClockData {

    // groovy sql 对象
    private Sql sql = null;
    // 数据库连接对象
    private Connection conn = null;
    // 预编译语言对象
    private PreparedStatement pstmt = null;
    // 数据库结果集对象
    private ResultSet rs = null;

    ClockData (Sql sql){
        this.sql = sql;
    }

    /**
     * 查询
     * @return
     */
    def fetch(){
        // 查询此日程在查询的日期下是否被安排了时间和提醒
        def currentClock = Clock.createCriteria().get {
            and{
                eq("todoId",todo.id);
                eq("taskDate",searchDate);
            }
        }
        def clock = null;
        if(!currentClock){
            // 获取这个日程最后一条提醒类型是“一直提醒”的时间，获取到
            clock = Clock.createCriteria().get {
                and{
                    eq("todoId",todo.id);
                    eq("alwaysAlert",true);
                }
                sqlRestriction("1=1 order by this_.id desc limit 1");
            }
        }
    }

    /**
     *  批量插入提醒
     */
    private static def batchInsertClock(Map todoIdAndClockMap,Map oldIdAndNewIdMap){
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try{

        } catch(Exception e){
            e.printStackTrace();
        } finally {
            if(conn){
                conn.close();
            }
            if(pstmt){
                pstmt.close();
            }
            if(rs){
                rs.close();
            }
        }
    }

}
