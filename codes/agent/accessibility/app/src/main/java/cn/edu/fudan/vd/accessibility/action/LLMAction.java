package cn.edu.fudan.vd.accessibility.action;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cn.edu.fudan.vd.accessibility.core.Constant;
import cn.edu.fudan.vd.accessibility.core.RecordReplayAccessibility;
import cn.edu.fudan.vd.accessibility.core.ReplayProcessor;
import cn.edu.fudan.vd.accessibility.entity.LLMInstruction;
import cn.edu.fudan.vd.accessibility.entity.AppInfo;
import cn.edu.fudan.vd.accessibility.entity.Operation;
import cn.edu.fudan.vd.accessibility.enumeration.OperationType;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;
import cn.edu.fudan.vd.accessibility.util.AppManager;
import cn.edu.fudan.vd.accessibility.voice.recognizer.VoiceRecognizer;
import cn.edu.fudan.vd.accessibility.voice.recognizer.listener.RecognizerCallbackListener;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.VoiceSynthesizer;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.BaseSynthesizerListener;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.SynthesizerCallbackListener;

import static cn.edu.fudan.vd.accessibility.core.Constant.CONSTANT_INPUT_PREFIX;
import static cn.edu.fudan.vd.accessibility.core.Constant.HINT_INPUT_PREFIX;

public class LLMAction {

    private Context context;

    public LLMAction(Context context) {
        this.context = context;
    }

    public void process(final AppInfo appInfo) {
        if (RecordReplayAccessibility.isStart()) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, "Get User Query");
            List<String> textList = new ArrayList<>();
            textList.add(String.format(Constant.LLM_QUERY_ASK, appInfo.getAppName()));
            VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
                if (param1.size() == 1) {
                    String userQuery = param1.get(0);
                    DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("User Query is %s.", userQuery));
                    List<String> queryText = new ArrayList<>();
                    queryText.add(Constant.LLM_QUERY_WAIT);
                    VoiceSynthesizer.getInstance().startSpeaking(queryText, new BaseSynthesizerListener());
                    this.send(userQuery, context, appInfo);
                }
            }))));
        } else {
            AppManager.launchSettingAccessibility(context);
        }

    }

    private void send(String query, Context context, final AppInfo appInfo) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(String.format("http://***.***.***.***:8002/ask?query=%s&appName=%s", query, appInfo.getAppName()));
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(600000);
                connection.setReadTimeout(600000);
                InputStream in = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                JSONArray instructions = new JSONArray(result.toString());
                DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Query Result is %s.", result));
                List<LLMInstruction> llmInstructions = new ArrayList<>();
                for (int i = 0; i < instructions.length(); i++) {
                    JSONObject instruction = instructions.getJSONObject(i);
                    LLMInstruction llmInstruction = new LLMInstruction(instruction.getString("path"), instruction.getString("page"),
                            instruction.getString("type"), instruction.getString("remark"), instruction.getString("instruction"), instruction.getString("package"));
                    llmInstructions.add(llmInstruction);
                }
                String packageName = llmInstructions.get(0).packageName;
                String activity = llmInstructions.get(0).activity;
                List<Operation> operationList = getOperationList(llmInstructions);
                ReplayProcessor.getInstance().replay(operationList, packageName, activity);
                AppManager.launchActivity(context, appInfo);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private List<Operation> getOperationList(List<LLMInstruction> llmInstructions) {
        List<Operation> operationList = new ArrayList<>();
        for (int i = 0; i < llmInstructions.size(); i++) {
            LLMInstruction llmInstruction = llmInstructions.get(i);
            String remark = "";
            String path = llmInstruction.path;
            OperationType operationType;
            switch (llmInstruction.type) {
                case "click":
                    operationType = OperationType.CLICK;
                    break;
                case "type":
                    operationType = OperationType.TYPE;
                    break;
                case "select":
                    operationType = OperationType.SELECT;
                    break;
                case "human":
                    operationType = OperationType.HUMAN;
                    break;
                default:
                    operationType = OperationType.UNKNOWN;
                    break;
            }
            if (operationType == OperationType.TYPE) {
                if (!llmInstruction.remark.startsWith(CONSTANT_INPUT_PREFIX)) {
                    remark = HINT_INPUT_PREFIX + llmInstruction.remark;
                } else {
                    remark = llmInstruction.remark;
                }
            }
            operationList.add(new Operation(path, llmInstruction.activity, operationType, remark));
        }
        return operationList;
    }
}
