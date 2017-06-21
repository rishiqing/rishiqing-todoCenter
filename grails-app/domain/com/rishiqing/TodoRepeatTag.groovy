package com.rishiqing

class TodoRepeatTag {

    transient static final int EVERY_DAY =1
    transient static final int EVERY_WEEK =2
    transient static final int EVERY_MONTH =3
    transient static final int EVERY_YEAR =4

    transient Todo todo
    /** 记录创建时间 */
    Date dateCreated
    /** 记录最后更新时间 */
    Date lastUpdated

//    周期性的日程通过这两个字段指定
    /** 重复日程的类型 */
    String repeatType
    /** 重复基本日期 */
    String repeatBaseTime

//    离散时间通过这个字段指定
    /** 重复的离散日期 */
    String repeatDates

//    指定下一个重复时间
    /** 下一次重复时间 (已废弃) */
    Date nextRepeatTime

//    表示重复是否结束
    /** 重复是否结束(已废弃) */
    Boolean finished = false
    /** 重复标记创建的用户 */
    Long userId
    /** 是否关闭重复 */
    Boolean isCloseRepeat = true   //手机端是否关闭重复，true为关闭，false和null为开启
    /** 是否是一直重复 */
    Boolean alwaysRepeat = true     //一直重复
    /** 重复日程的截至日期 */
    Date repeatOverDate     //重复截止日期
    /** 是否是当前月的最后一天 */
    Boolean isLastDate = false
    /** 重复日程的删除日期 */
    String deletedDate  //删除未来重复的日程或者未来日程修改后独立出来，都会将此字段赋值，格式："yyyyMMdd,yyyyMMdd"

}
