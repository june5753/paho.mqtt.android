package paho.mqtt.java.example.utils;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间工具类
 *
 * @author juneyang
 */
public class TimeUtils {

    public static String formatTime(long time) {
        Date nowTime = new Date(time);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sFormat = new SimpleDateFormat("HH:mm:ss:sss");
        return sFormat.format(nowTime) + " ";
    }
}
