package com.remi8.engine.renderer;

import com.remi8.engine.core.GameObject;

/**
 * كاميرا ثنائية الأبعاد متقدمة - REMI8
 * تدعم التتبع والاهتزاز والحدود
 */
public class Camera2D {

    // موضع الكاميرا
    private float x = 0, y = 0;
    private float zoom = 1.0f;
    private float rotation = 0f;

    // أبعاد الشاشة
    private float viewWidth, viewHeight;

    // التتبع
    private GameObject target;
    private float trackingSpeed = 5f;   // سرعة تتبع الهدف
    private float offsetX = 0, offsetY = 0; // إزاحة عن الهدف

    // حدود الكاميرا
    private float minX = Float.MIN_VALUE, maxX = Float.MAX_VALUE;
    private float minY = Float.MIN_VALUE, maxY = Float.MAX_VALUE;
    private boolean hasBounds = false;

    // اهتزاز الكاميرا
    private float shakeIntensity = 0;
    private float shakeTimer = 0;
    private float shakeDuration = 0;

    // ناعم (Lerp)
    private boolean smooth = true;

    public Camera2D(float viewW, float viewH) {
        this.viewWidth = viewW;
        this.viewHeight = viewH;
    }

    /**
     * تحديث الكاميرا كل إطار
     */
    public void update(float dt) {
        // تتبع الهدف
        if (target != null) {
            float targetX = target.getGlobalX() + offsetX - viewWidth / (2 * zoom);
            float targetY = target.getGlobalY() + offsetY - viewHeight / (2 * zoom);

            if (smooth) {
                x += (targetX - x) * trackingSpeed * dt;
                y += (targetY - y) * trackingSpeed * dt;
            } else {
                x = targetX;
                y = targetY;
            }
        }

        // تطبيق الحدود
        if (hasBounds) {
            float viewW = viewWidth / zoom;
            float viewH = viewHeight / zoom;
            x = Math.max(minX, Math.min(maxX - viewW, x));
            y = Math.max(minY, Math.min(maxY - viewH, y));
        }

        // اهتزاز الكاميرا
        if (shakeTimer > 0) {
            shakeTimer -= dt;
            if (shakeTimer <= 0) {
                shakeTimer = 0;
                shakeIntensity = 0;
            }
        }
    }

    /**
     * الحصول على موضع الكاميرا مع الاهتزاز
     */
    public float getDrawX() {
        if (shakeTimer > 0) {
            float shake = shakeIntensity * (shakeTimer / shakeDuration);
            return x + (float) ((Math.random() - 0.5) * 2 * shake);
        }
        return x;
    }

    public float getDrawY() {
        if (shakeTimer > 0) {
            float shake = shakeIntensity * (shakeTimer / shakeDuration);
            return y + (float) ((Math.random() - 0.5) * 2 * shake);
        }
        return y;
    }

    /**
     * تحويل إحداثيات العالم إلى إحداثيات الشاشة
     */
    public float worldToScreenX(float worldX) {
        return (worldX - getDrawX()) * zoom;
    }

    public float worldToScreenY(float worldY) {
        return (worldY - getDrawY()) * zoom;
    }

    /**
     * تحويل إحداثيات الشاشة إلى إحداثيات العالم
     */
    public float screenToWorldX(float screenX) {
        return screenX / zoom + getDrawX();
    }

    public float screenToWorldY(float screenY) {
        return screenY / zoom + getDrawY();
    }

    /**
     * هل الكائن مرئي في الكاميرا؟
     */
    public boolean isVisible(float objX, float objY, float objW, float objH) {
        float left   = getDrawX();
        float top    = getDrawY();
        float right  = left + viewWidth / zoom;
        float bottom = top  + viewHeight / zoom;
        return objX + objW > left && objX < right && objY + objH > top && objY < bottom;
    }

    /**
     * تشغيل اهتزاز الكاميرا
     */
    public void shake(float intensity, float duration) {
        shakeIntensity = intensity;
        shakeDuration  = duration;
        shakeTimer     = duration;
    }

    /**
     * تعيين حدود الكاميرا (حدود العالم)
     */
    public void setBounds(float x1, float y1, float x2, float y2) {
        minX = x1; minY = y1;
        maxX = x2; maxY = y2;
        hasBounds = true;
    }

    public void clearBounds() { hasBounds = false; }

    /**
     * تطبيق إعدادات الكاميرا على Renderer
     */
    public void applyToRenderer(Renderer2D renderer) {
        renderer.setCamera(getDrawX(), getDrawY());
        renderer.setCameraZoom(zoom);
    }

    // Getters & Setters
    public float getX() { return x; }
    public float getY() { return y; }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public float getZoom() { return zoom; }
    public void setZoom(float z) { zoom = Math.max(0.1f, z); }
    public void zoomIn(float amount)  { zoom = Math.min(5f, zoom + amount); }
    public void zoomOut(float amount) { zoom = Math.max(0.1f, zoom - amount); }
    public void setTarget(GameObject t) { target = t; }
    public void clearTarget() { target = null; }
    public void setOffset(float ox, float oy) { offsetX = ox; offsetY = oy; }
    public void setTrackingSpeed(float s) { trackingSpeed = s; }
    public void setSmooth(boolean s) { smooth = s; }
    public void setViewSize(float w, float h) { viewWidth = w; viewHeight = h; }
    public float getViewWidth() { return viewWidth; }
    public float getViewHeight() { return viewHeight; }
}
