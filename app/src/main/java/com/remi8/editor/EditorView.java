package com.remi8.editor;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.remi8.engine.core.GameEngine;
import com.remi8.engine.core.GameObject;
import com.remi8.engine.scene.Scene;

import java.util.List;

/**
 * واجهة العرض البصري للمحرر - REMI8
 * يتيح سحب الكائنات وتحديدها وتعديل خصائصها بصرياً
 */
public class EditorView extends View {

    private GameEngine gameEngine;
    private GameObject selectedObject;

    // رسم
    private final Paint objectPaint = new Paint();
    private final Paint selectedPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();

    // التنقل في المحرر
    private float viewOffsetX = 0, viewOffsetY = 0;
    private float viewZoom = 1.0f;

    // السحب
    private boolean isDragging = false;
    private float dragStartX, dragStartY;
    private float objStartX, objStartY;

    // السحب في الفضاء
    private boolean isPanning = false;
    private float panStartX, panStartY;
    private float panOffsetStartX, panOffsetStartY;

    // شبكة المحرر
    private static final float GRID_SIZE = 32f;
    private boolean showGrid = true;
    private boolean snapToGrid = false;

    // واجهة للاستدعاء عند تحديد كائن
    private OnObjectSelectedListener onObjectSelectedListener;

    public interface OnObjectSelectedListener {
        void onObjectSelected(GameObject obj);
        void onObjectMoved(GameObject obj, float newX, float newY);
    }

    public EditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditorView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        objectPaint.setAntiAlias(true);
        objectPaint.setStyle(Paint.Style.FILL);

        selectedPaint.setAntiAlias(true);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setColor(Color.YELLOW);
        selectedPaint.setStrokeWidth(3f);
        selectedPaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));

        gridPaint.setColor(Color.parseColor("#333355"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.5f);

        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        bgPaint.setColor(Color.parseColor("#1a1a2e"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null) return;

        // خلفية المحرر
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        canvas.save();
        canvas.translate(viewOffsetX, viewOffsetY);
        canvas.scale(viewZoom, viewZoom);

        // رسم الشبكة
        if (showGrid) drawGrid(canvas);

        // رسم حدود اللعبة
        drawGameBounds(canvas);

        // رسم الكائنات
        if (gameEngine != null && gameEngine.getSceneManager().getActiveScene() != null) {
            Scene scene = gameEngine.getSceneManager().getActiveScene();
            List<GameObject> objects = scene.getObjects();
            objects.sort((a, b) -> Integer.compare(a.getLayer() * 10000 + a.getZOrder(),
                                                     b.getLayer() * 10000 + b.getZOrder()));
            for (GameObject obj : objects) {
                if (obj.isVisible()) drawEditorObject(canvas, obj);
            }
        }

        canvas.restore();

        // معلومات الزوم والإزاحة
        textPaint.setTextSize(22f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(String.format("زوم: %.0f%%  إزاحة: (%.0f, %.0f)",
                viewZoom * 100, viewOffsetX, viewOffsetY), 16, 30, textPaint);

        postInvalidateDelayed(16);
    }

    /**
     * رسم شبكة المحرر
     */
    private void drawGrid(Canvas canvas) {
        float gridW = getWidth() / viewZoom + GRID_SIZE;
        float gridH = getHeight() / viewZoom + GRID_SIZE;
        float startX = (float) (Math.floor(-viewOffsetX / viewZoom / GRID_SIZE) * GRID_SIZE);
        float startY = (float) (Math.floor(-viewOffsetY / viewZoom / GRID_SIZE) * GRID_SIZE);

        for (float x = startX; x < startX + gridW; x += GRID_SIZE) {
            canvas.drawLine(x, startY, x, startY + gridH, gridPaint);
        }
        for (float y = startY; y < startY + gridH; y += GRID_SIZE) {
            canvas.drawLine(startX, y, startX + gridW, y, gridPaint);
        }
    }

    /**
     * رسم حدود شاشة اللعبة
     */
    private void drawGameBounds(Canvas canvas) {
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#556677"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        if (gameEngine != null) {
            canvas.drawRect(0, 0, gameEngine.getScreenWidth(), gameEngine.getScreenHeight(), borderPaint);
        }
    }

    /**
     * رسم كائن في المحرر
     */
    private void drawEditorObject(Canvas canvas, GameObject obj) {
        float left = obj.getGlobalX() - obj.width * obj.pivotX;
        float top = obj.getGlobalY() - obj.height * obj.pivotY;
        float right = left + obj.width;
        float bottom = top + obj.height;

        canvas.save();
        canvas.translate(obj.getGlobalX(), obj.getGlobalY());
        canvas.rotate(obj.rotation);
        canvas.translate(-obj.width * obj.pivotX, -obj.height * obj.pivotY);

        // رسم المستطيل
        Object colorProp = obj.getProperty("color");
        int color = Color.parseColor("#4a90d9");
        if (colorProp instanceof String) {
            try { color = Color.parseColor((String) colorProp); } catch (Exception ignored) {}
        } else if (colorProp instanceof Integer) {
            color = (Integer) colorProp;
        }
        objectPaint.setColor(color);
        objectPaint.setAlpha(obj.isActive() ? 230 : 100);
        canvas.drawRect(0, 0, obj.width, obj.height, objectPaint);

        // رسم النص
        Object textProp = obj.getProperty("text");
        if (textProp != null) {
            textPaint.setTextSize(Math.min(obj.height * 0.5f, 32f));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(textProp.toString(), obj.width / 2f, obj.height / 2f + 10, textPaint);
        }

        // رسم اسم الكائن
        Paint namePaint = new Paint();
        namePaint.setColor(Color.parseColor("#aaaacc"));
        namePaint.setTextSize(18f);
        namePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(obj.getName(), obj.width / 2f, -6, namePaint);

        canvas.restore();

        // مربع التحديد
        if (obj == selectedObject) {
            canvas.save();
            canvas.translate(obj.getGlobalX(), obj.getGlobalY());
            canvas.rotate(obj.rotation);
            canvas.translate(-obj.width * obj.pivotX, -obj.height * obj.pivotY);
            canvas.drawRect(-2, -2, obj.width + 2, obj.height + 2, selectedPaint);

            // نقاط التحكم
            drawHandle(canvas, 0, 0);
            drawHandle(canvas, obj.width, 0);
            drawHandle(canvas, 0, obj.height);
            drawHandle(canvas, obj.width, obj.height);
            drawHandle(canvas, obj.width / 2f, 0);
            drawHandle(canvas, obj.width / 2f, obj.height);
            canvas.restore();
        }
    }

    private void drawHandle(Canvas canvas, float x, float y) {
        Paint handle = new Paint();
        handle.setColor(Color.YELLOW);
        handle.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, 6, handle);
        handle.setColor(Color.BLACK);
        handle.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, 6, handle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float rawX = (event.getX() - viewOffsetX) / viewZoom;
        float rawY = (event.getY() - viewOffsetY) / viewZoom;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(rawX, rawY, event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(rawX, rawY, event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                isPanning = false;
                break;
        }
        return true;
    }

    private void handleTouchDown(float worldX, float worldY, float screenX, float screenY) {
        // البحث عن كائن في موضع اللمس
        GameObject hit = null;
        if (gameEngine != null && gameEngine.getSceneManager().getActiveScene() != null) {
            List<GameObject> objects = gameEngine.getSceneManager().getActiveScene().getObjects();
            for (int i = objects.size() - 1; i >= 0; i--) {
                GameObject obj = objects.get(i);
                if (obj.containsPoint(worldX, worldY)) {
                    hit = obj;
                    break;
                }
            }
        }

        if (hit != null) {
            selectedObject = hit;
            isDragging = true;
            dragStartX = worldX;
            dragStartY = worldY;
            objStartX = hit.x;
            objStartY = hit.y;
            if (onObjectSelectedListener != null) onObjectSelectedListener.onObjectSelected(hit);
        } else {
            selectedObject = null;
            isPanning = true;
            panStartX = screenX;
            panStartY = screenY;
            panOffsetStartX = viewOffsetX;
            panOffsetStartY = viewOffsetY;
        }
    }

    private void handleTouchMove(float worldX, float worldY, float screenX, float screenY) {
        if (isDragging && selectedObject != null) {
            float newX = objStartX + (worldX - dragStartX);
            float newY = objStartY + (worldY - dragStartY);
            if (snapToGrid) {
                newX = Math.round(newX / GRID_SIZE) * GRID_SIZE;
                newY = Math.round(newY / GRID_SIZE) * GRID_SIZE;
            }
            selectedObject.x = newX;
            selectedObject.y = newY;
            if (onObjectSelectedListener != null)
                onObjectSelectedListener.onObjectMoved(selectedObject, newX, newY);
        } else if (isPanning) {
            viewOffsetX = panOffsetStartX + (screenX - panStartX);
            viewOffsetY = panOffsetStartY + (screenY - panStartY);
        }
        invalidate();
    }

    public void setGameEngine(GameEngine engine) { this.gameEngine = engine; }
    public void setSelectedObject(GameObject obj) { this.selectedObject = obj; invalidate(); }
    public void setShowGrid(boolean show) { this.showGrid = show; invalidate(); }
    public void setSnapToGrid(boolean snap) { this.snapToGrid = snap; }
    public void setZoom(float zoom) { this.viewZoom = Math.max(0.1f, Math.min(5f, zoom)); invalidate(); }
    public void setOnObjectSelectedListener(OnObjectSelectedListener l) { onObjectSelectedListener = l; }
}
