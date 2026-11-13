package com.harmony.bzero;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = (int) (getResources().getDisplayMetrics().density * 20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("电池斜杠零");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText("把状态栏时钟当前的 fontFeatureSettings 复制到电池百分比，"
                + "让百分比也显示斜杠零。\n\n"
                + "在 LSPosed 中启用本模块，作用域勾选「系统界面」，然后重启系统界面。");
        body.setTextSize(14);
        body.setTextColor(Color.DKGRAY);
        body.setPadding(0, pad / 2, 0, pad);
        root.addView(body);

        Button restart = new Button(this);
        restart.setText("重启系统界面");
        restart.setGravity(Gravity.CENTER);
        restart.setOnClickListener(v -> restart());
        root.addView(restart);

        setContentView(root);
    }

    private void restart() {
        new Thread(() -> {
            String msg;
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                        "pkill -f '^com.android.systemui$' || killall com.android.systemui"});
                msg = p.waitFor() == 0 ? "已重启系统界面" : "重启失败，请手动重启";
            } catch (Throwable t) {
                msg = "重启失败：" + t;
            }
            final String m = msg;
            runOnUiThread(() -> Toast.makeText(this, m, Toast.LENGTH_LONG).show());
        }, "bzero-restart").start();
    }
}
