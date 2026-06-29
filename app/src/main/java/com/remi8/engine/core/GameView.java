package com.remi8.engine.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * سطح العرض الرسومي للعبة - REMI8
 * يتعامل مع رندرة Canvas ومدخلات اللمس
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private GameEngine gameEngine;
    private final Paint debugPaint;

    public GameView(Context context) {
        super(context);
        init();
        debugPaint = new Paint();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        debugPaint = new Paint();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (gameEngine != null) {
            gameEngine.getRenderer().setSurfaceHolder(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (gameEngine != null) {
            gameEngine.setScreenSize(width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (gameEngine != null) {
            gameEngine.getRenderer().setSurfaceHolder(null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameEngine != null) {
            gameEngine.getInputManager().processTouchEvent(event);
        }
        return true;
    }

    /**
     * رسم إطار اللعبة على Canvas مباشرة (للاختبار)
     */
    public void drawFrame(Canvas canvas) {
        if (canvas == null || gameEngine == null) return;

        // خلفية افتراضية
        canvas.drawColor(Color.parseColor("#1a1a2e"));

        // معلومات الديباغ
        debugPaint.setColor(Color.WHITE);
        debugPaint.setTextSize(30);
        debugPaint.setAntiAlias(true);
        canvas.drawText("REMI8 Engine v1.0", 20, 50, debugPaint);
        canvas.drawText("FPS: " + gameEngine.getCurrentFPS(), 20, 90, debugPaint);
    }
}
