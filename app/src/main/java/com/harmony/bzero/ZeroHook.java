package com.harmony.bzero;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 时钟的字体是“跑道 0”字形，电池百分比用的是另一套 fallback，所以显示圆弧 0。
 * 这里把时钟的 typeface 复制到电池百分比，并应用大小/上下位置/内外显设置。
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
        int scale = Config.insideScale;
        if (scale < 30) {
            scale = 30;
        }
        if (scale > 200) {
            scale = 200;
        }
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * scale / 100f);
        tv.setTranslationY(dp(Config.insideDy));
    }

    private void applyOutside(Object battery) {
        Object v = field(battery, "mOutsidePercentView");
        if (!(v instanceof TextView)) {
            return;
        }
        TextView tv = (TextView) v;
        applyTypeface(tv);
        tv.setTranslationY(dp(Config.outsideDy));

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
        tv.invalidate();
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
