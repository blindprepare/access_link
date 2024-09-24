package cn.edu.fudan.vd.accessibility.voice.synthesizer;

import android.content.Context;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import java.util.List;

import cn.edu.fudan.vd.accessibility.action.SpeakAction;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.SynthesizerInitListener;

public class VoiceSynthesizer {

    private static VoiceSynthesizer voiceSynthesizer;

    //语音合成对象
    private SpeechSynthesizer synthesizer;

    public static void initVoiceSynthesizer(Context context) {
        voiceSynthesizer = new VoiceSynthesizer(context);
    }

    public static VoiceSynthesizer getInstance() {
        return voiceSynthesizer;
    }


    private VoiceSynthesizer(Context context) {
        synthesizer = SpeechSynthesizer.createSynthesizer(context, new SynthesizerInitListener());
        initSynthesizer();
        DebugLogger.log(DebugLogger.Level.INFORMATION, "finish init voice synthesizer.");
    }

    private void initSynthesizer() {
        // 清空参数
        synthesizer.setParameter(SpeechConstant.PARAMS, null);
        synthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置在线合成发音人
        synthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        //设置合成语速
        synthesizer.setParameter(SpeechConstant.SPEED, "50");
        //设置合成音调
        synthesizer.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        synthesizer.setParameter(SpeechConstant.VOLUME, "50");
        // 设置播放器音频流类型
        synthesizer.setParameter(SpeechConstant.STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放（默认为true）
        synthesizer.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    public void startSpeaking(List<String> textList, SynthesizerListener listener) {
        Thread thread = new Thread(new SpeakAction(textList, listener));
        thread.start();
    }

    public SpeechSynthesizer getSynthesizer() {
        return synthesizer;
    }
}
