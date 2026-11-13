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
    public static final String KEY_OUTSIDE_SIDE = "outside_side";
    public static final String KEY_OUTSIDE_DY = "outside_dy";

    public static volatile int insideScale = 100;
    public static volatile float insideDy = 0f;
    public static volatile int outsideSide = 0;   // 0 = 右侧，1 = 左侧
    public static volatile float outsideDy = 0f;

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
            outsideSide = b.getInt(KEY_OUTSIDE_SIDE, 0);
            outsideDy = b.getFloat(KEY_OUTSIDE_DY, 0f);
        } catch (Throwable ignored) {
        }
    }
}
