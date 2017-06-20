package com.rishiqing.util

/**
 * Created by Thinkpad on 2017/6/3.
 * 日期计算工具
 */
class DateUtil {

    /**
     * 将orgDate解析为Date或null
     * @param orgDate
     */
    static def parseDate(String orgDate){
        return parseDate(orgDate,"yyyy-MM-dd HH:mm:ss")
    }
    static def parseDate(String orgDate, String format){
        try{
            if(orgDate==null||"null" == orgDate||"" == orgDate){
                return null
            }else{
                return Date.parse(format,orgDate)
            }
        } catch(Exception e) {
            e.printStackTrace();
            return null
        }
    }

    /**
     * 获取时间戳 <br>
     *     <p> 时间戳:时间戳是指格林威治时间1970年01月01日00时00分00秒
     *     (北京时间1970年01月01日08时00分00秒)起至现在的总秒数 </p>
     */
    public static Long getTimestamp(Date date){
        return (date.time / 1000).intValue();
    }

    /**
     * 把分钟，小时，天转换成对应的秒数
     * @param num 数值
     * @param type 类型，转换的是天，小时，还是分钟
     */
    public static Long getSecond(Long num,String type){
        if("day"==type){
            return num * 86400;
        } else if("hour"==type){
            return num * 3600;
        } else if("min"==type){
            return num * 60
        } else {
            return -1;  // 转换出错，类型不对
        }
    }

    /**
     * 把时间戳转换为时间 <br>
     */
    public static Date getDate(Long timestamp){
        return new Date(timestamp * 1000);
    }

    /**
     * 键入 dates ，重新排序之后按指定格式返回
     */
    static String datesFormat(String dates,String format) {
        List datesList = [];
        // 拆分
        String[] datesArr = dates.split(",");
        // 遍历
        Map<Long, String> compare = [:];
        datesArr.each { d ->
            Date oneDay = parseDate(d, "yyyyMMdd");
            Long mill = oneDay.getTime();
            compare.put(mill, oneDay.format(format));
        }
        // 排序
        TreeMap sortMap = new TreeMap(compare);
        //处理请求map ,使用 entrySet 的方式遍历 Map
        sortMap.entrySet().each { es ->
            datesList.add(es.value);
        }
        // 重新返回
        return datesList.join(",");
    }
}
