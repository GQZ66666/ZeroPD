package com.harmony.bzero;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

public final class PrefsProvider extends ContentProvider {

    static final String PREFS = "config";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!"get".equals(method)) {
            return null;
        }
        SharedPreferences sp = getContext().getSharedPreferences(PREFS, 0);
        Bundle b = new Bundle();
        b.putInt(Config.KEY_INSIDE_SCALE, sp.getInt(Config.KEY_INSIDE_SCALE, 100));
        b.putFloat(Config.KEY_INSIDE_DY, sp.getFloat(Config.KEY_INSIDE_DY, 0f));
        b.putInt(Config.KEY_OUTSIDE_SIDE, sp.getInt(Config.KEY_OUTSIDE_SIDE, 0));
        b.putFloat(Config.KEY_OUTSIDE_DY, sp.getFloat(Config.KEY_OUTSIDE_DY, 0f));
        return b;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
