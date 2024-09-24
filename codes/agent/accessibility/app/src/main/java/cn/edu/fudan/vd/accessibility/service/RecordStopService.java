package cn.edu.fudan.vd.accessibility.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import cn.edu.fudan.vd.accessibility.R;
import cn.edu.fudan.vd.accessibility.activity.MainActivity;
import cn.edu.fudan.vd.accessibility.core.RecordProcessor;


public class RecordStopService extends Service {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View view;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @SuppressLint({"InflateParams", "ClickableViewAccessibility", "RtlHardcoded"})
    private void init() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;

        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.x = 700;
        layoutParams.y = 200;

        view = LayoutInflater.from(this).inflate(R.layout.stop_button, null);

        Button button = view.findViewById(R.id.stop);
        button.setOnClickListener(view -> stopButtonToDo());
        button.setOnTouchListener(new View.OnTouchListener() {
            int inX = layoutParams.width >> 1, inY = layoutParams.height >> 2;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        inX = (int) event.getX();
                        inY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int nowX = (int) event.getRawX();
                        int nowY = (int) event.getRawY();
                        layoutParams.x = nowX - inX;
                        layoutParams.y = nowY - inY;
                        windowManager.updateViewLayout(view, layoutParams);
                        break;
                }
                return false;
            }
        });

        windowManager.addView(view, layoutParams);
    }

    public void stopButtonToDo() {
        String targetApp = RecordProcessor.getTargetApp();
        RecordProcessor.stopRecord();
        Intent main = new Intent(RecordStopService.this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        main.putExtra("targetApp", targetApp);
        startActivity(main);
        stopSelf();
        stopService(new Intent(RecordStopService.this, SetHumanActionService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (view != null)
            windowManager.removeView(view);
    }
}
