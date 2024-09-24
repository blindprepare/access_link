package cn.edu.fudan.vd.accessibility.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.edu.fudan.vd.accessibility.R;
import cn.edu.fudan.vd.accessibility.action.LLMAction;
import cn.edu.fudan.vd.accessibility.action.RecordAction;
import cn.edu.fudan.vd.accessibility.entity.AppInfo;
import cn.edu.fudan.vd.accessibility.logger.DebugLogger;

public class SelectAppActivity extends CanBackActivity {
    private List<AppInfo> appInfoList;

    private boolean useLLM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_select);
        useLLM = getIntent().getBooleanExtra("llm", false);
        updateAppList();
        ListView listView = findViewById(R.id.list_container);
        listView.setOnItemClickListener(new SelectAppOnItemClickListener());
        AppInfoArrayAdapter adapter = new AppInfoArrayAdapter(this, appInfoList);
        listView.setAdapter(adapter);
    }

    private void updateAppList() {
        String appName = getString(R.string.app_name);
        appInfoList = new ArrayList<>();
        PackageManager manager = getPackageManager();
        Intent startIntent = new Intent(Intent.ACTION_MAIN, null);
        startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfoList = manager.queryIntentActivities(startIntent, 0);
        for (ResolveInfo resolveInfo : resolveInfoList) {
            try {
                ApplicationInfo applicationInfo = manager.getApplicationInfo(resolveInfo.activityInfo.packageName, 0);
                String label = applicationInfo.loadLabel(manager).toString();
                if (label.equals(appName)) {
                    continue;
                }
                appInfoList.add(new AppInfo(applicationInfo.packageName, label, applicationInfo.loadIcon(manager), resolveInfo.activityInfo.name));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    void back() {
        Intent intent = new Intent(SelectAppActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    private class SelectAppOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
            AppInfo appInfo = appInfoList.get(index);
            DebugLogger.log(DebugLogger.Level.INFORMATION, String.format("Select app %s.", appInfo.toString()));
            if (useLLM) {
                LLMAction llmAction=new LLMAction(SelectAppActivity.this);
                llmAction.process(appInfo);
            } else {
                RecordAction recordAction = new RecordAction(SelectAppActivity.this);
                recordAction.record(appInfo);
            }
        }
    }

    private static class AppInfoArrayAdapter extends ArrayAdapter<AppInfo> {

        private List<AppInfo> appInfoList;

        public AppInfoArrayAdapter(Context context, List<AppInfo> objects) {
            super(context, 0, objects);
            appInfoList = objects;
        }

        @SuppressLint({"ViewHolder", "InflateParams"})
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.select_app_list_item, null);
            TextView label = convertView.findViewById(R.id.label);
            ImageView icon = convertView.findViewById(R.id.icon);
            AppInfo appInfo = appInfoList.get(position);
            label.setText(appInfo.getAppName());
            icon.setImageDrawable(appInfo.getIcon());
            return convertView;
        }
    }
}