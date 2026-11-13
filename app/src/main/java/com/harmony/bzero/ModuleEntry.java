package com.harmony.bzero;

import android.app.Application;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class ModuleEntry implements IXposedHookLoadPackage {

    public static final String TAG = "BatteryZero";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lp) {
        final String pkg = lp.packageName;
        if (!"com.android.systemui".equals(pkg) && !"com.hihonor.systemui".equals(pkg)) {
            return;
        }
        XposedBridge.log(TAG + ": loading into " + pkg);
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Context ctx = (Context) param.args[0];
                        if (ctx == null || !pkg.equals(ctx.getPackageName())) {
                            return;
                        }
                        try {
                            Config.load(ctx);
                            ctx.getContentResolver().registerContentObserver(
                                    Config.URI, false,
                                    new ContentObserver(new Handler(Looper.getMainLooper())) {
                                        @Override
                                        public void onChange(boolean selfChange) {
                                            Config.load(ctx);
                                        }
                                    });
                            new ZeroHook(ctx).install(lp.classLoader);
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                    }
                });
    }
}
