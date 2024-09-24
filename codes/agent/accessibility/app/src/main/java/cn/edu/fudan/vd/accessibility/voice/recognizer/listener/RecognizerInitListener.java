package cn.edu.fudan.vd.accessibility.voice.recognizer.listener;


import android.annotation.SuppressLint;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;

import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class RecognizerInitListener implements InitListener {

    @SuppressLint("DefaultLocale")
    @Override
    public void onInit(int code) {
        if (code != ErrorCode.SUCCESS) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Fail to init [error code = %d].", code));
        }
    }
}
