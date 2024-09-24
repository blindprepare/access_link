package cn.edu.fudan.vd.accessibility;

import android.app.Application;
import android.content.Context;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

import cn.edu.fudan.vd.accessibility.voice.recognizer.VoiceRecognizer;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.VoiceSynthesizer;

public class RecordReplayApplication extends Application {


    @Override
    public void onCreate() {
        // 应用程序入口处调用,避免手机内存过小,杀死后台进程后通过历史intent进入Activity造成SpeechUtility对象为null
        // 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true"
        // 参数间使用“,”分隔。
        // 设置你申请的应用appid

        // 注意： appid 必须和下载的SDK保持一致，否则会出现10407错误

        StringBuilder param = new StringBuilder();
        param.append(SpeechConstant.APPID).append("=").append(getString(R.string.app_id));
        param.append(",");
        param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC); // 设置使用v5+
        SpeechUtility.createUtility(RecordReplayApplication.this, param.toString());

        initModule();


        try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        super.onCreate();
    }

    private void initModule() {
        Context context = getApplicationContext();
        VoiceRecognizer.initVoiceRecognizer(context);
        VoiceSynthesizer.initVoiceSynthesizer(context);
    }

}
