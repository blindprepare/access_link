package cn.edu.fudan.vd.accessibility.core;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

import cn.edu.fudan.vd.accessibility.enumeration.Status;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class RecordReplayAccessibility extends AccessibilityService {

    private static final String TAG = RecordReplayAccessibility.class.getSimpleName();

    private static RecordReplayAccessibility recordReplayAccessibility;

    private static Status status = Status.FREE;

    public static synchronized Status getStatus() {
        return status;
    }

    public static synchronized void setStatus(Status status) {
        RecordReplayAccessibility.status = status;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        DebugLogger.log(DebugLogger.Level.INFORMATION, "Service connect .");
        AccessibilityServiceInfo accessibilityServiceInfo = getServiceInfo();
        setServiceInfo(accessibilityServiceInfo);
        recordReplayAccessibility = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        switch (status) {
            case RECORD:
                RecordProcessor.processEvent(event, getRootInActiveWindow());
                break;
            case REPLAY:
                ReplayProcessor.getInstance().processEvent(event, getRootInActiveWindow());
                break;
            case FREE:
                DebugLogger.log(DebugLogger.Level.INFORMATION,String.format("Produce event is %s .",event));
                break;
        }
    }

    @Override
    public void onInterrupt() {

    }

    public static boolean isStart() {
        return recordReplayAccessibility != null;
    }

    public static RecordReplayAccessibility getRecordReplayAccessibility() {
        return recordReplayAccessibility;
    }

    public void goBack() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }
}
