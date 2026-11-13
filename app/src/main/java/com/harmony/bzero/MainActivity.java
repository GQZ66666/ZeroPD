package com.harmony.bzero;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {

    private static final String PREFS = "config";

    private SeekBar sbScale;
    private SeekBar sbInsideDy;
    private SeekBar sbOutsideDy;
    private TextView tvScale;
    private TextView tvInsideDy;
    private TextView tvOutsideDy;
    private RadioGroup rgSide;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences(PREFS, 0);

        int pad = dp(20);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("电池UI个性化");
        title.setTextSize(24);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("让电池百分比沿用时钟的跑道 0 字形，并调整显示。\n"
                + "修改后点「保存并应用」，再重启系统界面。");
        desc.setTextSize(13);
        desc.setPadding(0, dp(6), 0, dp(10));
        root.addView(desc);

        // ---------------- 图标内 ----------------
        addSection(root, "图标内百分比");
        tvScale = addValueRow(root, "字号");
        sbScale = addSeek(root, 100);
        sbScale.setOnSeekBarChangeListener(new SimpleListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvScale.setText((progress + 50) + "%");
            }
        });

        tvInsideDy = addValueRow(root, "上下位置");
        sbInsideDy = addSeek(root, 40);
        sbInsideDy.setOnSeekBarChangeListener(new SimpleListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvInsideDy.setText(dyText(progress));
            }
        });

        // ---------------- 图标外 ----------------
        addSection(root, "图标外百分比");
        TextView sideLabel = new TextView(this);
        sideLabel.setText("显示在电池");
        sideLabel.setTextSize(14);
        sideLabel.setPadding(0, dp(8), 0, 0);
        root.addView(sideLabel);

        rgSide = new RadioGroup(this);
        rgSide.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton right = new RadioButton(this);
        right.setText("右侧");
        right.setId(View.generateViewId());
        RadioButton left = new RadioButton(this);
        left.setText("左侧");
        left.setId(View.generateViewId());
        rgSide.addView(right);
        rgSide.addView(left);
        root.addView(rgSide);

        tvOutsideDy = addValueRow(root, "上下位置");
        sbOutsideDy = addSeek(root, 40);
        sbOutsideDy.setOnSeekBarChangeListener(new SimpleListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvOutsideDy.setText(dyText(progress));
            }
        });

        Button save = new Button(this);
        save.setText("保存并应用");
        save.setGravity(Gravity.CENTER);
        save.setOnClickListener(v -> save());
        root.addView(save, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button restart = new Button(this);
        restart.setText("重启系统界面");
        restart.setGravity(Gravity.CENTER);
        restart.setOnClickListener(v -> restart());
        root.addView(restart);

        load();
        setContentView(scroll);
    }

    private void load() {
        int scale = sp.getInt(Config.KEY_INSIDE_SCALE, 100);
        float insideDy = sp.getFloat(Config.KEY_INSIDE_DY, 0f);
        int side = sp.getInt(Config.KEY_OUTSIDE_SIDE, 0);
        float outsideDy = sp.getFloat(Config.KEY_OUTSIDE_DY, 0f);

        sbScale.setProgress(Math.max(0, Math.min(100, scale - 50)));
        tvScale.setText(scale + "%");

        int p1 = Math.round((insideDy + 10f) / 0.5f);
        sbInsideDy.setProgress(Math.max(0, Math.min(40, p1)));
        tvInsideDy.setText(dyText(sbInsideDy.getProgress()));

        int p2 = Math.round((outsideDy + 10f) / 0.5f);
        sbOutsideDy.setProgress(Math.max(0, Math.min(40, p2)));
        tvOutsideDy.setText(dyText(sbOutsideDy.getProgress()));

        if (side == 1) {
            ((RadioButton) rgSide.getChildAt(1)).setChecked(true);
        } else {
            ((RadioButton) rgSide.getChildAt(0)).setChecked(true);
        }
    }

    private void save() {
        int scale = sbScale.getProgress() + 50;
        float insideDy = sbInsideDy.getProgress() * 0.5f - 10f;
        float outsideDy = sbOutsideDy.getProgress() * 0.5f - 10f;
        int side = rgSide.getCheckedRadioButtonId() == rgSide.getChildAt(1).getId() ? 1 : 0;

        sp.edit()
                .putInt(Config.KEY_INSIDE_SCALE, scale)
                .putFloat(Config.KEY_INSIDE_DY, insideDy)
                .putInt(Config.KEY_OUTSIDE_SIDE, side)
                .putFloat(Config.KEY_OUTSIDE_DY, outsideDy)
                .apply();
        try {
            getContentResolver().notifyChange(Config.URI, null);
        } catch (Throwable ignored) {
        }
        Toast.makeText(this, "已保存，点「重启系统界面」生效", Toast.LENGTH_SHORT).show();
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

    private void addSection(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setPadding(0, dp(18), 0, dp(4));
        root.addView(tv);
    }

    private TextView addValueRow(LinearLayout root, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(14);
        TextView value = new TextView(this);
        value.setTextSize(14);
        value.setPadding(dp(12), 0, 0, 0);
        row.addView(name);
        row.addView(value);
        root.addView(row);
        return value;
    }

    private SeekBar addSeek(LinearLayout root, int max) {
        SeekBar seek = new SeekBar(this);
        seek.setMax(max);
        root.addView(seek, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return seek;
    }

    private static String dyText(int progress) {
        float v = progress * 0.5f - 10f;
        return String.format(java.util.Locale.US, "%+.1f dp", v);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
