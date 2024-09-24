package cn.edu.fudan.vd.accessibility.core;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cn.edu.fudan.vd.accessibility.entity.Operation;
import cn.edu.fudan.vd.accessibility.enumeration.OperationType;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;
import cn.edu.fudan.vd.accessibility.voice.recognizer.VoiceRecognizer;
import cn.edu.fudan.vd.accessibility.voice.recognizer.listener.RecognizerCallbackListener;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.VoiceSynthesizer;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.BaseSynthesizerListener;
import cn.edu.fudan.vd.accessibility.voice.synthesizer.listener.SynthesizerCallbackListener;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT;
import static cn.edu.fudan.vd.accessibility.enumeration.Status.FREE;
import static cn.edu.fudan.vd.accessibility.enumeration.Status.REPLAY;
import static cn.edu.fudan.vd.accessibility.logger.DebugLogger.Level.ERROR;
import static cn.edu.fudan.vd.accessibility.logger.DebugLogger.Level.INFORMATION;

public class ReplayProcessor {

    private ReplayProcessor() {
    }

    private static class ReplayProcessorHolder {
        private static ReplayProcessor INSTANCE = new ReplayProcessor();
    }

    public static ReplayProcessor getInstance() {
        return ReplayProcessorHolder.INSTANCE;
    }

    private String targetApp = "";

    private volatile AccessibilityNodeInfo currentRoot;

    private volatile String currentActivity = "";

    // 用于进行列表元素的处理
    private volatile boolean isWaiting = false;
    private volatile int listIndex = 0;
    private volatile boolean useNewContainer = false;
    private volatile Operation listOperation = null;
    private volatile List<String> lastTextList = new ArrayList<>();

    private final Object replayLock = new Object();

    public void replay(List<Operation> operationList, String packageName, String activity) {
        RecordReplayAccessibility.setStatus(REPLAY);
        targetApp = packageName;
        currentRoot = null;
        currentActivity = activity;
        ReplayThread replayThread = new ReplayThread(operationList);
        replayThread.start();
    }

    private static class ReplayThread extends Thread {
        Queue<Operation> operationQueue;

        public ReplayThread(List<Operation> operationList) {
            operationQueue = new LinkedList<>(operationList);
        }

        @Override
        public void run() {
            super.run();
            long waitTime = 2000;
            Operation operation = operationQueue.poll();
            while (operation != null) {
                DebugLogger.log(DebugLogger.Level.INFORMATION, "Waiting to find the same activity");
                if (ReplayProcessorHolder.INSTANCE.currentActivity.equals(operation.getPage()) && ReplayProcessorHolder.INSTANCE.currentRoot != null && !ReplayProcessorHolder.INSTANCE.isWaiting) {
                    AccessibilityNodeInfo nodeInfo = ReplayProcessor.getInstance().findNode(operation.getPath());
                    if (operation.getType() != OperationType.UNKNOWN && nodeInfo == null) {
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    synchronized (ReplayProcessorHolder.INSTANCE.replayLock) {
                        DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Start to wait %s ms", waitTime));
                        try {
                            sleep(1000);
                            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Start to execute operation %s", operation));
                            ReplayProcessorHolder.INSTANCE.executeOperation(operation, nodeInfo);
                            operation = operationQueue.poll();
                            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Operation %s to execute", operation));
                        } catch (InterruptedException e) {
                            DebugLogger.log(DebugLogger.Level.ERROR, String.format("Sleep occur error %s", e));
                            e.printStackTrace();
                        }
                    }
                }
            }
            while (ReplayProcessorHolder.INSTANCE.isWaiting) {
                DebugLogger.log(DebugLogger.Level.INFORMATION, "Waiting to the operation execution.");
            }
            DebugLogger.log(DebugLogger.Level.INFORMATION, "Don't need to wait.");
            if (operationQueue.isEmpty()) {
                ReplayProcessorHolder.INSTANCE.finishReplay();
            }
        }
    }

    private void finishReplay() {
        DebugLogger.log(DebugLogger.Level.INFORMATION, "replay thread finished!");
        RecordReplayAccessibility.setStatus(FREE);
        targetApp = "";
        currentRoot = null;
        currentActivity = "";
        isWaiting = false;
        listIndex = 0;
        useNewContainer = false;
        lastTextList = new ArrayList<>();
    }

    private void executeOperation(Operation operation, AccessibilityNodeInfo nodeInfo) {
        switch (operation.getType()) {
            case CLICK:
                nodeInfo.performAction(ACTION_CLICK);
                break;
            case SELECT:
                AccessibilityNodeInfo container = findNode(operation.getPath());
                isWaiting = true;
                listOperation = operation;
                executeElementInList(operation, container);
            case TYPE:
                List<String> textList = new ArrayList<>();
                if (operation.getRemark().startsWith(Constant.HINT_INPUT_PREFIX)) {
                    isWaiting = true;
                    String text = operation.getRemark().replace(Constant.HINT_INPUT_PREFIX, "");
                    textList.add(Constant.INPUT_ASK_PREFIX + text);
                    VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
                        if (param1.size() == 1) {
                            Bundle arguments = new Bundle();
                            arguments.putCharSequence(ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, param1.get(0));
                            nodeInfo.performAction(ACTION_SET_TEXT, arguments);
                            isWaiting = false;
                        }
                    }))));
                } else if (operation.getRemark().startsWith(Constant.CONSTANT_INPUT_PREFIX)) {
                    String text = operation.getRemark().replace(Constant.CONSTANT_INPUT_PREFIX, "");
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                    nodeInfo.performAction(ACTION_SET_TEXT, arguments);
                }
                break;
            case HUMAN:
                List<String> texts = new ArrayList<>();
                String text = "Access Link的职责已完成，后续还剩余" + operation.getRemark().replace(Constant.CONSTANT_INPUT_PREFIX, "") + "操作，需要由您自己执行啦。";
                texts.add(text);
                VoiceSynthesizer.getInstance().startSpeaking(texts, new BaseSynthesizerListener());
            default:
                break;
        }
    }

    private synchronized void executeElementInList(Operation operation, AccessibilityNodeInfo container) {
        if (container != null) {
            if (listIndex >= container.getChildCount()) {
                useNewContainer = true;
                listIndex = 0;
                container.performAction(ACTION_SCROLL_FORWARD);
                DebugLogger.log(DebugLogger.Level.INFORMATION, "node execute scroll and child count is  " + container.getChildCount());
                return;
            }
            AccessibilityNodeInfo askSelect = container.getChild(listIndex);
            if (askSelect == null) {
                isWaiting = false;
                return;
            }
            String[] positions = operation.getPath().substring(operation.getRemark().length() + 1).split(",");
            for (int i = 1; i < positions.length; i++) {
                askSelect = askSelect.getChild(Integer.parseInt(positions[i]));
                if (askSelect == null) {
                    break;
                }
            }

            Queue<AccessibilityNodeInfo> infoQueue = new LinkedList<>();
            infoQueue.add(askSelect);
            List<String> textList = new ArrayList<>();
            List<String> descriptionList = new ArrayList<>();
            try {
                while (!infoQueue.isEmpty() && textList.isEmpty() && descriptionList.isEmpty()) {
                    AccessibilityNodeInfo node = infoQueue.poll();
                    if (node == null) {
                        continue;
                    }
                    if (node.getText() != null && node.getText().length() > 0) {
                        textList.add(node.getText().toString());
                    }
                    if (node.getContentDescription() != null && node.getContentDescription().length() > 0) {
                        descriptionList.add(node.getContentDescription().toString());
                    }
                    for (int i = 0; i < node.getChildCount(); i++) {
                        AccessibilityNodeInfo child = node.getChild(i);
                        if (child != null) {
                            infoQueue.add(AccessibilityNodeInfo.obtain(child));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            DebugLogger.log(DebugLogger.Level.INFORMATION, "textList is " + textList);
            DebugLogger.log(DebugLogger.Level.INFORMATION, "descriptionList is " + descriptionList);
            if (textList.isEmpty()) {
                textList.addAll(descriptionList);
            }
            textList.add(Constant.SELECT_ASK_PREFIX);
            AccessibilityNodeInfo finalAskSelect = askSelect;
            listIndex++;
            if (textList.size() == 1 || lastTextList.equals(textList)) {
                executeElementInList(operation, container);
            } else {
                lastTextList = textList;
                VoiceSynthesizer.getInstance().startSpeaking(textList, new SynthesizerCallbackListener(param -> {
                    VoiceRecognizer.getInstance().startListening(new RecognizerCallbackListener(param1 -> {
                        if (param1.size() == 1) {
                            if (Constant.YES.equals(param1.get(0))) {
                                listIndex = 0;
                                finalAskSelect.performAction(ACTION_CLICK);
                                isWaiting = false;
                            } else {
                                executeElementInList(operation, container);
                            }
                        } else {
                            executeElementInList(operation, container);
                        }
                    }));
                }));
            }
        }
    }

    private AccessibilityNodeInfo findNode(String viewSequence) {
        AccessibilityNodeInfo result;
        try {
            result = currentRoot;
            String[] sequences = viewSequence.split(",");
            String[] compare = new String[sequences.length];
            for (int i = 0; i < sequences.length; i++) {
                String position = sequences[i];
                int index = Integer.parseInt(position);
                if (result != null && result.getChildCount() > index) {
                    compare[i] = result.getClassName().toString();
                    result = result.getChild(index);
                } else {
                    if (result != null) {
                        compare[i] = result.getClassName().toString();
                        result = null;
                    }
                    break;
                }
                sequences[i] = "-";
            }
            DebugLogger.log(INFORMATION, String.format("Current path is %s", Arrays.toString(compare)));
        } catch (Exception e) {
            DebugLogger.log(ERROR, String.format("Error is %s", e.getLocalizedMessage()));
            result = null;
        }
        DebugLogger.log(INFORMATION, String.format("Find the node is %s", result));
        return result;
    }


    @SuppressLint("SwitchIntDef")
    public void processEvent(AccessibilityEvent event, AccessibilityNodeInfo root) {
        if (RecordReplayAccessibility.getStatus() == REPLAY && event.getPackageName() != null && event.getPackageName().toString().equals(targetApp)) {
            DebugLogger.log(INFORMATION, String.format("Find the event is %s", event));
            switch (event.getEventType()) {
                case TYPE_WINDOW_STATE_CHANGED:
                    processActivityChangedEvent(event, root);
                    break;
                case TYPE_VIEW_SCROLLED:
                    if (listOperation != null && useNewContainer) {
                        useNewContainer = false;
                        executeElementInList(listOperation, event.getSource());
                    }
                    break;
            }
        }
    }

    private void processActivityChangedEvent(AccessibilityEvent event, AccessibilityNodeInfo root) {
        String name = event.getClassName().toString();
        synchronized (replayLock) {
            try {
                currentRoot = root;
            } catch (Exception e) {
                DebugLogger.log(ERROR, String.format("Error is %s", e.getLocalizedMessage()));
            }
            if (name.contains("Activity") || name.startsWith(targetApp) || name.startsWith("com.android.")
                    || name.startsWith("com.tencent.mm.ui.") || name.startsWith("com.alipay.mobile")) {
                DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Last activity is %s", currentActivity));
                currentActivity = name;
                DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Current activity is %s", currentActivity));
            }
        }
    }
}

