package com.rishiqing

class Todo {

    String pTitle
    String pNote
    boolean pIsDone = false
    Date pPlanedTime
    Date pFinishedTime
    Long pDisplayOrder = 0  //显示顺序
    String pContainer = "inbox"  //所属容器，inbox/IE/IU/UE/UU
    String inboxPContainer = "IU"
    String receiverIds
    String receiverNames
    Long senderId
    Date dateCreated
    Date lastUpdated
    boolean isDeleted = false
    String cid = "-1"
    String createdByClient = "web"
    Boolean teamTodoRead
    String clockAlert
    Date startDate
    Date endDate
    String  closingDateFinished
    Boolean isFromSubTodo = false
    Boolean isChangeDate = false
    Boolean isRevoke = false
    Boolean isRepeatTodo = false
    Boolean alertEveryDay = false
    String checkAuthority = "public"
    String editAuthority = "member"
    String dates
    Long pParentId
    Boolean isArchived = false
    Long repeatTagId
    Long pUserId
    Long senderTodoId
    Long kanbanItemId
    boolean isSystem = 1

    static belongsTo = [todoDeploy: TodoDeploy]
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
