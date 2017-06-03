package com.rishiqing.ds

import com.rishiqing.Clock
import com.rishiqing.util.CommonUtil

import java.sql.PreparedStatement
import java.sql.Timestamp

/**
 * Created by Thinkpad on 2017/6/1.
 * 负责组装 clock 的参数，用于新的 Clock 的插入
 */
class ClockDs {
    /**
     * Clock 参数组装
     * @param clock
     * @param clockId
     * @return
     */
    static def toSqlMap(Clock clock, Long clockId,Map<Long,Long> oldTodoIdAndNewTodoIdMap){
        def map = [:];
        // 获取时间戳
        Timestamp timestampNow = new Timestamp(new Date().getTime()); // 当前时间
        Timestamp timestampToday = new Timestamp(new Date().clearTime().getTime()); // 今天日期
        // 数据组装
        map.id                  = clockId;
        map.clockUserId         = clock.clockUserId;
        map.dateCreated         = timestampNow;
        map.endTime             = clock.endTime;
        map.isDeleted           = false;
        map.startTime           = clock.startTime;
        map.taskDate            = timestampToday;
        map.todoId              = oldTodoIdAndNewTodoIdMap.get(clock.todoId);
        map.alwaysAlert         = true;
        // 返回组装好的数据
        return map;
    }

    /**
     * 把 Clock 的参数转入预编译语言对象
     * @param clock
     * @param pstmt
     * @param oldClockIdAndNewClockIdMap
     * @param clockId
     */
    static def prepareInsert(Clock clock,PreparedStatement pstmt,Map<Long,Long> oldClockIdAndNewClockIdMap,Long clockId,Map<Long,Long> oldTodoIdAndNewTodoIdMap){
        // 组装 Clock 新旧 id 的映射
        oldClockIdAndNewClockIdMap.put(clock.id,clockId);
        // 转换成 clock 信息的Map
        def clockMap = toSqlMap(clock,clockId,oldTodoIdAndNewTodoIdMap);
        // 处理参数并装入预编译对象
        Integer i = 1;
        // 遍历
        clockMap.each { key,value ->
            CommonUtil.setPstmtParams(value,pstmt,i);
            i ++ ;
        }
        // 批量装入
        pstmt.addBatch();
    }
}
