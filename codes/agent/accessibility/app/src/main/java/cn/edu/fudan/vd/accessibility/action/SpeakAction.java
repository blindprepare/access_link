package cn.edu.fudan.vd.accessibility.action;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import cn.edu.fudan.vd.accessibility.logger.DebugLogger;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.VoiceSynthesizer;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.BaseSynthesizerListener;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.SynthesizerCallbackListener;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.SynthesizerCountDownListener;

public class SpeakAction implements Runnable {

    private List<String> textList;
    private SynthesizerListener listener;
    private String listenerType;
    private SpeechSynthesizer speechSynthesizer;

    public SpeakAction(List<String> textList, SynthesizerListener listener) {
        this.textList = textList;
        this.listener = listener;
        initListenerType(listener);
        speechSynthesizer = VoiceSynthesizer.getInstance().getSynthesizer();
    }

    private void initListenerType(SynthesizerListener listener) {
        if (listener instanceof SynthesizerCallbackListener) {
            this.listenerType = "callback";
        } else if (listener instanceof SynthesizerCountDownListener) {
            this.listenerType = "count down";
        } else if (listener instanceof BaseSynthesizerListener) {
            this.listenerType = "base";
        } else {
            this.listenerType = "custom";
        }
    }

    @Override
    public void run() {
        if (textList != null) {
            for (int i = 0; i < textList.size(); i++) {
                String text = textList.get(i);
                int result;
                DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Start to read [content = %s].", text));
                if (i != textList.size() - 1) {
                    DebugLogger.log(DebugLogger.Level.INFORMATION, "Invoke count down synthesizer listener.");
                    CountDownLatch latch = new CountDownLatch(1);
                    result = speechSynthesizer.startSpeaking(text, new SynthesizerCountDownListener(latch));
                    DebugLogger.log(DebugLogger.Level.INFORMATION, "Wait for reading.");
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Invoke %s synthesizer listener.", listenerType));
                    result = speechSynthesizer.startSpeaking(text, listener);
                }
                if (result != ErrorCode.SUCCESS)
                    DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Fail to speak [error code = %d].", result));
            }
        }
    }
}
