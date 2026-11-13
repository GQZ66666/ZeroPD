package com.harmony.bzero;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 时钟 HwStatusBarClock 已经能显示斜杠零，说明主题字体里带这个特性；
 * 电池百分比的 CenteredTextView 只写了 "pnum"，所以拿不到斜杠零。
 *
 * 这里把时钟当前的 fontFeatureSettings（必要时连 typeface 一起）复制到
 * HwStatusBarBatteryView 的 mInsidePercentView / mOutsidePercentView。
 */
public final class ZeroHook {

    private static final String CLOCK = "com.android.systemui.time.HwStatusBarClock";
    private static final String BATTERY = "com.android.systemui.battery.HwStatusBarBatteryView";

    private static volatile String sFeature;
    private static volatile Typeface sTypeface;

    private static final Map<Object, Boolean> BATTERIES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicBoolean sLogged = new AtomicBoolean();

    private final Context ctx;

    public ZeroHook(Context ctx) {
        this.ctx = ctx;
    }

    public void install(ClassLoader cl) {
        Class<?> clock = XposedHelpers.findClassIfExists(CLOCK, cl);
        if (clock != null) {
            XposedBridge.hookAllMethods(clock, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (param.thisObject instanceof TextView) {
                            TextView tv = (TextView) param.thisObject;
                            sFeature = tv.getFontFeatureSettings();
                            sTypeface = tv.getTypeface();
                            if (sLogged.compareAndSet(false, true)) {
                                XposedBridge.log(ModuleEntry.TAG + ": clock feature=" + sFeature);
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
        XposedBridge.log(ModuleEntry.TAG + ": battery hooked");
    }

    private void apply(Object battery) {
        applyOne(battery, "mInsidePercentView");
        applyOne(battery, "mOutsidePercentView");
    }

    private void applyOne(Object battery, String field) {
        Object v = field(battery, field);
        if (!(v instanceof TextView)) {
            return;
        }
        TextView tv = (TextView) v;
        String feature = sFeature;
        if (feature == null || feature.trim().isEmpty()) {
            feature = "pnum,zero";
        } else if (!feature.toLowerCase().contains("zero")) {
            feature = feature + ",zero";
        }
        tv.setFontFeatureSettings(feature);
        if (sTypeface != null) {
            tv.setTypeface(sTypeface);
        }
        tv.invalidate();
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
