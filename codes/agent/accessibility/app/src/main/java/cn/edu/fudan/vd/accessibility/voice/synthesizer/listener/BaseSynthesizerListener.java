package cn.edu.fudan.vd.accessibility.voice.synthesizer.listener;

import android.os.Bundle;
import android.util.Log;

import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SynthesizerListener;

import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class BaseSynthesizerListener implements SynthesizerListener {
    private static String TAG = BaseSynthesizerListener.class.getSimpleName();

    @Override
    public void onSpeakBegin() {
        DebugLogger.log(DebugLogger.Level.INFORMATION, "speak begin.");
    }

    @Override
    public void onBufferProgress(int i, int i1, int i2, String s) {

    }

    @Override
    public void onSpeakPaused() {
        DebugLogger.log(DebugLogger.Level.INFORMATION, "speak paused.");
    }

    @Override
    public void onSpeakResumed() {
        DebugLogger.log(DebugLogger.Level.INFORMATION, "speak resumed.");
    }

    @Override
    public void onSpeakProgress(int i, int i1, int i2) {

    }

    @Override
    public void onCompleted(SpeechError speechError) {
        if (speechError == null)
            DebugLogger.log(DebugLogger.Level.INFORMATION, "Speak completed.");
        else
            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Speak error : %s.", speechError.getPlainDescription(true)));
    }

    @Override
    public void onEvent(int i, int i1, int i2, Bundle bundle) {

    }
}
