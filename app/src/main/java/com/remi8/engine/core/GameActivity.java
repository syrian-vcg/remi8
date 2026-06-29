package com.remi8.engine.core;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.remi8.R;

/**
 * نشاط تشغيل اللعبة - REMI8
 * يعرض اللعبة بملء الشاشة مع إخفاء شريط الحالة
 */
public class GameActivity extends Activity implements GameEngine.GameEngineListener {

    private GameEngine gameEngine;
    private GameView gameView;
    private TextView fpsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ملء الشاشة
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // إخفاء شريط التنقل
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_game);

        // تهيئة المحرك
        gameEngine = new GameEngine(this);
        gameEngine.setListener(this);

        gameView = findViewById(R.id.game_view);
        fpsText = findViewById(R.id.fps_text);

        // الحصول على اسم المشروع من Intent
        String projectPath = getIntent().getStringExtra("project_path");
        if (projectPath != null) {
            loadProject(projectPath);
        }

        // تشغيل المحرك
        gameEngine.start();
    }

    private void loadProject(String path) {
        try {
            gameEngine.getSceneManager().loadProjectFromPath(path);
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في تحميل المشروع: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameEngine != null) gameEngine.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameEngine != null) gameEngine.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameEngine != null) gameEngine.stop();
    }

    @Override
    public void onEngineStarted() {
        Toast.makeText(this, "تم تشغيل محرك REMI8", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEngineStopped() {}

    @Override
    public void onFPSUpdate(int fps) {
        if (fpsText != null) {
            fpsText.setText("FPS: " + fps);
        }
    }

    @Override
    public void onError(String error) {
        Toast.makeText(this, "خطأ: " + error, Toast.LENGTH_SHORT).show();
    }
}
