package cn.edu.fudan.vd.accessibility.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import cn.edu.fudan.vd.accessibility.R;
import cn.edu.fudan.vd.accessibility.util.AppManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        killTargetApp();
        setContentView(R.layout.activity_main);
        final Button startRecord = findViewById(R.id.startRecord);
        startRecord.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SelectAppActivity.class);
            startActivity(intent);
            finish();
        });

        final Button llmHelper = findViewById(R.id.LLMHelper);
        llmHelper.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SelectAppActivity.class);
            intent.putExtra("llm", true);
            startActivity(intent);
            finish();
        });

    }

    private void killTargetApp() {
        String targetApp = getIntent().getStringExtra("targetApp");
        if (targetApp != null) {
            AppManager.stopApp(getBaseContext(), targetApp);
        }
    }
}