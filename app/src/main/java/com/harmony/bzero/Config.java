package com.harmony.bzero;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

/** 模块 App 与 SystemUI 之间的配置（通过 ContentProvider 跨进程读取）。 */
public final class Config {

    public static final String AUTHORITY = "com.harmony.bzero.prefs";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/config");

    public static final String KEY_INSIDE_SCALE = "inside_scale";
    public static final String KEY_INSIDE_DY = "inside_dy";
    public static final String KEY_INSIDE_WEIGHT = "inside_weight";
    public static final String KEY_OUTSIDE_SIDE = "outside_side";
    public static final String KEY_OUTSIDE_SCALE = "outside_scale";
    public static final String KEY_OUTSIDE_DY = "outside_dy";
    public static final String KEY_OUTSIDE_WEIGHT = "outside_weight";

    public static volatile int insideScale = 100;
    public static volatile float insideDy = 0f;
    public static volatile int insideWeight = 650;

    public static volatile int outsideSide = 0;   // 0 = 右侧，1 = 左侧
    public static volatile int outsideScale = 100;
    public static volatile float outsideDy = 0f;
    public static volatile int outsideWeight = 650;

    private Config() {
    }

    public static void load(Context ctx) {
        try {
            Bundle b = ctx.getContentResolver().call(URI, "get", null, null);
            if (b == null) {
                return;
            }
            insideScale = b.getInt(KEY_INSIDE_SCALE, 100);
            insideDy = b.getFloat(KEY_INSIDE_DY, 0f);
            insideWeight = b.getInt(KEY_INSIDE_WEIGHT, 650);
            outsideSide = b.getInt(KEY_OUTSIDE_SIDE, 0);
            outsideScale = b.getInt(KEY_OUTSIDE_SCALE, 100);
            outsideDy = b.getFloat(KEY_OUTSIDE_DY, 0f);
            outsideWeight = b.getInt(KEY_OUTSIDE_WEIGHT, 650);
        } catch (Throwable ignored) {
        }
    }
}
