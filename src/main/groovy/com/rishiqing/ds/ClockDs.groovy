package com.rishiqing.ds

import com.rishiqing.Clock

import java.sql.Timestamp

/**
 * Created by Thinkpad on 2017/6/1.
 */
class ClockDs {
    static def toSqlMap(Clock clock, Long todoId){
        def map = [:];
        // 获取时间戳
        Timestamp timestampNow = new Timestamp(new Date().getTime()); // 当前时间
        Timestamp timestampToday = new Timestamp(new Date().clearTime().getTime()); // 今天日期

        map.clockUserId         = clock.clockUserId;
        map.dateCreated         = timestampNow;
        map.endTime             = clock.endTime;
        map.startTime           = clock.startTime;
        map.isDeleted           = clock.isDeleted;
        map.taskDate            = timestampToday;
        map.todoId              = clock.todoId;
        map.alwaysAlert         = clock.alwaysAlert;

        return map;
    }
}
