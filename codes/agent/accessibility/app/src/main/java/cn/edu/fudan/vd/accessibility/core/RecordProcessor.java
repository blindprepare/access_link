package cn.edu.fudan.vd.accessibility.core;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Pair;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cn.edu.fudan.vd.accessibility.entity.AppInfo;
import cn.edu.fudan.vd.accessibility.entity.Operation;
import cn.edu.fudan.vd.accessibility.entity.PageInfo;
import cn.edu.fudan.vd.accessibility.enumeration.OperationType;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;
import cn.edu.fudan.vd.accessibility.voice.recognizer.VoiceRecognizer;
import cn.edu.fudan.vd.accessibility.voice.recognizer.listener.RecognizerCallbackListener;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.VoiceSynthesizer;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.SynthesizerCallbackListener;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static cn.edu.fudan.vd.accessibility.enumeration.Status.FREE;
import static cn.edu.fudan.vd.accessibility.enumeration.Status.RECORD;
import static cn.edu.fudan.vd.accessibility.logger.DebugLogger.Level.ERROR;
import static cn.edu.fudan.vd.accessibility.logger.DebugLogger.Level.INFORMATION;


public class RecordProcessor {

    private static String targetApp = "";

    private static AppInfo appInfo;


    private static volatile String currentActivity = "";
    private static volatile String currentActivityName = "";
    private static volatile AccessibilityNodeInfo currentRoot = null;

    private static List<Operation> operationList = new ArrayList<>();

    private static volatile int lastEventType = -1;
    private static volatile Boolean useHint = null;

    private static boolean triggerBack = false;
    private static boolean needExecute = false;

    public static void startRecord(AppInfo app) {
        RecordReplayAccessibility.setStatus(RECORD);
        targetApp = app.getPackageName();
        currentActivity = app.getActivityName();
        appInfo = app;
        lastEventType = -1;
        useHint = null;
    }

    public static String getTargetApp() {
        return targetApp;
    }

    public static void stopRecord() {
        RecordReplayAccessibility.setStatus(FREE);
        generateRecordResult();
        targetApp = "";
        currentActivity = "";
        currentActivityName = "";
        currentRoot = null;
        lastEventType = -1;
        useHint = null;
        operationList.clear();
    }

    public static void recordHumanAction() {
        List<String> textList = new ArrayList<>();
        textList.add(Constant.HUMAN_ACTION_DESC);
        VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
            if (param1.size() == 1) {
                Operation operation = new Operation("NO_NEED", currentActivity, OperationType.HUMAN, "");
                operation.setPageName(currentActivityName);
                operation.setNextPageName(currentActivityName);
                operation.setExplain(param1.get(0));
                operationList.add(operation);
            }
        }))));
    }

    private static synchronized void generateRecordResult() {
        Map<String, PageInfo> pageInfoMap = new HashMap<>();
        for (Operation operation : operationList) {
            PageInfo pageInfo = pageInfoMap.get(operation.getPageName());
            if (pageInfo == null) {
                pageInfo = new PageInfo();
                pageInfo.setActionList(new ArrayList<>());
                pageInfo.setPageName(operation.getPageName());
                pageInfo.setPage(operation.getPage());
                pageInfo.setPageName(appInfo.getPackageName());
            }
            pageInfo.getActionList().add(operation);
        }
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + "/+" + appInfo.getAppName() + "/pages/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            for (String key : pageInfoMap.keySet()) {
                PageInfo pageInfo = pageInfoMap.get(key);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("pageName", pageInfo.getPageName());
                jsonObject.put("page", pageInfo.getPage());
                jsonObject.put("package", pageInfo.getPackageName());
                JSONArray array = new JSONArray();
                for (Operation operation : pageInfo.getActionList()) {
                    JSONObject action = new JSONObject();
                    action.put("action", operation.getExplain());
                    action.put("type", operation.getType().explain);
                    if (operation.getType() == OperationType.TYPE) {
                        if (operation.getRemark().startsWith(Constant.HINT_INPUT_PREFIX)) {
                            action.put("parameter", operation.getRemark().substring(Constant.HINT_INPUT_PREFIX.length()));
                        } else {
                            action.put("parameter", operation.getRemark());
                        }
                    } else {
                        action.put("parameter", "无需参数");
                    }
                    action.put("nextPage", operation.getNextPageName());
                    array.add(action);
                }
                jsonObject.put("actionList", array);
                File file = new File(dir, key);
                FileWriter writer;
                writer = new FileWriter(file);
                writer.write(jsonObject.toJSONString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("SwitchIntDef")
    public static synchronized void processEvent(AccessibilityEvent event, AccessibilityNodeInfo root) {
        if (RecordReplayAccessibility.getStatus() == RECORD && event.getPackageName() != null && event.getPackageName().toString().equals(targetApp)) {
            DebugLogger.log(INFORMATION, String.format("Find the event is %s", event));
            switch (event.getEventType()) {
                case TYPE_WINDOW_STATE_CHANGED:
                    processActivityChangedEvent(event, root);
                    break;
                case TYPE_VIEW_CLICKED:
                    processClickEvent(event, currentRoot);
                    break;
                case TYPE_VIEW_TEXT_CHANGED:
                    processInputEvent(event, currentRoot);
                    break;
                default:
                    break;
            }
        }
    }

    private static synchronized void processActivityChangedEvent(AccessibilityEvent event, AccessibilityNodeInfo root) {
        lastEventType = event.getEventType();
        String name = event.getClassName().toString();
        try {
            currentRoot = root;
//            ArrayList<AccessibilityNodeInfo> nodeInfoArrayList = new ArrayList<>();
//            nodeInfoArrayList.add(root);
//            printRoot(nodeInfoArrayList);
        } catch (Exception e) {
            DebugLogger.log(ERROR, String.format("Error is %s", e.getLocalizedMessage()));
        }
        if (name.contains("Activity") || name.startsWith(targetApp) || name.startsWith("com.android.")) {
            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Last activity is %s", currentActivity));
            currentActivity = name;
            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Current activity is %s", currentActivity));
            currentActivityName = checkOrSetLabel(String.valueOf(currentRoot.getContentDescription()), Constant.RECORD_ACTIVITY_NAME_ASK, Constant.ACTIVITY_NAME_ASK);
            if (operationList.size() > 0 && operationList.get(operationList.size() - 1).getNextPageName() == null) {
                operationList.get(operationList.size() - 1).setNextPageName(currentActivityName);
            }
        }
    }

    private static synchronized String checkOrSetLabel(String temp, String hintSet, String hintCheck) {
        AtomicReference<String> result = new AtomicReference<>("");
        if (temp == null || temp.equals("null") || temp.isEmpty()) {
            List<String> textList = new ArrayList<>();
            textList.add(hintSet);
            VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
                if (param1.size() == 1) {
                    result.set(param1.get(0));
                }
            }))));
        } else {
            List<String> textList = new ArrayList<>();
            textList.add(String.format(hintCheck, temp));
            VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
                if (param1.size() == 1 && param1.get(0).equals(Constant.YES)) {
                    result.set(temp);
                } else {
                    result.set(param1.get(0));
                }
            }))));
        }
        return result.get();
    }

    private static synchronized void printRoot(ArrayList<AccessibilityNodeInfo> nodeInfoArrayList) {
        System.out.println("---------------------------------------------------------");
        ArrayList<AccessibilityNodeInfo> children = new ArrayList<>();
        for (AccessibilityNodeInfo root : nodeInfoArrayList) {
            System.out.println(JSON.toJSONString(root));
            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo nodeInfo = root.getChild(i);
                if (nodeInfo == null) {
                    continue;
                }
                children.add(nodeInfo);
            }
        }
        if (children.isEmpty()) {
            return;
        }
        printRoot(children);
    }


    private static synchronized void processInputEvent(AccessibilityEvent event, AccessibilityNodeInfo root) {
        if (lastEventType == event.getEventType() && useHint) {
            return;
        }
        AccessibilityNodeInfoWrapper accessibilityNodeInfoWrapper = AccessibilityNodeInfoWrapper.getInstance(event, root);
        if (accessibilityNodeInfoWrapper.flag == WrapperFlag.NORMAL) {
            recordInputOperation(accessibilityNodeInfoWrapper);
        }
    }

    private static synchronized void processClickEvent(AccessibilityEvent event, AccessibilityNodeInfo root) {
        if (triggerBack) {
            triggerBack = false;
            return;
        }
        AccessibilityNodeInfoWrapper accessibilityNodeInfoWrapper = AccessibilityNodeInfoWrapper.getInstance(event, root);
        switch (accessibilityNodeInfoWrapper.flag) {
            case SOURCE_NOT_FOUNT:
                try {
                    Thread.sleep(1000);
                    RecordReplayAccessibility.getRecordReplayAccessibility().goBack();
                    Thread.sleep(1000);
                    needExecute = true;
                    processClickEvent(event, root);
                    triggerBack = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case NORMAL:
                recordClickOperation(accessibilityNodeInfoWrapper);
                break;
            default:
                break;
        }
    }

    private static synchronized void recordInputOperation(AccessibilityNodeInfoWrapper accessibilityNodeInfoWrapper) {
        if (lastEventType != accessibilityNodeInfoWrapper.event.getEventType()) {
            List<String> textList = new ArrayList<>();
            textList.add(Constant.HINT_CHECK_ASK);
            VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
                if (param1.size() == 1 && Constant.YES.equals(param1.get(0))) {
                    useHint = true;
                    recordHintInputOperation(accessibilityNodeInfoWrapper);
                } else {
                    useHint = false;
                    recordConstantInputOperation(accessibilityNodeInfoWrapper);
                }
            }))));
        } else if (!useHint) {
            recordConstantInputOperation(accessibilityNodeInfoWrapper);
        }
    }

    private static synchronized void recordHintInputOperation(AccessibilityNodeInfoWrapper accessibilityNodeInfoWrapper) {
        String viewPosition = accessibilityNodeInfoWrapper.viewSequence;
        final String[] remark = new String[1];
        String recordActivity = currentActivity;
        List<String> textList = new ArrayList<>();
        textList.add(Constant.INPUT_HINT_ASK);
        VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
            if (param1.size() == 1) {
                remark[0] = Constant.HINT_INPUT_PREFIX + param1.get(0);
                DebugLogger.log(INFORMATION, String.format("Input hint is %s.", remark[0]));
                Operation operation = new Operation(viewPosition, recordActivity, OperationType.TYPE, remark[0]);
                operation.setPageName(currentActivityName);
                operation.setExplain(checkOrSetLabel(String.valueOf(accessibilityNodeInfoWrapper.nodeInfo.getContentDescription()), Constant.RECORD_NODE_LABEL_ASK, Constant.NODE_LABEL_ASK));
                operationList.add(operation);
            }
        }))));
        lastEventType = accessibilityNodeInfoWrapper.event.getEventType();
    }

    private static synchronized void recordConstantInputOperation(AccessibilityNodeInfoWrapper accessibilityNodeInfoWrapper) {
        String remark = Constant.CONSTANT_INPUT_PREFIX + accessibilityNodeInfoWrapper.nodeInfo.getText();
        if (lastEventType != accessibilityNodeInfoWrapper.event.getEventType()) {
            Operation operation = new Operation(accessibilityNodeInfoWrapper.viewSequence, currentActivity, OperationType.TYPE, remark);
            operation.setPageName(currentActivityName);
            operation.setExplain(checkOrSetLabel(String.valueOf(accessibilityNodeInfoWrapper.nodeInfo.getContentDescription()), Constant.RECORD_NODE_LABEL_ASK, Constant.NODE_LABEL_ASK));
            operationList.add(operation);
        } else {
            operationList.get(operationList.size() - 1).setRemark(remark);
        }
    }

    private static synchronized void recordClickOperation(AccessibilityNodeInfoWrapper accessibilityNodeInfoWrapper) {
        String remark = listContainerPosition(accessibilityNodeInfoWrapper);
        String viewPosition = accessibilityNodeInfoWrapper.viewSequence;
        if (remark == null) {
            remark = "";
            Operation operation = new Operation(viewPosition, currentActivity, OperationType.CLICK, remark);
            operation.setPageName(currentActivityName);
            operation.setExplain(checkOrSetLabel(String.valueOf(accessibilityNodeInfoWrapper.nodeInfo.getContentDescription()), Constant.RECORD_NODE_LABEL_ASK, Constant.NODE_LABEL_ASK));
            operationList.add(operation);
            if (needExecute) {
                needExecute = false;
                accessibilityNodeInfoWrapper.nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        } else {
            String recordActivity = currentActivity;
            List<String> textList = new ArrayList<>();
            textList.add(Constant.CLICK_LIST_ITEM_ASK);
            String finalRemark = remark;
            VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
                Operation operation = new Operation(viewPosition, recordActivity, OperationType.CLICK, "");
                if (param1.size() == 1 && Constant.YES.equals(param1.get(0))) {
                    operation = new Operation(finalRemark, recordActivity, OperationType.SELECT, "");
                }
                operation.setPageName(currentActivityName);
                operation.setExplain(checkOrSetLabel(String.valueOf(accessibilityNodeInfoWrapper.nodeInfo.getContentDescription()), Constant.RECORD_NODE_LABEL_ASK, Constant.NODE_LABEL_ASK));
                operationList.add(operation);
                if (needExecute) {
                    needExecute = false;
                    accessibilityNodeInfoWrapper.nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }))));
        }
        lastEventType = accessibilityNodeInfoWrapper.event.getEventType();
    }

    private static String listContainerPosition(AccessibilityNodeInfoWrapper accessibilityNodeInfoWrapper) {
        String viewPosition = accessibilityNodeInfoWrapper.viewSequence;
        String[] positions = viewPosition.split(",");
        AccessibilityNodeInfo parent = accessibilityNodeInfoWrapper.nodeInfo;
        int index = positions.length - 1;
        for (int i = positions.length - 1; i >= 0; i--) {
            if (parent.getCollectionInfo() == null || parent.getChildCount() == 1) {
                parent = parent.getParent();
            } else {
                index = i;
                break;
            }
        }
        if (index == positions.length - 1) {
            return null;
        } else {
            StringBuilder newPosition = new StringBuilder();
            for (int i = 0; i <= index; i++) {
                newPosition.append(positions[i]).append(",");
            }
            return newPosition.substring(0, newPosition.length() - 1);
        }
    }

    private static Pair<List<Integer>, List<String>> getTargetNodeSequences(AccessibilityNodeInfo root, AccessibilityNodeInfo target) {
        if (target == null) {
            DebugLogger.log(INFORMATION, "target is null.");
            return new Pair<>(new ArrayList<>(), new ArrayList<>());
        }
        List<Integer> sequences = new ArrayList<>();
        List<String> path = new ArrayList<>();
        while (!target.equals(root)) {
            AccessibilityNodeInfo parent = target.getParent();
            if (parent == null) {
                DebugLogger.log(INFORMATION, "Parent is null");
                break;
            }
            int childCount = parent.getChildCount();
            int index = -1;
            for (int i = 0; i < childCount; i++) {
                if (parent.getChild(i) == null)
                    continue;
                AccessibilityNodeInfo child = parent.getChild(i);
                if (child.equals(target)) {
                    index = i;
                }
            }
            if (index == -1) {
                DebugLogger.log(INFORMATION, "Target node not found.");
                return new Pair<>(new ArrayList<>(), new ArrayList<>());
            }
            sequences.add(0, index);
            path.add(0, target.getClassName().toString());
            target = parent;
        }
        DebugLogger.log(INFORMATION, String.format("Sequences is %s", sequences.toString().replace(" ", "")));
        DebugLogger.log(INFORMATION, String.format("package is %s", appInfo.getPackageName()));
        DebugLogger.log(INFORMATION, String.format("activity is %s", currentActivity));
        return new Pair<>(sequences, path);
    }


    private static class AccessibilityNodeInfoWrapper {
        AccessibilityEvent event;

        AccessibilityNodeInfo nodeInfo;

        String viewSequence;

        String path;

        WrapperFlag flag;

        static AccessibilityNodeInfoWrapper getInstance(AccessibilityEvent event, AccessibilityNodeInfo root) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            AccessibilityNodeInfoWrapper result = new AccessibilityNodeInfoWrapper();
            result.event = AccessibilityEvent.obtain(event);
            if (nodeInfo == null) {
                result.flag = WrapperFlag.SOURCE_NOT_FOUNT;
                return result;
            }
            result.nodeInfo = nodeInfo;
            Pair<List<Integer>, List<String>> pair = getTargetNodeSequences(root, nodeInfo);
            List<Integer> sequences = pair.first;
            List<String> path = pair.second;
            if (sequences.isEmpty()) {
                result.flag = WrapperFlag.SEQUENCE_NOT_FOUND;
                result.viewSequence = "";
                return result;
            }
            result.flag = WrapperFlag.NORMAL;
            StringBuilder sequenceString = new StringBuilder();
            StringBuilder pathString = new StringBuilder();
            int size = sequences.size();
            for (int i = 0; i < size - 1; i++) {
                Integer index = sequences.get(i);
                sequenceString.append(index).append(",");
                pathString.append(path.get(index)).append(",");
            }
            sequenceString.append(sequences.get(size - 1));
            result.viewSequence = sequenceString.toString();
            result.path = pathString.toString();
            return result;
        }

    }

    private enum WrapperFlag {
        SOURCE_NOT_FOUNT, NORMAL, SEQUENCE_NOT_FOUND;
    }
}
