package com.rishiqing.util

import com.rishiqing.Todo
import com.rishiqing.TodoRepeatTag

import java.sql.PreparedStatement
import java.sql.Timestamp

/**
 * Created by Thinkpad on 2017/6/2.
 * 公共工具类
 */
class CommonUtil {

    /**
     * 通过一个重复找需要生成的那天
     * @param tag          重复
     * @param secondDay    需要生成新日程的那一天
     */
    public static Boolean needCreateForToday(TodoRepeatTag tag, Date secondDay){
        // 参数错误
        if(!tag || !secondDay){
            return false
        }
        // 如果重复标记基本时间没有，或者已经被关闭，或者没有重复类型（每天每周每月每年）
        if(!tag.repeatBaseTime || tag.isCloseRepeat || !tag.repeatType){
            return false
        }
        // 获取重复类型（每天每周每月每年）
        String repeatType = tag.repeatType
        //每天重复
        if("everyDay" == repeatType){

            // 获取重复基本日期
            Date repeatBaseTime = Date.parse("yyyyMMdd",tag.repeatBaseTime)
            // 重复的基本日期是否 < 生成重复的日期
            return repeatBaseTime.getTime()<secondDay.getTime()

        }// 每周重复
        else if("everyWeek"== repeatType){
            // 这一天是否和每周重复的那个星期是相同的一天
            return isSameDayInOtherWeek(tag.repeatBaseTime.split(","),secondDay);

        }// 如果是每月重复
        else if("everyMonth" == repeatType) {
            /*
             * 如果是最后一天重复则查看secondDay是否为最后一天
             * 如果不是最后一天重复则查看baseTime是否有需要创建日程的
             */
            return tag.isLastDate?getLastDay(secondDay).getTime()==secondDay.getTime():isSameDayInOtherMonth(tag.repeatBaseTime.split(","),secondDay);
        }// 如果是每年重复
        else if("everyYear" == repeatType ){
            // 这一天是每年重复的相同的一天吗
            return isSameDayInOtherYear(tag.repeatBaseTime,secondDay);

        }
        return false;
    }

    /**
     * 数组中的某个日期对应的星期，是否与 targetDate 对应的星期是相同的
     * @param baseDates
     * @param targetDate
     * @return
     */
    public static Boolean isSameDayInOtherWeek(String[] baseDates,Date targetDate){
        if(!baseDates || !targetDate){
            return false
        }
        // 获取当前日期的一个日历实体
        Calendar cTarget = targetDate.toCalendar()
        for(int i=0;i<baseDates.length;i++){
            // 获取第一个基本日期 (repeatBaseTime 中的一个)
            Date baseDate = Date.parse("yyyyMMdd",baseDates[i])
            if(baseDate == null){
                continue
            }
            // 转换成日历格式
            Calendar cByI = baseDate.toCalendar()
            // 如果这个基本日期的星期和 targetDate 对应的星期是相同的，返回true
            if(cTarget.get(Calendar.DAY_OF_WEEK)==cByI.get(Calendar.DAY_OF_WEEK)){
                return true
            }
        }
        // 否则
        return false
    }
    /**
     * 数组中的某一天是否和 targetDate 所对应的那一天是同一天（日期相同）
     * @param baseDates
     * @param targetDate
     * @return
     */
    public static Boolean isSameDayInOtherMonth(String[] baseDates,Date targetDate){
        if(!baseDates || !targetDate){
            return false
        }
        Calendar cTarget = targetDate.toCalendar()
        for(int i=0;i<baseDates.length;i++){
            Date baseDate = Date.parse("yyyyMMdd",baseDates[i])
            if(baseDate == null){
                continue
            }
            Calendar cByI = baseDate.toCalendar()
            if(cTarget.get(Calendar.DAY_OF_MONTH)==cByI.get(Calendar.DAY_OF_MONTH)){
                return true
            }
        }
        return false
    }
    /**
     * 数组中的某一天是否和targetDate的日是相同的
     * @param baseDates
     * @param targetDate
     * @return
     */
    public static Boolean isSameDayInOtherYear(String baseDate,Date targetDate){
        // 参数验证
        if(!baseDate || !targetDate){
            return false
        }
        // 把 targetDate 转换成日历格式
        Calendar cTarget = targetDate.toCalendar()
        // 把 repeatBaseTime 中的日期转换成日期格式
        Date b = Date.parse("yyyyMMdd",baseDate)
        if(b == null){
            return false
        }
        // 再次转换成日历格式
        Calendar cByI = b.toCalendar()
        // 比较是否是下一年的今天，相同则为 true
        if(cTarget.get(Calendar.DAY_OF_YEAR)==cByI.get(Calendar.DAY_OF_YEAR)){
            return true
        }
        // 否则
        return false
    }

    /**
     * 用来坚检测复日程查询的百分比。当为 100%　时，证明所有重复日程已经查询完毕
     * @param size
     * @param index
     */
    public static void percent (int size, int index) {
        int a = size
        if (index % a  == 0) {
            println('检索日程完成百分比 : (' + index / a/100 + '%)' )
        }
    }

    /**
     * 获取某天的月份中最后一天
     * @param date
     * @return
     */
    public static Date getLastDay(Date date){
        def cal = date.toCalendar()
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, 1, 0, 0, 0)
        cal.add(Calendar.DAY_OF_MONTH,-1)
        return cal.getTime()
    }

    /**
     * 此日程是否应该进行创建重复日程的操作
     * @param todo
     * @param it
     * @param date
     * @return
     */
    public static Boolean shouldBeGenerated(Todo todo, TodoRepeatTag it, Date date){
        // 参数验证
        if(!todo || !it || !date){
            return false
        }
        // 查看当前日程是否在收纳箱，如果在，那么不为它创建
        if("inbox" == todo.pContainer){
            return false
        }
        // 查看日程是否没有 pPlanedTime ，没有则不创建
//        if(!todo.pPlanedTime){
//            return false
//        }
        // 如果日程没有起止时间，则不创建
        if(!todo.startDate || !todo.endDate) {
            return false
        }
        // 日程没有被完成
        if(!todo.pIsDone){
            // 查看是否有开始和结束时间
            if(todo.startDate && todo.endDate){
                // 如果date1的时间在上一条日程的截止时间内，则不生成。现在重复日程的逻辑，一设置重复日程，这个日程就
                // 变成了单天的日程（在创建重复的那一天），比如：20170101 这天设置的重复，
                // 那么 startDate = endDate = 20170101，因此，date 是不在这个区间里的（因为今天是 20170102）
                if(todo.startDate.getTime()<=date.getTime()
                        && todo.endDate.getTime()>=date.getTime()) return false
            }else{
                //如果上一条日程的时间和要生成的日期相等则不生成
                if(todo.pPlanedTime.getTime()>=date.getTime()) return false
            }
        }
        //如果重复里已被删除的日期里包含了date，则不生成
        if(it.deletedDate && it.deletedDate.split(",").contains(date.format("yyyyMMdd"))) return false
        return true
    }

    /**
     * 数据装入预处理对象
     * @param value 值
     * @param pstmt 预编译语言对象
     * @param i 装入位置
     * @return
     */
    public static def setPstmtParams(def value,PreparedStatement pstmt,Integer i){

        if (value instanceof  Timestamp) { // 转时间戳
            // 设置第 i 个位置的值为时间戳格式
            pstmt.setTimestamp(i, value);
        } else if (value instanceof  Integer) {// 转 int
            // 设置第 i 个位置的值为int
            pstmt.setInt(i, value);
        } else if (value instanceof  Long) { // 转 long
            // 设置第 i 个位置的值为long
            pstmt.setLong(i, value);
        } else if (value instanceof  Boolean) {  // 转 boolean
            // 设置第 i 个位置的值为boolean
            pstmt.setBoolean(i, value);
        }else {
            // 设置第 i 个位置的值为任意格式
            pstmt.setString(i, value);// 转 字符串
        }

    }
}
