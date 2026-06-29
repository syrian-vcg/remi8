package com.remi8.engine.input;

import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * مدير المدخلات - REMI8
 * يدير اللمس، الأزرار الافتراضية، والإيماءات
 */
public class InputManager {

    // حالة اللمس
    private final Map<Integer, TouchPoint> touchPoints = new HashMap<>();
    private final List<TouchEvent> pendingEvents = new ArrayList<>();

    // الأزرار الافتراضية
    private boolean buttonLeft = false;
    private boolean buttonRight = false;
    private boolean buttonUp = false;
    private boolean buttonDown = false;
    private boolean buttonA = false;
    private boolean buttonB = false;
    private boolean buttonJump = false;

    // إيماءة التمرير
    private float swipeStartX, swipeStartY;
    private boolean isSwiping = false;
    private String lastSwipeDirection = "";

    // حساسية العصا الافتراضية
    private float joystickX = 0;
    private float joystickY = 0;

    // الزناد المرفقة
    private final List<InputListener> listeners = new ArrayList<>();

    public interface InputListener {
        void onTouch(float x, float y, int pointerId);
        void onRelease(float x, float y, int pointerId);
        void onSwipe(String direction);
    }

    public InputManager() {}

    /**
     * معالجة حدث لمس Android
     */
    public void processTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        synchronized (pendingEvents) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    touchPoints.put(pointerId, new TouchPoint(x, y, true));
                    pendingEvents.add(new TouchEvent(TouchEvent.Type.DOWN, x, y, pointerId));
                    swipeStartX = x;
                    swipeStartY = y;
                    isSwiping = true;

                    // إشعار المستمعين
                    for (InputListener l : listeners) l.onTouch(x, y, pointerId);
                    break;

                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        int id = event.getPointerId(i);
                        TouchPoint tp = touchPoints.get(id);
                        if (tp != null) {
                            tp.x = event.getX(i);
                            tp.y = event.getY(i);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    touchPoints.remove(pointerId);
                    pendingEvents.add(new TouchEvent(TouchEvent.Type.UP, x, y, pointerId));

                    // فحص الإيماءة
                    if (isSwiping) {
                        detectSwipe(swipeStartX, swipeStartY, x, y);
                        isSwiping = false;
                    }

                    for (InputListener l : listeners) l.onRelease(x, y, pointerId);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    touchPoints.clear();
                    isSwiping = false;
                    break;
            }
        }
    }

    /**
     * تحديث في كل إطار
     */
    public void update() {
        synchronized (pendingEvents) {
            pendingEvents.clear();
        }
        // إعادة تعيين الجويستيك إذا لا يوجد لمس
        if (touchPoints.isEmpty()) {
            joystickX = 0;
            joystickY = 0;
        }
    }

    /**
     * كشف اتجاه الإيماءة
     */
    private void detectSwipe(float startX, float startY, float endX, float endY) {
        float dx = endX - startX;
        float dy = endY - startY;
        float minDistance = 50;

        if (Math.abs(dx) < minDistance && Math.abs(dy) < minDistance) return;

        if (Math.abs(dx) > Math.abs(dy)) {
            lastSwipeDirection = dx > 0 ? "يمين" : "يسار";
        } else {
            lastSwipeDirection = dy > 0 ? "أسفل" : "أعلى";
        }

        for (InputListener l : listeners) l.onSwipe(lastSwipeDirection);
    }

    /**
     * هل يتم اللمس في منطقة معينة؟
     */
    public boolean isTouchingArea(float x, float y, float width, float height) {
        for (TouchPoint tp : touchPoints.values()) {
            if (tp.x >= x && tp.x <= x + width && tp.y >= y && tp.y <= y + height) {
                return true;
            }
        }
        return false;
    }

    /**
     * الحصول على أول نقطة لمس
     */
    public TouchPoint getFirstTouch() {
        if (touchPoints.isEmpty()) return null;
        return touchPoints.values().iterator().next();
    }

    /**
     * عدد نقاط اللمس الحالية
     */
    public int getTouchCount() {
        return touchPoints.size();
    }

    // إدارة الأزرار الافتراضية
    public void setButton(String name, boolean pressed) {
        switch (name) {
            case "يسار": buttonLeft = pressed; joystickX = pressed ? -1 : 0; break;
            case "يمين": buttonRight = pressed; joystickX = pressed ? 1 : 0; break;
            case "أعلى": buttonUp = pressed; joystickY = pressed ? -1 : 0; break;
            case "أسفل": buttonDown = pressed; joystickY = pressed ? 1 : 0; break;
            case "أ": buttonA = pressed; break;
            case "ب": buttonB = pressed; break;
            case "قفز": buttonJump = pressed; break;
        }
    }

    public boolean isButtonPressed(String name) {
        switch (name) {
            case "يسار": return buttonLeft;
            case "يمين": return buttonRight;
            case "أعلى": return buttonUp;
            case "أسفل": return buttonDown;
            case "أ": return buttonA;
            case "ب": return buttonB;
            case "قفز": return buttonJump;
            default: return false;
        }
    }

    public float getJoystickX() { return joystickX; }
    public float getJoystickY() { return joystickY; }
    public String getLastSwipeDirection() { return lastSwipeDirection; }
    public Map<Integer, TouchPoint> getTouchPoints() { return touchPoints; }

    public void addListener(InputListener listener) { listeners.add(listener); }
    public void removeListener(InputListener listener) { listeners.remove(listener); }

    /**
     * نقطة لمس
     */
    public static class TouchPoint {
        public float x, y;
        public boolean active;
        public TouchPoint(float x, float y, boolean active) {
            this.x = x; this.y = y; this.active = active;
        }
    }

    /**
     * حدث لمس
     */
    public static class TouchEvent {
        public enum Type { DOWN, UP, MOVE }
        public final Type type;
        public final float x, y;
        public final int pointerId;
        public TouchEvent(Type type, float x, float y, int pointerId) {
            this.type = type; this.x = x; this.y = y; this.pointerId = pointerId;
        }
    }
}
