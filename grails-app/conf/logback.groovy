import ch.qos.logback.core.util.FileSize
import grails.util.Environment

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('consoleAppender', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    }
}


//if (Environment.isDevelopmentMode()) {
    appender("rootDailyAppender", FileAppender) {
        file = "${System.properties['catalina.base']}${File.separator}logs${File.separator}todoCenter${File.separator}rsq_root_daily_rolling.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
        }
    }

//}
root(error, ['rootDailyAppender'])
