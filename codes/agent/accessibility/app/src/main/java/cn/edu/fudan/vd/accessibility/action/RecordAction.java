package cn.edu.fudan.vd.accessibility.action;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import cn.edu.fudan.vd.accessibility.core.RecordProcessor;
import cn.edu.fudan.vd.accessibility.core.RecordReplayAccessibility;
import cn.edu.fudan.vd.accessibility.entity.AppInfo;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;
import cn.edu.fudan.vd.accessibility.util.AppManager;
import cn.edu.fudan.vd.accessibility.core.Constant;
import cn.edu.fudan.vd.accessibility.voice.recognizer.VoiceRecognizer;
import cn.edu.fudan.vd.accessibility.voice.recognizer.listener.RecognizerCallbackListener;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.VoiceSynthesizer;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.SynthesizerCallbackListener;

public class RecordAction {
    private Context context;

    public RecordAction(Context context) {
        this.context = context;
    }

    public void record(final AppInfo appInfo) {
        if (RecordReplayAccessibility.isStart()) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, "Perform record action.");
            if ("".equals(RecordProcessor.getTargetApp())) {
                RecordProcessor.startRecord(appInfo);
                AppManager.launchActivity(context, appInfo);
                AppManager.startRecordStopFloat(context);
                AppManager.startSetHumanActionFloat(context);
            }
        } else {
            AppManager.launchSettingAccessibility(context);
        }

    }
}
