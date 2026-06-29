package com.remi8.remiscript.interpreter;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

/**
 * خدمة تجميع سكربتات remiscript في الخلفية
 */
public class CompilerService extends Service {

    private static final String TAG = "REMI8_Compiler";
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public CompilerService getService() { return CompilerService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    /**
     * تحميل وتنفيذ ملف .remi8s
     */
    public String loadScript(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return null;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            Log.i(TAG, "تم تحميل السكربت: " + path);
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "فشل تحميل السكربت: " + e.getMessage());
            return null;
        }
    }

    /**
     * التحقق من صحة سكربت
     */
    public boolean validateScript(String code) {
        try {
            com.remi8.remiscript.parser.RemiScriptLexer lexer =
                new com.remi8.remiscript.parser.RemiScriptLexer(code);
            lexer.tokenize();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "سكربت غير صالح: " + e.getMessage());
            return false;
        }
    }
}
