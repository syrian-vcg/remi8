package com.remi8.editor;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.remi8.engine.input.InputManager;

/**
 * لوحة التحكم الافتراضية - REMI8
 * تُعرض أثناء تشغيل اللعبة على الشاشة
 */
public class VirtualGamepad extends View {

    private InputManager inputManager;

    // ألوان الأزرار
    private final Paint btnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint btnActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint joystickBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint joystickKnobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // أبعاد الشاشة
    private int screenW, screenH;

    // جويستيك يسار
    private float joyBaseX, joyBaseY;
    private float joyKnobX, joyKnobY;
    private static final float JOY_RADIUS = 80f;
    private static final float KNOB_RADIUS = 35f;
    private int joyPointerId = -1;

    // أزرار يمين (أ، ب، قفز)
    private RectF btnJump, btnA, btnB;
    private boolean pressJump, pressA, pressB;

    // زر الإيقاف المؤقت
    private RectF btnPause;

    public VirtualGamepad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VirtualGamepad(Context context) {
        super(context);
        init();
    }

    private void init() {
        btnPaint.setColor(Color.argb(140, 255, 255, 255));
        btnPaint.setStyle(Paint.Style.FILL);

        btnActivePaint.setColor(Color.argb(200, 230, 57, 70));
        btnActivePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        joystickBgPaint.setColor(Color.argb(100, 100, 100, 150));
        joystickBgPaint.setStyle(Paint.Style.FILL);

        joystickKnobPaint.setColor(Color.argb(200, 230, 57, 70));
        joystickKnobPaint.setStyle(Paint.Style.FILL);

        setWillNotDraw(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        screenW = w;
        screenH = h;
        setupLayout();
    }

    private void setupLayout() {
        // جويستيك يسار - ركن أسفل يسار
        joyBaseX = 150f;
        joyBaseY = screenH - 160f;
        joyKnobX = joyBaseX;
        joyKnobY = joyBaseY;

        // أزرار يمين - ركن أسفل يمين
        float btnRight = screenW - 60f;
        float btnBottom = screenH - 80f;
        float btnSize = 60f;

        btnJump = new RectF(btnRight - btnSize / 2, btnBottom - btnSize - 70 - btnSize / 2,
                             btnRight + btnSize / 2, btnBottom - 70 + btnSize / 2);
        btnA = new RectF(btnRight - btnSize - 70 - btnSize / 2, btnBottom - btnSize / 2,
                          btnRight - 70 + btnSize / 2, btnBottom + btnSize / 2);
        btnB = new RectF(btnRight - btnSize / 2, btnBottom - btnSize / 2,
                          btnRight + btnSize / 2, btnBottom + btnSize / 2);

        // زر إيقاف مؤقت - أعلى يمين
        btnPause = new RectF(screenW - 80, 20, screenW - 20, 70);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (screenW == 0) return;

        // ─── رسم الجويستيك ───
        canvas.drawCircle(joyBaseX, joyBaseY, JOY_RADIUS, joystickBgPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.argb(80, 255, 255, 255));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        canvas.drawCircle(joyBaseX, joyBaseY, JOY_RADIUS, borderPaint);

        canvas.drawCircle(joyKnobX, joyKnobY, KNOB_RADIUS, joystickKnobPaint);

        // ─── رسم أزرار القفز والتحكم ───
        if (btnJump != null) {
            canvas.drawOval(btnJump, pressJump ? btnActivePaint : btnPaint);
            canvas.drawText("↑", btnJump.centerX(), btnJump.centerY() + 10, textPaint);

            canvas.drawOval(btnA, pressA ? btnActivePaint : btnPaint);
            canvas.drawText("أ", btnA.centerX(), btnA.centerY() + 10, textPaint);

            canvas.drawOval(btnB, pressB ? btnActivePaint : btnPaint);
            canvas.drawText("ب", btnB.centerX(), btnB.centerY() + 10, textPaint);
        }

        // ─── زر الإيقاف المؤقت ───
        if (btnPause != null) {
            Paint pausePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pausePaint.setColor(Color.argb(120, 50, 50, 80));
            pausePaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(btnPause, 8, 8, pausePaint);
            textPaint.setTextSize(22f);
            canvas.drawText("⏸", btnPause.centerX(), btnPause.centerY() + 8, textPaint);
            textPaint.setTextSize(28f);
        }

        postInvalidateDelayed(16);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handleDown(pointerId, x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    handleMove(event.getPointerId(i), event.getX(i), event.getY(i));
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handleUp(pointerId, x, y);
                break;
        }
        invalidate();
        return true;
    }

    private void handleDown(int id, float x, float y) {
        // هل اللمس على الجويستيك؟
        float dist = (float) Math.sqrt(Math.pow(x - joyBaseX, 2) + Math.pow(y - joyBaseY, 2));
        if (dist < JOY_RADIUS + 30) {
            joyPointerId = id;
            moveJoystick(x, y);
            return;
        }

        // الأزرار
        if (btnJump != null && btnJump.contains(x, y)) {
            pressJump = true;
            if (inputManager != null) inputManager.setButton("قفز", true);
        }
        if (btnA != null && btnA.contains(x, y)) {
            pressA = true;
            if (inputManager != null) inputManager.setButton("أ", true);
        }
        if (btnB != null && btnB.contains(x, y)) {
            pressB = true;
            if (inputManager != null) inputManager.setButton("ب", true);
        }
    }

    private void handleMove(int id, float x, float y) {
        if (id == joyPointerId) {
            moveJoystick(x, y);
        }
    }

    private void handleUp(int id, float x, float y) {
        if (id == joyPointerId) {
            joyPointerId = -1;
            joyKnobX = joyBaseX;
            joyKnobY = joyBaseY;
            if (inputManager != null) {
                inputManager.setButton("يسار", false);
                inputManager.setButton("يمين", false);
                inputManager.setButton("أعلى", false);
                inputManager.setButton("أسفل", false);
            }
        }

        if (btnJump != null && btnJump.contains(x, y)) {
            pressJump = false;
            if (inputManager != null) inputManager.setButton("قفز", false);
        }
        if (btnA != null && btnA.contains(x, y)) {
            pressA = false;
            if (inputManager != null) inputManager.setButton("أ", false);
        }
        if (btnB != null && btnB.contains(x, y)) {
            pressB = false;
            if (inputManager != null) inputManager.setButton("ب", false);
        }
    }

    private void moveJoystick(float x, float y) {
        float dx = x - joyBaseX;
        float dy = y - joyBaseY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > JOY_RADIUS) {
            dx = dx / dist * JOY_RADIUS;
            dy = dy / dist * JOY_RADIUS;
        }

        joyKnobX = joyBaseX + dx;
        joyKnobY = joyBaseY + dy;

        float normX = dx / JOY_RADIUS;
        float normY = dy / JOY_RADIUS;
        float deadZone = 0.2f;

        if (inputManager != null) {
            inputManager.setButton("يمين", normX > deadZone);
            inputManager.setButton("يسار", normX < -deadZone);
            inputManager.setButton("أسفل", normY > deadZone);
            inputManager.setButton("أعلى", normY < -deadZone);
        }
    }

    public void setInputManager(InputManager inputManager) {
        this.inputManager = inputManager;
    }
}
