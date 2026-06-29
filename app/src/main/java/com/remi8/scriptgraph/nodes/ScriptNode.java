package com.remi8.scriptgraph.nodes;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ScriptNode - نموذج بيانات عقدة Script Graph
 * يمثل كل عقدة مرئية في الـ Graph مع pins المدخلات والمخرجات
 */
public class ScriptNode {

    // ── أنواع Pin ────────────────────────────────────────────────────────

    public enum PinType { EXEC, FLOAT, INT, BOOL, STRING }

    public static class Pin {
        public String name;
        public PinType type;
        public boolean isInput;
        public float value = 0f;
        public String stringValue = "";

        // موقع Pin في الشاشة (يُحسب عند الرسم)
        public float screenX, screenY;

        // اتصال: (nodeId, pinName) أو null
        public String connectedNodeId = null;
        public String connectedPin    = null;

        public Pin(String name, PinType type, boolean isInput) {
            this.name    = name;
            this.type    = type;
            this.isInput = isInput;
        }
    }

    // ── بيانات العقدة ───────────────────────────────────────────────────

    public int    id;
    public int    nativeType;   // NODE_* constant
    public String label;
    public String category;

    // موقع وحجم في Canvas
    public float x, y, width, height;

    // Pins
    public List<Pin> inputs  = new ArrayList<>();
    public List<Pin> outputs = new ArrayList<>();

    // تحديد
    public boolean selected = false;

    // لون الـ header حسب Category
    public int headerColor;

    // ── ألوان الفئات ─────────────────────────────────────────────────────

    public static final int COLOR_EVENT  = 0xFF1A7A55;  // أخضر - Events
    public static final int COLOR_MATH   = 0xFF1B4F8A;  // أزرق - Math
    public static final int COLOR_VALUE  = 0xFF2D4A6A;  // أزرق داكن - Values
    public static final int COLOR_FLOW   = 0xFF6B3A2A;  // بني - Flow
    public static final int COLOR_DEBUG  = 0xFF4A2A6A;  // بنفسجي - Debug

    // ── باني ─────────────────────────────────────────────────────────────

    public ScriptNode(int id, int nativeType, String label, String category,
                      float x, float y) {
        this.id         = id;
        this.nativeType = nativeType;
        this.label      = label;
        this.category   = category;
        this.x          = x;
        this.y          = y;
        this.width      = 160f;
        this.height     = 100f;

        this.headerColor = colorForCategory(category);
        setupPins();
        recalcHeight();
    }

    private int colorForCategory(String cat) {
        switch (cat) {
            case "Event":  return COLOR_EVENT;
            case "Math":   return COLOR_MATH;
            case "Value":  return COLOR_VALUE;
            case "Flow":   return COLOR_FLOW;
            case "Debug":  return COLOR_DEBUG;
            default:       return COLOR_VALUE;
        }
    }

    /** إعداد Pins حسب نوع العقدة */
    private void setupPins() {
        switch (nativeType) {
            case 0: // NODE_EVENT_ON_UPDATE
            case 1: // NODE_EVENT_ON_START
                outputs.add(new Pin("exec",   PinType.EXEC,  false));
                outputs.add(new Pin("deltaTime", PinType.FLOAT, false));
                break;

            case 2: // NODE_MATH_ABS
                inputs.add (new Pin("exec",  PinType.EXEC,  true));
                inputs.add (new Pin("F",     PinType.FLOAT, true));
                outputs.add(new Pin("exec",  PinType.EXEC,  false));
                outputs.add(new Pin("O",     PinType.FLOAT, false));
                break;

            case 3: // NODE_MATH_MIN
            case 4: // NODE_MATH_MAX
                inputs.add (new Pin("exec",  PinType.EXEC,  true));
                inputs.add (new Pin("A",     PinType.FLOAT, true));
                inputs.add (new Pin("B",     PinType.FLOAT, true));
                outputs.add(new Pin("exec",  PinType.EXEC,  false));
                outputs.add(new Pin("O",     PinType.FLOAT, false));
                break;

            case 5: // MATH_SQRT
            case 6: // MATH_SIN
            case 7: // MATH_COS
                inputs.add (new Pin("exec",  PinType.EXEC,  true));
                inputs.add (new Pin("F",     PinType.FLOAT, true));
                outputs.add(new Pin("exec",  PinType.EXEC,  false));
                outputs.add(new Pin("O",     PinType.FLOAT, false));
                break;

            case 8: // NODE_FLOAT_VALUE
                outputs.add(new Pin("value", PinType.FLOAT, false));
                break;

            case 12: // OP_SUBTRACT
            case 13: // OP_ADD
            case 14: // OP_MULTIPLY
            case 15: // OP_DIVIDE
                inputs.add (new Pin("A",     PinType.FLOAT, true));
                inputs.add (new Pin("B",     PinType.FLOAT, true));
                outputs.add(new Pin("A-B",   PinType.FLOAT, false));
                break;

            case 21: // PRINT
                inputs.add (new Pin("exec",  PinType.EXEC,  true));
                inputs.add (new Pin("Value", PinType.FLOAT, true));
                outputs.add(new Pin("exec",  PinType.EXEC,  false));
                break;
        }
    }

    public void recalcHeight() {
        int maxPins = Math.max(inputs.size(), outputs.size());
        height = 30f /* header */ + 10f /* padding */ + Math.max(maxPins, 1) * 24f + 10f;
    }

    public RectF getBounds() {
        return new RectF(x, y, x + width, y + height);
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    /** إيجاد Pin بالاسم */
    public Pin findPin(String name, boolean input) {
        List<Pin> list = input ? inputs : outputs;
        for (Pin p : list) if (p.name.equals(name)) return p;
        return null;
    }

    /** قيمة عرض الـ Float pin */
    public String getDisplayValue() {
        for (Pin p : outputs) {
            if (p.type == PinType.FLOAT) {
                float v = p.value;
                if (v == Math.floor(v)) return String.valueOf((int)v);
                return String.format("%.2f", v);
            }
        }
        return "";
    }
}
