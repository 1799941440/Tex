package com.wz.tex.bean;

import android.os.Build;

import com.wz.tex.exception.SettingsException;
import com.wz.tex.wrappers.ContentProvider;
import com.wz.tex.wrappers.ServiceManager;

import java.io.IOException;

public class Settings {

    public static final String TABLE_SYSTEM = ContentProvider.TABLE_SYSTEM;
    public static final String TABLE_SECURE = ContentProvider.TABLE_SECURE;
    public static final String TABLE_GLOBAL = ContentProvider.TABLE_GLOBAL;

    private final ServiceManager serviceManager;

    public Settings(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    private static void execSettingsPut(String table, String key, String value) throws SettingsException {
        try {
            Command.exec("settings", "put", table, key, value);
        } catch (IOException | InterruptedException e) {
            throw new SettingsException("put", table, key, value, e);
        }
    }

    private static String execSettingsGet(String table, String key) throws SettingsException {
        try {
            return Command.execReadLine("settings", "get", table, key);
        } catch (IOException | InterruptedException e) {
            throw new SettingsException("get", table, key, null, e);
        }
    }

    public String getValue(String table, String key) throws SettingsException {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            // on Android >= 12, it always fails: <https://github.com/Genymobile/scrcpy/issues/2788>
            try (ContentProvider provider = serviceManager.getActivityManager().createSettingsProvider()) {
                return provider.getValue(table, key);
            } catch (SettingsException e) {
                e.printStackTrace();
            }
        }

        return execSettingsGet(table, key);
    }

    public void putValue(String table, String key, String value) throws SettingsException {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            // on Android >= 12, it always fails: <https://github.com/Genymobile/scrcpy/issues/2788>
            try (ContentProvider provider = serviceManager.getActivityManager().createSettingsProvider()) {
                provider.putValue(table, key, value);
            } catch (SettingsException e) {
                e.printStackTrace();
            }
        }

        execSettingsPut(table, key, value);
    }

    public String getAndPutValue(String table, String key, String value) throws SettingsException {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            // on Android >= 12, it always fails: <https://github.com/Genymobile/scrcpy/issues/2788>
            try (ContentProvider provider = serviceManager.getActivityManager().createSettingsProvider()) {
                String oldValue = provider.getValue(table, key);
                if (!value.equals(oldValue)) {
                    provider.putValue(table, key, value);
                }
                return oldValue;
            } catch (SettingsException e) {
                e.printStackTrace();
            }
        }

        String oldValue = getValue(table, key);
        if (!value.equals(oldValue)) {
             putValue(table, key, value);
        }
        return oldValue;
    }
}