package com.rishiqing

class TodoRepeatTag {

    transient static final int EVERY_DAY =1
    transient static final int EVERY_WEEK =2
    transient static final int EVERY_MONTH =3
    transient static final int EVERY_YEAR =4

    transient Todo todo

    Date dateCreated
    Date lastUpdated

//    周期性的日程通过这两个字段指定
    String repeatType
    String repeatBaseTime

//    离散时间通过这个字段指定
    String repeatDates

//    指定下一个重复时间
    Date nextRepeatTime

//    表示重复是否结束
    Boolean finished = false

    Long userId

    Boolean isCloseRepeat = true   //手机端是否关闭重复，true为关闭，false和null为开启

    Boolean alwaysRepeat = true     //一直重复

    Date repeatOverDate     //重复截止日期

    Boolean isLastDate = false

    String deletedDate  //删除未来重复的日程或者未来日程修改后独立出来，都会将此字段赋值，格式："yyyyMMdd,yyyyMMdd"

}
