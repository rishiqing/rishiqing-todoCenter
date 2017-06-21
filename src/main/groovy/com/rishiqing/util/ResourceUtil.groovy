package com.rishiqing.util

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Created by Thinkpad on 2017/6/2.
 * 数据库资源工具
 */
class ResourceUtil {
    /**
     * 资源释放
     */
    public static void resourceClose(Connection conn,PreparedStatement pstmt,ResultSet rs){
        if(conn){
            conn.close();
        }
        if(pstmt){
            pstmt.close();
        }
        if(rs){
            rs.close();
        }
    }
}
