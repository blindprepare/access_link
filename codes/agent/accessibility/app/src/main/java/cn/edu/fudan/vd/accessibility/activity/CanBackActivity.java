package cn.edu.fudan.vd.accessibility.activity;

import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;

public abstract class CanBackActivity extends AppCompatActivity {
    abstract void back();

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == 4) {//回退键的KeyCode是4.
            back();
        }
        return super.dispatchKeyEvent(event);

    }
}
