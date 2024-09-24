package cn.edu.fudan.vd.accessibility.voice.synthesizer.listener;

import android.annotation.SuppressLint;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;

import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class SynthesizerInitListener implements InitListener {

    @SuppressLint("DefaultLocale")
    @Override
    public void onInit(int code) {
        if (code != ErrorCode.SUCCESS) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Fail to init [error code = %d].", code));
        }
    }
}
