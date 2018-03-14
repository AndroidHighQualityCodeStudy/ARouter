package com.alibaba.android.arouter.utils;


import android.util.Log;

/**
 * @author xiaxl <a href="mailto:xiaxveliang@163.com">Contact me.</a>
 */

public class LogUtils {

    private static final String DEFAULT_TAG = "xiaxl";


    /**
     *
     */
    // 日志是否显示
    private static boolean isShowLog = true;
    // 是否显示堆栈信息
    private static boolean isShowStackTrace = false;

    public static void showLog(boolean showLog) {
        isShowLog = showLog;
    }

    public static void showStackTrace(boolean showStackTrace) {
        isShowStackTrace = showStackTrace;
    }

    // #################################################


    public static void i(String tag, String s) {
        if (isShowLog) {
            Log.i(getTAG(tag), getMsg(s));
        }
    }

    public static void e(String tag, String s) {
        if (isShowLog) {
            Log.e(getTAG(tag), getMsg(s));
        }
    }

    public static void e(String tag, String s, Throwable tr) {
        if (isShowLog) {
            Log.e(getTAG(tag), getMsg(s), tr);
        }
    }

    public static void d(String tag, String s) {
        if (isShowLog) {
            Log.d(getTAG(tag), getMsg(s));
        }
    }

    public static void w(String tag, String s) {
        if (isShowLog) {
            Log.w(getTAG(tag), getMsg(s));
        }
    }

    public static void w(String tag, String s, Throwable tr) {
        if (isShowLog) {
            Log.w(getTAG(tag), getMsg(s), tr);
        }
    }

    public static void v(String tag, String s) {
        if (isShowLog) {
            Log.v(getTAG(tag), getMsg(s));
        }
    }

    public static void v(String tag, String s, Throwable tr) {
        if (isShowLog) {
            Log.v(getTAG(tag), getMsg(s), tr);
        }
    }

    // #################################################

    /**
     * tag
     *
     * @param tag
     * @return
     */
    private static String getTAG(String tag) {
        // -------------------------
        StringBuffer sb = new StringBuffer();
        sb.append(DEFAULT_TAG);
        sb.append(" ");
        sb.append(tag);
        return sb.toString();
    }


    /**
     * @param msg
     * @return
     */
    private static String getMsg(String msg) {
        return msg + getTraceInfo(Thread.currentThread().getStackTrace()[4]);
    }


    // #################################################

    /**
     * @param stackTraceElement
     * @return
     */
    private static String getTraceInfo(StackTraceElement stackTraceElement) {

        String separator = " & ";

        // 打印堆栈信息
        if (isShowStackTrace) {
            StringBuilder sb = new StringBuilder("[");
            //
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
            //
            sb.append(" ] ");
            return sb.toString();
        }
        return "";
    }
}
