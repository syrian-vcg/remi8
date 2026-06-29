package com.remi8.engine.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.remi8.engine.audio.AudioManager;
import com.remi8.engine.input.InputManager;
import com.remi8.engine.physics.PhysicsEngine;
import com.remi8.engine.renderer.Renderer2D;
import com.remi8.engine.scene.SceneManager;
import com.remi8.remiscript.interpreter.RemiScriptInterpreter;

/**
 * محرك الألعاب الرئيسي REMI8
 * يدير حلقة اللعبة الرئيسية، الرندرة، الفيزياء، والصوت
 */
public class GameEngine {

    private static final String TAG = "REMI8_Engine";
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME_MS = 1000 / TARGET_FPS;

    // مكونات المحرك الأساسية
    private final Context context;
    private final Renderer2D renderer;
    private final PhysicsEngine physicsEngine;
    private final AudioManager audioManager;
    private final InputManager inputManager;
    private final SceneManager sceneManager;
    private final RemiScriptInterpreter scriptInterpreter;

    // حالة المحرك
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private Thread gameThread;
    private long lastFrameTime;
    private int currentFPS;
    private float deltaTime;

    // معلومات المشروع
    private String gameName = "لعبة جديدة";
    private String gameVersion = "1.0";
    private int screenWidth = 1280;
    private int screenHeight = 720;

    // واجهة الاستدعاءات
    private GameEngineListener listener;

    public interface GameEngineListener {
        void onEngineStarted();
        void onEngineStopped();
        void onFPSUpdate(int fps);
        void onError(String error);
    }

    public GameEngine(Context context) {
        this.context = context;
        this.renderer = new Renderer2D(context);
        this.physicsEngine = new PhysicsEngine();
        this.audioManager = new AudioManager(context);
        this.inputManager = new InputManager();
        this.sceneManager = new SceneManager(this);
        this.scriptInterpreter = new RemiScriptInterpreter(this);

        Log.i(TAG, "تم تهيئة محرك REMI8 بنجاح");
    }

    /**
     * تشغيل المحرك
     */
    public void start() {
        if (isRunning) return;

        isRunning = true;
        isPaused = false;
        lastFrameTime = System.currentTimeMillis();

        gameThread = new Thread(this::gameLoop, "REMI8-GameThread");
        gameThread.setDaemon(true);
        gameThread.start();

        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onEngineStarted());
        }

        Log.i(TAG, "تم تشغيل محرك REMI8");
    }

    /**
     * إيقاف المحرك
     */
    public void stop() {
        isRunning = false;
        if (gameThread != null) {
            try {
                gameThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        audioManager.release();

        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(() -> listener.onEngineStopped());
        }

        Log.i(TAG, "تم إيقاف محرك REMI8");
    }

    /**
     * تعليق / استئناف المحرك
     */
    public void pause() {
        isPaused = true;
        audioManager.pauseAll();
    }

    public void resume() {
        isPaused = false;
        audioManager.resumeAll();
        lastFrameTime = System.currentTimeMillis();
    }

    /**
     * حلقة اللعبة الرئيسية
     */
    private void gameLoop() {
        int fpsCounter = 0;
        long fpsTimer = System.currentTimeMillis();

        while (isRunning) {
            if (isPaused) {
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            long currentTime = System.currentTimeMillis();
            deltaTime = (currentTime - lastFrameTime) / 1000.0f;
            lastFrameTime = currentTime;

            // تحديث المنطق
            update(deltaTime);

            // رندرة الإطار
            render();

            // حساب FPS
            fpsCounter++;
            if (currentTime - fpsTimer >= 1000) {
                currentFPS = fpsCounter;
                fpsCounter = 0;
                fpsTimer = currentTime;
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        listener.onFPSUpdate(currentFPS));
                }
            }

            // تنظيم سرعة الإطار
            long elapsed = System.currentTimeMillis() - currentTime;
            long sleepTime = FRAME_TIME_MS - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * تحديث حالة اللعبة
     */
    private void update(float dt) {
        try {
            // تحديث المدخلات
            inputManager.update();

            // تحديث السيناريو النشط
            sceneManager.update(dt);

            // تحديث محرك الفيزياء
            physicsEngine.update(dt);

            // تشغيل سكربتات remiscript
            scriptInterpreter.update(dt);

        } catch (Exception e) {
            Log.e(TAG, "خطأ في تحديث اللعبة: " + e.getMessage());
            if (listener != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                    listener.onError("خطأ في تحديث اللعبة: " + e.getMessage()));
            }
        }
    }

    /**
     * رندرة الإطار الحالي
     */
    private void render() {
        try {
            renderer.beginFrame();
            sceneManager.render(renderer);
            renderer.endFrame();
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الرندرة: " + e.getMessage());
        }
    }

    // Getters للمكونات
    public Renderer2D getRenderer() { return renderer; }
    public PhysicsEngine getPhysicsEngine() { return physicsEngine; }
    public AudioManager getAudioManager() { return audioManager; }
    public InputManager getInputManager() { return inputManager; }
    public SceneManager getSceneManager() { return sceneManager; }
    public RemiScriptInterpreter getScriptInterpreter() { return scriptInterpreter; }
    public Context getContext() { return context; }

    public float getDeltaTime() { return deltaTime; }
    public int getCurrentFPS() { return currentFPS; }
    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }

    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    public void setScreenSize(int w, int h) { screenWidth = w; screenHeight = h; }

    public String getGameName() { return gameName; }
    public void setGameName(String name) { gameName = name; }
    public String getGameVersion() { return gameVersion; }
    public void setGameVersion(String v) { gameVersion = v; }

    public void setListener(GameEngineListener listener) { this.listener = listener; }
}
