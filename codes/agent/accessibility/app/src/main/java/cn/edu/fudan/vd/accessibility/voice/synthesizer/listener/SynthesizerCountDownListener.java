package cn.edu.fudan.vd.accessibility.voice.synthesizer.listener;

import com.iflytek.cloud.SpeechError;

import java.util.concurrent.CountDownLatch;

import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class SynthesizerCountDownListener extends BaseSynthesizerListener {

    private CountDownLatch countDownLatch;

    public SynthesizerCountDownListener(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onCompleted(SpeechError error) {
        super.onCompleted(error);
        if (countDownLatch != null) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, "Decrease latch.");
            countDownLatch.countDown();
        }
    }
}
