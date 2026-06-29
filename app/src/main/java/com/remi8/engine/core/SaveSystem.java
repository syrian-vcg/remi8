package com.remi8.engine.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * نظام الحفظ والتحميل - REMI8
 * يحفظ بيانات اللعبة (نقاط، مراحل، إعدادات)
 */
public class SaveSystem {

    private static final String TAG = "REMI8_Save";
    private static final String PREFS_NAME = "remi8_save";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    // بيانات الحفظ الحالية
    private final Map<String, Object> saveData = new HashMap<>();

    public SaveSystem(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * حفظ قيمة
     */
    public void set(String key, Object value) {
        saveData.put(key, value);
    }

    /**
     * قراءة قيمة
     */
    public Object get(String key, Object defaultValue) {
        return saveData.getOrDefault(key, defaultValue);
    }

    public float getFloat(String key, float def) {
        Object v = saveData.get(key);
        return v instanceof Number ? ((Number) v).floatValue() : def;
    }

    public int getInt(String key, int def) {
        Object v = saveData.get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    public String getString(String key, String def) {
        Object v = saveData.get(key);
        return v instanceof String ? (String) v : def;
    }

    public boolean getBool(String key, boolean def) {
        Object v = saveData.get(key);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    /**
     * حفظ كل البيانات على القرص
     */
    public boolean save() {
        return save("default");
    }

    public boolean save(String slot) {
        try {
            String json = gson.toJson(saveData);
            prefs.edit().putString("slot_" + slot, json).apply();
            Log.i(TAG, "تم الحفظ في: slot_" + slot);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "فشل الحفظ: " + e.getMessage());
            return false;
        }
    }

    /**
     * تحميل البيانات من القرص
     */
    @SuppressWarnings("unchecked")
    public boolean load() {
        return load("default");
    }

    @SuppressWarnings("unchecked")
    public boolean load(String slot) {
        try {
            String json = prefs.getString("slot_" + slot, null);
            if (json == null) return false;
            Map<String, Object> loaded = gson.fromJson(json, Map.class);
            if (loaded != null) {
                saveData.clear();
                saveData.putAll(loaded);
            }
            Log.i(TAG, "تم التحميل من: slot_" + slot);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "فشل التحميل: " + e.getMessage());
            return false;
        }
    }

    /**
     * حذف بيانات الحفظ
     */
    public void delete(String slot) {
        prefs.edit().remove("slot_" + slot).apply();
        Log.i(TAG, "تم حذف: slot_" + slot);
    }

    public void deleteAll() {
        prefs.edit().clear().apply();
        saveData.clear();
    }

    /**
     * هل يوجد حفظ؟
     */
    public boolean hasSave(String slot) {
        return prefs.contains("slot_" + slot);
    }

    public boolean hasSave() {
        return hasSave("default");
    }

    public Map<String, Object> getAllData() { return saveData; }
}
