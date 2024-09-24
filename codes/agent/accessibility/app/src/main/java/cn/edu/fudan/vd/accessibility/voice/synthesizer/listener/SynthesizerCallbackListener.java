package cn.edu.fudan.vd.accessibility.voice.synthesizer.listener;

import com.iflytek.cloud.SpeechError;

import org.json.JSONException;

import cn.edu.fudan.vd.accessibility.action.ICallback;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class SynthesizerCallbackListener extends BaseSynthesizerListener {
    protected ICallback<Void> callback;

    public SynthesizerCallbackListener(ICallback<Void> callback) {
        this.callback = callback;
    }

    @Override
    public void onCompleted(SpeechError error) {
        super.onCompleted(error);
        DebugLogger.log(DebugLogger.Level.INFORMATION, "invoke callback.");
        try {
            callback.invoke(null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
