package com.rishiqing

class Clock {

    /**　记录创建时间 */
    Date dateCreated;
    /** 闹钟的开始时间 */
    String startTime;  // 23:00
    /** 闹钟的结束时间 */
    String endTime;
    /** 创建闹钟的时间 */
    Date taskDate;  // 用来和任务的开始和结束时间进行比较
    /** 是否被删除 */
    Boolean isDeleted = false;
    /**　时间归属的用户的的 id */
    Long clockUserId;
    /** 所属日程的 id */
    Long todoId;
    /** 标记是否是一直提醒
     * <p> 默认不是一直提醒（即单天的提醒），如果设置了一直提醒，该字段为 true </p> */
    Boolean alwaysAlert = false;
    /** 惯例配置 */
    static mapping = {
        version(false);
    }

//    /** 一对多 */
//    static hasMany = [
//            alert:Alert,  // 和提醒表一对多:一个有开始时间，结束时间的闹钟，可以有多个提醒
//
//    ]
//    /** 一对一 */
//    static belongsTo = [
//            todo:Todo,   // 闹钟归属的日程
//    ]
}

