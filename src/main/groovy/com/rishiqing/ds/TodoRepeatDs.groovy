package com.rishiqing.ds

import com.rishiqing.Todo

import java.sql.PreparedStatement
import java.sql.Timestamp

/**
 * Created by solax on 2017-1-10.
 */
class TodoRepeatDs {
    /**
     * 转换为 sql 需要的数据
     * @param
     */
    static  toSqlMap(Todo todo){
        String pTitle = todo.todoDeploy?todo.todoDeploy.pTitle:todo.pTitle
        String pNote = todo.todoDeploy?todo.todoDeploy.pNote:todo.pNote
        def map = [:]
        Timestamp timestamp = new Timestamp(new Date().clearTime().getTime())
        map.version           =   0
        map.dateCreated       =   timestamp
        map.lastUpdated       =   timestamp
        map.pContainer       =   todo.pContainer
        map.pDisplayOrder    =   0
        map.pFinishedTime    =   null
        map.pIsDone           =   0
        map.pNote             =   pNote
        map.pParentId         =   todo.pParentId
        map.pPlanedTime       =   timestamp
        map.pTitle            =   pTitle
        map.pUserId           =   todo.pUserId
        map.createdByClient    =   'web'
        map.receiverIds        =   null
        map.receiverNames      =   null
        map.senderId           =   null
        map.isDeleted          =   0
        map.cid                =   '-1'
        map.repeatTagId        =   todo.repeatTagId
        map.senderTodoId       =   null
        map.teamTodoRead       =   null
        map.clockAlert         =   todo.clockAlert
        map.kanbanItemId       =   todo.kanbanItemId
        map.isRevoke           =   0
        map.closingDateFinished =  null
        map.endDate            =   timestamp
        map.startDate          =   timestamp
        map.todoDeployId       =   null
        map.isFromSubTodo      =   0
        map.isChangeDate       =   0
        map.isRepeatTodo       =   false
        map.alertEveryDay      =   todo.alertEveryDay
        map.checkAuthority     =   todo.checkAuthority
        map.dates              =   todo.dates
        map.editAuthority      =   todo.editAuthority
        map.isArchived         =   todo.isArchived
        map.inboxPContainer    =   todo.inboxPContainer
        map.isSystem           =   true
        return map
    }

    static  void prepareInsert(Todo todo, PreparedStatement pstmt) {
        def todoMap = toSqlMap(todo)
        int i = 1
        todoMap.each { key, value ->
            println "key:${key},value:${value}"
            if (value instanceof  Timestamp) {
                pstmt.setTimestamp(i, value);
            } else if (value instanceof  Integer) {
                pstmt.setInt(i, value);
            } else if (value instanceof  Long) {
                pstmt.setLong(i, value);
            } else if (value instanceof  Boolean) {
                pstmt.setBoolean(i, value);
            }else {
                pstmt.setString(i, value)
            }
            i ++
        }
        pstmt.addBatch()
    }

    /**
     * map转string
     * @return
     */
    static  String toInsertString (Todo todo) {
        int i = 0
        StringBuffer sql = new StringBuffer()
        def todoMap = toSqlMap(todo)
        todoMap.each { key, value->
            sql .append(parse(value, i))
            i ++
        }
        return '(' +  sql + ')'
    }

    static  String parse (def object) {
        if  (!object || object == 'NULL' ) {
            return null
        } else {
            return object
        }
    }
}
