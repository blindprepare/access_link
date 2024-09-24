package cn.edu.fudan.vd.accessibility.voice.recognizer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechRecognizer;

import cn.edu.fudan.vd.accessibility.logger.DebugLogger;
import cn.edu.fudan.vd.accessibility.voice.recognizer.listener.RecognizerInitListener;

public class VoiceRecognizer {
    private static VoiceRecognizer voiceRecognizer;
    private SpeechRecognizer recognizer;
    public static final int RECORD_MODE = 1, REPLAY_MODE = 2;
    private int mode;

    public static void initVoiceRecognizer(Context context) {
        voiceRecognizer = new VoiceRecognizer(context);
    }

    public static VoiceRecognizer getInstance() {
        return voiceRecognizer;
    }


    private VoiceRecognizer(Context context) {
        recognizer = SpeechRecognizer.createRecognizer(context, new RecognizerInitListener());
        initRecognizer();
    }

    private void initRecognizer() {
        // 清空参数
        recognizer.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        recognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式
        recognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        recognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置标点符号（设置为"0"则返回结果无标点，设置为"1"返回结果有标点）
        recognizer.setParameter(SpeechConstant.ASR_PTT, "1");
        // 设置语音前端点和后端点
        setMode(RECORD_MODE);
    }

    public void setMode(int mode) {
        this.mode = mode;
        switchMode();
    }

    private void switchMode() {
        int bosValue = 0; // 语音前端点（静音超时时间，即用户多长时间不说话则当做超时处理）
        int eosValue = 0; // 语音后端点（后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音）
        if (mode == RECORD_MODE) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, "Switch to record mode");
            bosValue = 2000;
            eosValue = 2000;
        } else if (mode == REPLAY_MODE) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, "Switch to replay mode");
            bosValue = 2000;
            eosValue = 2000;
        }
        recognizer.setParameter(SpeechConstant.VAD_BOS, String.valueOf(bosValue));
        recognizer.setParameter(SpeechConstant.VAD_EOS, String.valueOf(eosValue));
    }

    @SuppressLint("DefaultLocale")
    public void startListening(RecognizerListener listener) {
        int result = recognizer.startListening(listener);
        if (result != ErrorCode.SUCCESS)
            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Fail to recognize speech [error code = %d].", result));
    }

    public SpeechRecognizer getRecognizer() {
        return recognizer;
    }
}
