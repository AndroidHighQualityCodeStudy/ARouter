package com.alibaba.android.arouter.utils;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.android.arouter.facade.template.ILogger;

/**
 * Default logger
 *
 * @author zhilong <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2015-12-08 21:44:10
 */
public class DefaultLogger implements ILogger {

    private static boolean isShowLog = false;
    private static boolean isShowStackTrace = false;
    private static boolean isMonitorMode = false;

    //
    private String defaultTag = "ARouter";

    /**
     * log
     *
     * @param showLog
     */
    public void showLog(boolean showLog) {
        isShowLog = showLog;
    }

    /**
     * @param showStackTrace
     */
    public void showStackTrace(boolean showStackTrace) {
        isShowStackTrace = showStackTrace;
    }

    public void showMonitor(boolean showMonitor) {
        isMonitorMode = showMonitor;
    }

    public DefaultLogger() {
    }

    public DefaultLogger(String defaultTag) {
        this.defaultTag = defaultTag;
    }

    @Override
    public void debug(String tag, String message) {
//        if (isShowLog) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            Log.d(TextUtils.isEmpty(tag) ? getDefaultTag() : tag, message + getExtInfo(stackTraceElement));
//        }
    }

    @Override
    public void info(String tag, String message) {
//        if (isShowLog) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            Log.i(TextUtils.isEmpty(tag) ? getDefaultTag() : tag, message + getExtInfo(stackTraceElement));
//        }
    }

    @Override
    public void warning(String tag, String message) {
//        if (isShowLog) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            Log.w(TextUtils.isEmpty(tag) ? getDefaultTag() : tag, message + getExtInfo(stackTraceElement));
//        }
    }

    @Override
    public void error(String tag, String message) {
//        if (isShowLog) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            Log.e(TextUtils.isEmpty(tag) ? getDefaultTag() : tag, message + getExtInfo(stackTraceElement));
//        }
    }

    @Override
    public void monitor(String message) {
//        if (isShowLog && isMonitorMode()) {
//            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
//            Log.d(defaultTag + "::monitor", message + getExtInfo(stackTraceElement));
//        }
    }

    @Override
    public boolean isMonitorMode() {
        return isMonitorMode;
    }

    /**
     * 默认tag
     *
     * @return
     */
    @Override
    public String getDefaultTag() {
        return defaultTag;
    }


    /**
     * @param stackTraceElement
     * @return
     */
    public static String getExtInfo(StackTraceElement stackTraceElement) {

        String separator = " & ";
        StringBuilder sb = new StringBuilder("[");
        // 打印堆栈信息
        if (isShowStackTrace) {
            String threadName = Thread.currentThread().getName();
            String fileName = stackTraceElement.getFileName();
            String className = stackTraceElement.getClassName();
            String methodName = stackTraceElement.getMethodName();
            long threadID = Thread.currentThread().getId();
            int lineNumber = stackTraceElement.getLineNumber();
            // 线程id
            sb.append("ThreadId=").append(threadID).append(separator);
            // 线程名称
            sb.append("ThreadName=").append(threadName).append(separator);
            // 文件名
            sb.append("FileName=").append(fileName).append(separator);
            // class 名
            sb.append("ClassName=").append(className).append(separator);
            // 方法名
            sb.append("MethodName=").append(methodName).append(separator);
            // 行号
            sb.append("LineNumber=").append(lineNumber);
        }

        sb.append(" ] ");
        return sb.toString();
    }
}