package com.rishiqing

class Todo {

//    transient public static final long SYS_USER_ID = 686938;
    transient public static final long SYS_USER_ID = 2;
    // 需要维护的系统生成的日程 id
    transient public static long SYS_INSERT_TODO_ID = 0;
    /** 日程标题 */
    String pTitle
    /** 日程备注 */
    String pNote
    /** 日程是否完成 */
    boolean pIsDone = false
    /** 奇怪的字段 */
    Date pPlanedTime
    /** 完成时间 */
    Date pFinishedTime = null
    /** 排序字段 */
    Long pDisplayOrder = 0  //显示顺序
    /** 所属象限 */
    String pContainer = "inbox"  //所属容器，inbox/IE/IU/UE/UU
    /** 收纳箱日程所属象限 */
    String inboxPContainer = "IU"
    /** 所有被下发人的 ids */
    String receiverIds
    /** 下发人的名字 */
    String receiverNames
    /** 下发人的id */
    Long senderId
    /** 数据的创建时间　*/
    Date dateCreated
    /** 数据的最后更新时间 */
    Date lastUpdated
    /** 是否被删除 */
    boolean isDeleted = false
    /** */
    String cid = "-1"
    /** 在什么客户端创建的（移动端，web端） */
    String createdByClient = "web"
    /**  */
    Boolean teamTodoRead
    /** 闹钟字段（已废弃） */
    String clockAlert
    /** 日程开始时间 */
    Date startDate
    /** 日程结束时间 */
    Date endDate
    /**  */
    String  closingDateFinished
    /**  */
    Boolean isFromSubTodo = false
    /**  */
    Boolean isChangeDate = false
    /** 是否被系统删除 */
    Boolean isRevoke = false
    /** 是否需要创建重复日程标记 */
    Boolean isRepeatTodo = false
    /** 是否每天提醒（已废弃） */
    Boolean alertEveryDay = false
    /** 查看权限 */
    String checkAuthority = "public"
    /** 编辑权限 */
    String editAuthority = "member"
    /** 离散日期 */
    String dates
    /**  */
    Long pParentId
    /**  */
    Boolean isArchived = false
    /** 日程重复标记的 id */
    Long repeatTagId
    /** 日程所属用户的 id */
    Long pUserId
    /**  */
    Long senderTodoId
    /** 日程关联 kanbanItem 的 id */
    Long kanbanItemId
    /**  */
    boolean isSystem = 1
    /** 日程关联表 */
    Long todoDeployId
//
//    static hasMany = [clock:Clock];
//    static belongsTo = [todoDeploy: TodoDeploy]
    /**
     * map转 domain
     * @param map
     */
    static  def toTodoDomain(def map) {
        Todo todo  = new Todo()
        todo.pContainer         =   map.p_container
        todo.pNote              =   map.p_note
        todo.pParentId          =   map.p_parent_id
        todo.pTitle             =   map.p_title
        todo.pUserId            =   map.p_user_id
        todo.repeatTagId        =   map.repeat_tag_id
        todo.clockAlert         =   map.clock_alert
        todo.endDate            =   map.end_date
        todo.startDate          =   map.start_date
//        todo.todoDeployId       =   map.todo_deploy_id
        todo.isRepeatTodo       =   map.is_repeat_todo
        todo.alertEveryDay      =   map.alert_every_day
        todo.checkAuthority     =   map.check_authority
        todo.dates               =   map.dates
        todo.editAuthority      =   map.edit_authority
        todo.inboxPContainer    =   map.inboxPContainer
        return todo
    }
}
