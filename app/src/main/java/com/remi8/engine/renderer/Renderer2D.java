package com.remi8.engine.renderer;

import android.content.Context;
import android.graphics.*;
import android.view.SurfaceHolder;

import com.remi8.engine.core.GameObject;

import java.util.HashMap;
import java.util.Map;

/**
 * محرك الرندرة ثنائي الأبعاد - REMI8
 * يرسم جميع عناصر اللعبة على Canvas
 */
public class Renderer2D {

    private final Context context;
    private SurfaceHolder surfaceHolder;
    private Canvas currentCanvas;

    // الكاميرا
    private float cameraX = 0, cameraY = 0;
    private float cameraZoom = 1.0f;

    // الفرش الجاهزة
    private final Paint defaultPaint;
    private final Paint textPaint;
    private final Paint debugPaint;

    // ذاكرة تخزين الصور المؤقتة
    private final Map<String, Bitmap> textureCache = new HashMap<>();

    // إعدادات الرندرة
    private boolean showDebug = false;
    private boolean antiAlias = true;

    // مصفوفة التحويل
    private final Matrix transformMatrix = new Matrix();

    public Renderer2D(Context context) {
        this.context = context;

        defaultPaint = new Paint();
        defaultPaint.setAntiAlias(true);
        defaultPaint.setFilterBitmap(true);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32);

        debugPaint = new Paint();
        debugPaint.setAntiAlias(true);
        debugPaint.setColor(Color.GREEN);
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setStrokeWidth(2);
    }

    /**
     * بدء إطار الرندرة
     */
    public Canvas beginFrame() {
        if (surfaceHolder == null) return null;
        try {
            currentCanvas = surfaceHolder.lockCanvas();
            if (currentCanvas != null) {
                currentCanvas.drawColor(Color.parseColor("#1a1a2e")); // لون الخلفية
            }
        } catch (Exception e) {
            currentCanvas = null;
        }
        return currentCanvas;
    }

    /**
     * إنهاء إطار الرندرة
     */
    public void endFrame() {
        if (surfaceHolder != null && currentCanvas != null) {
            try {
                surfaceHolder.unlockCanvasAndPost(currentCanvas);
            } catch (Exception e) {
                // تجاهل أخطاء إغلاق السطح
            }
            currentCanvas = null;
        }
    }

    /**
     * رسم كائن لعبة
     */
    public void drawGameObject(GameObject obj) {
        if (currentCanvas == null || !obj.isVisible()) return;

        currentCanvas.save();

        // تطبيق تحويل الكاميرا
        float screenX = (obj.getGlobalX() - cameraX) * cameraZoom;
        float screenY = (obj.getGlobalY() - cameraY) * cameraZoom;

        currentCanvas.translate(screenX, screenY);
        currentCanvas.rotate(obj.rotation);
        currentCanvas.scale(obj.scaleX * cameraZoom, obj.scaleY * cameraZoom);
        currentCanvas.translate(-obj.width * obj.pivotX, -obj.height * obj.pivotY);

        // رسم الصورة إن وجدت
        String textureName = (String) obj.getProperty("texture");
        if (textureName != null && textureCache.containsKey(textureName)) {
            Bitmap bitmap = textureCache.get(textureName);
            RectF destRect = new RectF(0, 0, obj.width, obj.height);
            currentCanvas.drawBitmap(bitmap, null, destRect, defaultPaint);
        } else {
            // رسم مستطيل بلون افتراضي
            Object colorProp = obj.getProperty("color");
            int color = Color.WHITE;
            if (colorProp instanceof Integer) color = (Integer) colorProp;
            else if (colorProp instanceof String) {
                try { color = Color.parseColor((String) colorProp); } catch (Exception ignored) {}
            }
            defaultPaint.setColor(color);
            defaultPaint.setStyle(Paint.Style.FILL);
            currentCanvas.drawRect(0, 0, obj.width, obj.height, defaultPaint);
        }

        // رسم النص إن وجد
        String text = (String) obj.getProperty("text");
        if (text != null) {
            Object textSizeProp = obj.getProperty("textSize");
            float fontSize = textSizeProp instanceof Number ? ((Number) textSizeProp).floatValue() : 32f;
            Object textColorProp = obj.getProperty("textColor");
            int textColor = Color.WHITE;
            if (textColorProp instanceof String) {
                try { textColor = Color.parseColor((String) textColorProp); } catch (Exception ignored) {}
            }
            textPaint.setTextSize(fontSize);
            textPaint.setColor(textColor);
            currentCanvas.drawText(text, obj.width / 2f - textPaint.measureText(text) / 2f, obj.height / 2f, textPaint);
        }

        // رسم حدود الديباغ
        if (showDebug) {
            currentCanvas.drawRect(0, 0, obj.width, obj.height, debugPaint);
        }

        currentCanvas.restore();
    }

    /**
     * رسم نص مباشرة على الشاشة
     */
    public void drawText(String text, float x, float y, int color, float size) {
        if (currentCanvas == null) return;
        textPaint.setColor(color);
        textPaint.setTextSize(size);
        currentCanvas.drawText(text, x, y, textPaint);
    }

    /**
     * رسم مستطيل
     */
    public void drawRect(float x, float y, float w, float h, int color, boolean filled) {
        if (currentCanvas == null) return;
        defaultPaint.setColor(color);
        defaultPaint.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE);
        currentCanvas.drawRect(x - cameraX * cameraZoom, y - cameraY * cameraZoom,
                               x - cameraX * cameraZoom + w * cameraZoom,
                               y - cameraY * cameraZoom + h * cameraZoom, defaultPaint);
    }

    /**
     * رسم دائرة
     */
    public void drawCircle(float cx, float cy, float radius, int color, boolean filled) {
        if (currentCanvas == null) return;
        defaultPaint.setColor(color);
        defaultPaint.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE);
        currentCanvas.drawCircle((cx - cameraX) * cameraZoom, (cy - cameraY) * cameraZoom,
                                  radius * cameraZoom, defaultPaint);
    }

    /**
     * رسم خط
     */
    public void drawLine(float x1, float y1, float x2, float y2, int color, float strokeWidth) {
        if (currentCanvas == null) return;
        defaultPaint.setColor(color);
        defaultPaint.setStyle(Paint.Style.STROKE);
        defaultPaint.setStrokeWidth(strokeWidth);
        currentCanvas.drawLine((x1 - cameraX) * cameraZoom, (y1 - cameraY) * cameraZoom,
                               (x2 - cameraX) * cameraZoom, (y2 - cameraY) * cameraZoom, defaultPaint);
    }

    /**
     * ملء الشاشة بلون
     */
    public void clearScreen(int color) {
        if (currentCanvas != null) currentCanvas.drawColor(color);
    }

    /**
     * تحميل صورة في الذاكرة المؤقتة
     */
    public void loadTexture(String name, Bitmap bitmap) {
        if (textureCache.containsKey(name)) {
            Bitmap old = textureCache.get(name);
            if (old != null && !old.isRecycled()) old.recycle();
        }
        textureCache.put(name, bitmap);
    }

    public void unloadTexture(String name) {
        Bitmap bmp = textureCache.remove(name);
        if (bmp != null && !bmp.isRecycled()) bmp.recycle();
    }

    public boolean hasTexture(String name) {
        return textureCache.containsKey(name);
    }

    // إدارة الكاميرا
    public void setCamera(float x, float y) { cameraX = x; cameraY = y; }
    public void setCameraZoom(float zoom) { cameraZoom = Math.max(0.1f, zoom); }
    public float getCameraX() { return cameraX; }
    public float getCameraY() { return cameraY; }
    public float getCameraZoom() { return cameraZoom; }

    public void setSurfaceHolder(SurfaceHolder holder) { this.surfaceHolder = holder; }
    public Canvas getCurrentCanvas() { return currentCanvas; }
    public void setShowDebug(boolean show) { this.showDebug = show; }
    public boolean isShowDebug() { return showDebug; }
}
