package com.harmony.bzero;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 电池百分比的文字最终都经过 Canvas.drawText / drawTextRun：
 *   - 内显普通状态：BatteryTintView.onDraw 用 mTextPaint 画，字号/字重直接取自 mInsidePercent；
 *   - 内显充电/低电：CenteredTextView 自己画；
 *   - 外显：mOutsidePercentView 自己画。
 *
 * 所以字号/字重统一设在 mInsidePercent / mOutsidePercentView 上（BatteryTintView 会自动读取），
 * 上下位置统一在电池绘制期间对 Canvas 文字基线加偏移。
 */
public final class ZeroHook {

    private static final String CLOCK = "com.android.systemui.time.HwStatusBarClock";
    private static final String BATTERY = "com.android.systemui.battery.HwStatusBarBatteryView";

    private static volatile String sFeature;
    private static volatile Typeface sTypeface;

    private static final Map<Object, Boolean> BATTERIES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextView, Float> BASE_SIZE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextView, Integer> SIDE =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Object> BATTERY_DRAW = new ThreadLocal<>();
    private static final AtomicBoolean sLogged = new AtomicBoolean();

    private final Context ctx;

    public ZeroHook(Context ctx) {
        this.ctx = ctx;
    }

    public void install(ClassLoader cl) {
        hookClock(cl);
        hookBattery(cl);
        hookCanvasText();
    }

    private void hookClock(ClassLoader cl) {
        Class<?> clock = XposedHelpers.findClassIfExists(CLOCK, cl);
        if (clock == null) {
            return;
        }
        XposedBridge.hookAllMethods(clock, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.thisObject instanceof TextView) {
                        TextView tv = (TextView) param.thisObject;
                        sFeature = tv.getFontFeatureSettings();
                        sTypeface = tv.getTypeface();
                        if (sLogged.compareAndSet(false, true)) {
                            XposedBridge.log(ModuleEntry.TAG + ": clock typeface="
                                    + sTypeface + " feature=" + sFeature);
                        }
                        synchronized (BATTERIES) {
                            for (Object b : BATTERIES.keySet()) {
                                apply(b);
                            }
                        }
                    }
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        });
    }

    private void hookBattery(ClassLoader cl) {
        Class<?> battery = XposedHelpers.findClassIfExists(BATTERY, cl);
        if (battery == null) {
            XposedBridge.log(ModuleEntry.TAG + ": battery view not found");
            return;
        }
        for (String method : new String[]{"onAttachedToWindow", "onBatteryUpdate"}) {
            XposedBridge.hookAllMethods(battery, method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        BATTERIES.put(param.thisObject, Boolean.TRUE);
                        apply(param.thisObject);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            });
        }

        Method dispatch = findInherited(battery, "dispatchDraw", Canvas.class);
        if (dispatch != null) {
            XposedBridge.hookMethod(dispatch, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (battery.isInstance(param.thisObject)) {
                        BATTERY_DRAW.set(param.thisObject);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    BATTERY_DRAW.remove();
                }
            });
        }
        XposedBridge.log(ModuleEntry.TAG + ": battery hooked");
    }

    private void hookCanvasText() {
        XposedBridge.hookAllMethods(Canvas.class, "drawText", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                int yIdx = param.args.length == 4 ? 2 : (param.args.length == 6 ? 4 : -1);
                adjust(param, yIdx);
            }
        });
        XposedBridge.hookAllMethods(Canvas.class, "drawTextRun", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length == 9) {
                    adjust(param, 6);
                }
            }
        });
    }

    private void adjust(XC_MethodHook.MethodHookParam param, int yIdx) {
        try {
            Object battery = BATTERY_DRAW.get();
            if (battery == null || yIdx < 0 || !isPercent(param.args)) {
                return;
            }
            boolean inside = getBool(battery, "mIsShowPercentIn");
            float dy = dp(inside ? Config.insideDy : Config.outsideDy);
            if (dy != 0f) {
                param.args[yIdx] = (Float) param.args[yIdx] + dy;
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void apply(Object battery) {
        applyInside(battery);
        applyOutside(battery);
    }

    private void applyInside(Object battery) {
        Object v = field(battery, "mInsidePercentView");
        if (!(v instanceof TextView)) {
            return;
        }
        TextView tv = (TextView) v;
        applyTypeface(tv);
        float base = BASE_SIZE.containsKey(tv) ? BASE_SIZE.get(tv) : tv.getTextSize();
        BASE_SIZE.put(tv, base);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * clamp(Config.insideScale, 30, 200) / 100f);
        applyWeight(tv, Config.insideWeight);
    }

    private void applyOutside(Object battery) {
        Object v = field(battery, "mOutsidePercentView");
        if (!(v instanceof TextView)) {
            return;
        }
        TextView tv = (TextView) v;
        applyTypeface(tv);
        float base = BASE_SIZE.containsKey(tv) ? BASE_SIZE.get(tv) : tv.getTextSize();
        BASE_SIZE.put(tv, base);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * clamp(Config.outsideScale, 30, 200) / 100f);
        applyWeight(tv, Config.outsideWeight);

        int want = Config.outsideSide;
        Integer last = SIDE.get(tv);
        if (last != null && last == want) {
            return;
        }
        ViewGroup parent = tv.getParent() instanceof ViewGroup ? (ViewGroup) tv.getParent() : null;
        if (parent == null) {
            return;
        }
        try {
            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            if (lp == null) {
                return;
            }
            parent.removeView(tv);
            if (want == 1) {
                parent.addView(tv, 0, lp);
            } else {
                parent.addView(tv, lp);
            }
            SIDE.put(tv, want);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void applyTypeface(TextView tv) {
        if (sTypeface != null) {
            tv.setTypeface(sTypeface);
        }
        String feature = sFeature;
        if (feature != null && !feature.trim().isEmpty()) {
            tv.setFontFeatureSettings(feature);
        }
    }

    private void applyWeight(TextView tv, int weight) {
        Typeface base = sTypeface != null ? sTypeface : tv.getTypeface();
        if (base == null) {
            return;
        }
        weight = clamp(weight, 100, 900);
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                Typeface tf = Typeface.create(base, weight, false);
                if (tf != null) {
                    tv.setTypeface(tf);
                }
            } else {
                tv.setTypeface(base, weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean isPercent(Object[] args) {
        if (args.length == 0) {
            return false;
        }
        Object t = args[0];
        if (!(t instanceof CharSequence)) {
            return false;
        }
        CharSequence text = (CharSequence) t;
        if (text.length() == 0 || text.length() > 3) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static Method findInherited(Class<?> start, String name, Class<?>... params) {
        for (Class<?> c = start; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    private static boolean getBool(Object obj, String name) {
        Object v = field(obj, name);
        return v instanceof Boolean && (Boolean) v;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private float dp(float v) {
        return v * ctx.getResources().getDisplayMetrics().density;
    }

    private static Object field(Object obj, String name) {
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }
}
