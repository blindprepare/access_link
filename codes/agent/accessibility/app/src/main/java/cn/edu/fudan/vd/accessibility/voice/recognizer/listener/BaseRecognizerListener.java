package cn.edu.fudan.vd.accessibility.voice.recognizer.listener;

import android.os.Bundle;
import android.util.Log;

import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class BaseRecognizerListener implements RecognizerListener {

    private HashMap<String, String> resultMap;
    protected List<String> textList;

    public BaseRecognizerListener() {
        resultMap = new HashMap<>();
        textList = new ArrayList<>();
    }


    @Override
    public void onVolumeChanged(int i, byte[] bytes) {
    }

    @Override
    public void onBeginOfSpeech() {
        // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
        DebugLogger.log(DebugLogger.Level.INFORMATION, "开始说话");
    }

    @Override
    public void onEndOfSpeech() {
        // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
        DebugLogger.log(DebugLogger.Level.INFORMATION, "结束说话");
    }

    @Override
    public void onResult(RecognizerResult recognizerResult, boolean b) {
        if (null != recognizerResult) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, "recognizer result：" + recognizerResult.getResultString());
            String text = getRecognizedText(recognizerResult);
            if (!text.isEmpty()) {
                textList.add(text);
            }
        } else {
            DebugLogger.log(DebugLogger.Level.INFORMATION, "recognizer result : null");
        }
    }

    @Override
    public void onError(SpeechError speechError) {

    }

    @Override
    public void onEvent(int i, int i1, int i2, Bundle bundle) {

    }

    private String getRecognizedText(RecognizerResult results) {
        String text = parseGrammarResult(results.getResultString());
        String sn = null;
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");// 第几句
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (sn != null) {
            resultMap.put(sn, text);
        }
        StringBuilder resultBuffer = new StringBuilder();
        for (String key : resultMap.keySet()) {
            resultBuffer.append(resultMap.get(key));
        }
        return resultBuffer.toString();
    }

    private String parseGrammarResult(String json) {
        StringBuilder ret = new StringBuilder();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);
            JSONArray words = joResult.getJSONArray("ws");//词
            for (int i = 0; i < words.length(); i++) {
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");//中文分词
                if (items.length() >= 1) {
                    JSONObject obj = items.getJSONObject(0);
                    if (obj.getString("w").contains("nomatch")) {
                        return ret.toString();
                    }
                    ret.append(obj.getString("w"));//单字
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }
}
