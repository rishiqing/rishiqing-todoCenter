package com.rishiqing

class Alert {

    /** 记录的创建时间 */
    Date dateCreated;
    /** 是否是用户自定义 */
    Boolean isUserDefined = false;
    /**
     * 提醒调度时间 <br>
     * <p> 目前的格式有：（注意，时间值不固定，前缀固定）
     *  begin_-30_min   ：开始前30分钟响铃
     *  begin_30_min    ：开始后30分钟响铃
     *  end_-30_min     ：结束前30分钟提醒
     *  end_30_hour      ：结束后30小时提醒
     */
    String schedule;
    /** 闹钟响铃时间　*/
    Date alertTime;   // 这个值需要后台自己计算并存入

    /** 惯例配置 */
    static mapping = {
        version(false);
    }
    /** 一对一 */
    static belongsTo = [
            clock:Clock  // 一个提醒归属于一个闹钟
    ]


    /**
     * 计算提醒时间
     */
    def getAlertTime(Date taskDate,String startTime,String endTime,String schedule){
        // 参数验证
        if(!taskDate){
            return ReturnMapV2Util.missingParameterError("taskDate");
        }
        if(!startTime && !endTime){
            return ReturnMapV2Util.missingParameterError("s or e");
        }
        if(!schedule){
            return ReturnMapV2Util.missingParameterError("schedule");
        }
        String date = taskDate.format("yyyy-MM-dd");
        String head = schedule.split("_")[0]; // begin or end
        Integer num = schedule.split("_")[1].toInteger(); // 数量
        String type = schedule.split("_")[2]; // hour or min
        // 提醒时间的时间戳
        Long timestamp;
        if("begin".equals(head)){
            if(!startTime){
                return ReturnMapV2Util.dataNotSupportError("请先设置开始时间");
            }
            // 2017-01-01 23:35:00
            Date point = DateUtil.parseDate(date+" "+startTime+":00");
            timestamp = DateUtil.getTimestamp(point);
        } else if("end".equals(head)){
            if(!endTime){
                return ReturnMapV2Util.dataNotSupportError("请先设置结束时间");
            }
            // 2017-01-01 23:35:00
            Date point = DateUtil.parseDate(date+" "+endTime+":00");
            timestamp = DateUtil.getTimestamp(point);
        } else {
            // 数据格式不支持
            return ReturnMapV2Util.dataNotSupportError();
        }
        if(num>0){ // 在节点 point 后提醒
            if("min".equals(type)){
                timestamp = timestamp + DateUtil.getSecond(num,"min");
            } else if("hour".equals(type)){
                timestamp = timestamp + DateUtil.getSecond(num,"hour");
            }
        } else if (num<0){  // 在节点 point 前提醒
            if("min".equals(type)){
                timestamp = timestamp - DateUtil.getSecond(Math.abs(num),"min");
            } else if("hour".equals(type)){
                timestamp = timestamp - DateUtil.getSecond(Math.abs(num),"hour");
            }
        }
        // 时间戳存在，则把时间戳转换为日期格式，进行返回
        if(timestamp){
            return [data:[alertTime:DateUtil.getDate(timestamp)]];
        } else {
            return ReturnMapV2Util.numericValueError();
        }
    }
}
