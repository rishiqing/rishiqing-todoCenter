package com.rishiqing.ds

import com.rishiqing.Clock
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
        Timestamp timestampNow = new Timestamp(new Date().getTime())
        Timestamp timestampToday = new Timestamp(new Date().clearTime().getTime())
        map.version           =   0
        map.dateCreated       =   timestampNow
        map.lastUpdated       =   timestampNow
        map.pContainer       =   todo.pContainer
        map.pDisplayOrder    =   todo.pDisplayOrder
        map.pFinishedTime    =   null
        map.pIsDone           =   0
        map.pNote             =   pNote
        map.pParentId         =   todo.pParentId
        map.pPlanedTime       =   timestampToday
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
        map.endDate            =   timestampToday
        map.startDate          =   timestampToday
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

    /**
     * 把要插入的数据导入到预编译对象中
     * @param todo
     * @param pstmt
     */
    static  void prepareInsert(Todo todo, PreparedStatement pstmt) {
        // 把 todo转换为 map
        def todoMap = toSqlMap(todo)
        // 查询 todo是否有时间和提醒
        // 设定阀值，用来指定插入位置，比如 todo的第一个参数为 version 那么当 i = 1 时，取第一个值配置到预编译语言对象中
        int i = 1
        // 遍历 todoMap ,转换给定的值的类型，把类型转换成可以插入数据库的格式，并存入预编译对象
        todoMap.each { key, value ->
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
                // 设置第 i 个位置的值为 String 格式
                pstmt.setString(i, value) // 转 字符串
            }
            i ++
        }
        // 将设置好的一组数据添加到 预编译对象中
        pstmt.addBatch()
    }

    /**
     * 把要更新的提醒数据导入到预编译对象中
     * @param todo
     * @param clock
     * @param pstmt
     * @param type : update / insert
     */
    static  void prepareInsert(Todo todo,Clock clock, PreparedStatement pstmt,String type) {
        // 更新
        if("update".equals(type)){
            pstmt.setLong(1,todo.id);
            pstmt.setLong(2,clock.id);
        }// 插入
        else if("insert".equals(type)){

        }
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
