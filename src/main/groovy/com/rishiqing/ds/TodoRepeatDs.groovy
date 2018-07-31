package com.rishiqing.ds

import com.rishiqing.Todo
import com.rishiqing.TodoDeploy
import com.rishiqing.util.CommonUtil

import java.sql.PreparedStatement
import java.sql.Timestamp

/**
 * Created by solax on 2017-1-10.
 * Update by codingR on 2017-06-03.
 * 日程数据组装
 */
class TodoRepeatDs {
    /**
     * 转换为 sql 需要的数据
     * @param
     */
    static def toSqlMap(Todo todo,Long todoId){
        // 查询日程信息关联表是否存在
        TodoDeploy todoDeploy = TodoDeploy.get(todo.todoDeployId);
        // 存在标题和备注获取 todoDeploy 表中的，否则获取日程的
        String pTitle = todoDeploy?todoDeploy.pTitle:todo.pTitle;
        String pNote = todoDeploy?todoDeploy.pNote:todo.pNote;
        def map = [:]
        // 获取时间戳
        Timestamp timestampNow = new Timestamp(new Date().getTime()); // 当前时间
        Timestamp timestampToday = new Timestamp(new Date().clearTime().getTime()); // 今天日期

        map.id                          =   todoId
        map.version                     =   0
        map.dateCreated                 =   timestampNow
        map.lastUpdated                 =   timestampNow
        map.pContainer                  =   todo.pContainer
        map.pDisplayOrder               =   todo.pDisplayOrder
        map.pFinishedTime               =   null
        map.pIsDone                     =   0
        map.pNote                       =   pNote
        map.pParentId                   =   todo.pParentId
        map.pPlanedTime                 =   null
        map.pTitle                      =   pTitle
        map.pUserId                     =   todo.pUserId
        map.createdByClient             =   'web'
        map.receiverIds                 =   null
        map.receiverNames               =   null
        map.senderId                    =   null
        map.isDeleted                   =   0
        map.cid                         =   '-1'
        map.repeatTagId                 =   todo.repeatTagId
        map.senderTodoId                =   null
        map.teamTodoRead                =   null
        map.clockAlert                  =   todo.clockAlert
        map.kanbanItemId                =   todo.kanbanItemId
        map.isRevoke                    =   0
        map.closingDateFinished         =   null
        map.endDate                     =   timestampToday
        map.startDate                   =   timestampToday
        map.todoDeployId                =   null
        map.isFromSubTodo               =   0
        map.isChangeDate                =   0
        map.isRepeatTodo                =   false
        map.alertEveryDay               =   todo.alertEveryDay
        map.checkAuthority              =   todo.checkAuthority
        map.dates                       =   todo.dates
        map.editAuthority               =   todo.editAuthority
        map.isArchived                  =   todo.isArchived
        map.inboxPContainer             =   todo.inboxPContainer
        map.isSystem                    =   true

        return map
    }

    /**
     * 把要插入的数据导入到预编译对象中
     * @param todo
     * @param pstmt
     */
    static void prepareInsert(Todo todo, PreparedStatement pstmt,Map oldIdAndNewIdMap,Long todoId) {
        // 把日程老 id 和 新 id 装入 Map
        oldIdAndNewIdMap.put(todo.id,todoId);
        // 把 todo转换为 map，此 map 负责向预处理对象中录入数据
        def todoMap = toSqlMap(todo,todoId);
        // 设定阀值，用来指定插入位置，比如 todo的第一个参数为 version 那么当 i = 1 时，取第一个值配置到预编译语言对象中
        int i = 1;
        // 遍历 todoMap ,转换给定的值的类型，把类型转换成可以插入数据库的格式，并存入预编译对象
        todoMap.each { key, value ->
            CommonUtil.setPstmtParams(value,pstmt,i);
            i ++;
        }
        // 将设置好的一组数据添加到 预编译对象中
        pstmt.addBatch();
    }
}
