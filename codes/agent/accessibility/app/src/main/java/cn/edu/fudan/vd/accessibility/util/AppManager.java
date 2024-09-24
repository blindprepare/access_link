package cn.edu.fudan.vd.accessibility.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import cn.edu.fudan.vd.accessibility.entity.AppInfo;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;
import cn.edu.fudan.vd.accessibility.service.RecordStopService;
import cn.edu.fudan.vd.accessibility.service.SetHumanActionService;

public class AppManager {

    public static void launchActivity(Context context, AppInfo appInfo) {
        DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("To start app %s", appInfo.toString()));
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(appInfo.getPackageName(), appInfo.getActivityName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void stopApp(Context context, String packageName) {
        DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("To stop app is %s.", packageName));
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        manager.killBackgroundProcesses(packageName);
    }

    public static void startRecordStopFloat(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
                return;
            }
        }
        Intent stopApp = new Intent(context, RecordStopService.class);
        context.startService(stopApp);
    }

    public static void startSetHumanActionFloat(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
                return;
            }
        }
        Intent stopApp = new Intent(context, SetHumanActionService.class);
        context.startService(stopApp);
    }


    @SuppressLint("ShowToast")
    public static void launchSettingAccessibility(Context context) {
        DebugLogger.log(DebugLogger.Level.INFORMATION, "To open record and replay accessibility.");
        Toast.makeText(context, "未开启Accessibility能力，请开启", Toast.LENGTH_SHORT).show();
        try {
            context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            context.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
