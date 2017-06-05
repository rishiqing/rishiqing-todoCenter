package com.rishiqing.ds

import com.rishiqing.Alert
import com.rishiqing.Clock
import com.rishiqing.util.CommonUtil
import com.rishiqing.util.DateUtil

import java.sql.PreparedStatement
import java.sql.Timestamp

/**
 * Created by Thinkpad on 2017/6/1.
 */
class AlertDs {

    /**
     * 数据组装
     */
    static def toSqlMap(Alert alert,Map oldClockIdAndNewClockIdMap){
        def map = [:];
        // 获取时间戳
        Timestamp timestampNow = new Timestamp(new Date().getTime()); // 当前时间
        Timestamp timestampToday = new Timestamp(new Date().clearTime().getTime()); // 今天日期
        // 获取到新创建的时间
        Clock newClock = Clock.findById(oldClockIdAndNewClockIdMap.get(alert.clockId).toString().toLong());

        // 组装
        map.alertTime       = getAlertTime(newClock.taskDate,newClock.startTime,newClock.endTime,alert.schedule);
        map.clockId         = newClock.id;
        map.dateCreated     = timestampNow;
        map.isUserDefined   = alert.isUserDefined
        map.schedule        = alert.schedule;
        // 返回
        return map;
    }

    /**
     * 根据 clock 信息计算 alertTime 的方法
     * @param taskDate
     * @param startTime
     * @param endTime
     * @param schedule
     * @return
     */
    private static def getAlertTime(Date taskDate,String startTime,String endTime,String schedule){
        String date = taskDate.format("yyyy-MM-dd");
        String head = schedule.split("_")[0]; // begin or end
        Integer num = schedule.split("_")[1].toInteger(); // 数量
        String type = schedule.split("_")[2]; // hour or min
        // 提醒时间的时间戳
        Long timestamp;
        if("begin" == head){
            // 2017-01-01 23:35:00
            Date point = DateUtil.parseDate(date+" "+startTime+":00");
            timestamp = DateUtil.getTimestamp(point);
        } else {
            // 2017-01-01 23:35:00
            Date point = DateUtil.parseDate(date+" "+endTime+":00");
            timestamp = DateUtil.getTimestamp(point);
        }
        if(num>0){ // 在节点 point 后提醒
            if("min"==type){
                timestamp = timestamp + DateUtil.getSecond(num,"min");
            } else if("hour"==type){
                timestamp = timestamp + DateUtil.getSecond(num,"hour");
            }
        } else if (num<0){  // 在节点 point 前提醒
            if("min"==type){
                timestamp = timestamp - DateUtil.getSecond(Math.abs(num),"min");
            } else if("hour"==type){
                timestamp = timestamp - DateUtil.getSecond(Math.abs(num),"hour");
            }
        }
        // 时间戳存在，则把时间戳转换为日期格式，进行返回
        return DateUtil.getDate(timestamp);
    }

    /**
     * 转入预编译语言对象
     * @return
     */
    static def prepareInsert(Alert alert, PreparedStatement pstmt,Map<Long,Long> oldClockIdAndNewClockIdMap){
        // 组装 alert 信息
        def alertMap = toSqlMap(alert,oldClockIdAndNewClockIdMap);
        // 参数处理
        Integer i = 1;
        // 遍历
        alertMap.each { key,value ->
            CommonUtil.setPstmtParams(value,pstmt,i);
            i++ ;
        }
        // 批量装入
        pstmt.addBatch();
    }
}
