package cn.edu.fudan.vd.accessibility.voice.recognizer.listener;

import org.json.JSONException;

import java.util.List;

import cn.edu.fudan.vd.accessibility.action.ICallback;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class RecognizerCallbackListener extends BaseRecognizerListener {
    protected ICallback<List<String>> callback;

    public RecognizerCallbackListener(ICallback<List<String>> callback) {
        this.callback = callback;
    }

    @Override
    public void onEndOfSpeech() {
        super.onEndOfSpeech();
        DebugLogger.log(DebugLogger.Level.INFORMATION, "invoke callback.");
        try {
            callback.invoke(textList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
