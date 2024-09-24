package cn.edu.fudan.vd.accessibility.entity;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String packageName;
    private String appName;
    private Drawable icon;
    private String activityName;

    public Drawable getIcon() {
        return icon;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    public AppInfo(String packageName, String appName, Drawable icon, String activityName) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.activityName = activityName;
    }

    @Override
    public String toString() {
        return "{" +
                "packageName='" + packageName + '\'' +
                ", appName='" + appName + '\'' +
                ", icon=" + icon +
                ", activityName='" + activityName + '\'' +
                '}';
    }
}
