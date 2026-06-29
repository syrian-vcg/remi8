package com.remi8.scriptgraph;

/**
 * ─────────────────────────────────────────────────────
 *  ScriptGraphEngine - JNI Bridge
 *  جسر Java ↔ C++ لمحرك Script Graph
 *
 *  يربط واجهة Java/Android مع محرك C++ عالي الأداء
 * ─────────────────────────────────────────────────────
 */
public class ScriptGraphEngine {

    // تحميل المكتبة الأصلية
    static {
        System.loadLibrary("remi8_engine");
    }

    // ── أنواع العقد (تطابق NodeType في C++) ───────────────────────────────

    public static final int NODE_EVENT_ON_UPDATE  = 0;
    public static final int NODE_EVENT_ON_START   = 1;
    public static final int NODE_MATH_ABS         = 2;
    public static final int NODE_MATH_MIN         = 3;
    public static final int NODE_MATH_MAX         = 4;
    public static final int NODE_MATH_SQRT        = 5;
    public static final int NODE_MATH_SIN         = 6;
    public static final int NODE_MATH_COS         = 7;
    public static final int NODE_FLOAT_VALUE      = 8;
    public static final int NODE_INT_VALUE        = 9;
    public static final int NODE_BOOL_VALUE       = 10;
    public static final int NODE_STRING_VALUE     = 11;
    public static final int NODE_OP_SUBTRACT      = 12;
    public static final int NODE_OP_ADD           = 13;
    public static final int NODE_OP_MULTIPLY      = 14;
    public static final int NODE_OP_DIVIDE        = 15;
    public static final int NODE_OP_COMPARE_GT    = 16;
    public static final int NODE_OP_COMPARE_LT    = 17;
    public static final int NODE_BRANCH           = 18;
    public static final int NODE_SET_VARIABLE     = 19;
    public static final int NODE_GET_VARIABLE     = 20;
    public static final int NODE_PRINT            = 21;
    public static final int NODE_CUSTOM           = 22;

    // ── JNI Native Methods ────────────────────────────────────────────────

    /** تهيئة المحرك */
    public static native void nativeInit();

    /** إضافة عقدة جديدة → يُعيد ID */
    public static native int nativeAddNode(int type, float x, float y);

    /** ربط مخرج عقدة بمدخل عقدة أخرى */
    public static native boolean nativeConnect(int fromId, String fromPin, int toId, String toPin);

    /** تعيين قيمة float لـ pin */
    public static native void nativeSetFloat(int nodeId, String pin, float value);

    /** تقييم قيمة مخرج عقدة */
    public static native float nativeEvaluate(int nodeId, String outputPin);

    /** تنفيذ حدث (OnUpdate / OnStart) → يُعيد log */
    public static native String nativeExecuteEvent(String eventName);

    /** تصدير Graph كـ remiscript */
    public static native String nativeExportRemiScript();

    /** إعادة تعيين Graph */
    public static native void nativeClear();

    /** الحصول على Graph كـ JSON */
    public static native String nativeGetGraphJson();

    // ── Helper Methods ────────────────────────────────────────────────────

    /**
     * بناء المثال من الصورة:
     *
     *  OnUpdate → Mathf.Abs(Subtract(Float5, Float3)) = 2
     *  OnUpdate → Mathf.Min(Float5, Float1) = 1
     */
    public static void buildExampleFromImage() {
        nativeClear();

        // Graph 1: OnUpdate → Subtract(5,3) → Abs
        int ev1    = nativeAddNode(NODE_EVENT_ON_UPDATE, 50,  80);
        int float5a = nativeAddNode(NODE_FLOAT_VALUE,    80, 220);
        int float3  = nativeAddNode(NODE_FLOAT_VALUE,   80, 300);
        int sub    = nativeAddNode(NODE_OP_SUBTRACT,   300, 230);
        int abs    = nativeAddNode(NODE_MATH_ABS,      540,  80);

        nativeSetFloat(float5a, "value", 5.0f);
        nativeSetFloat(float3,  "value", 3.0f);

        nativeConnect(float5a, "value", sub, "A");
        nativeConnect(float3,  "value", sub, "B");
        nativeConnect(ev1,     "exec",  abs, "exec");
        nativeConnect(sub,     "result", abs, "F");

        // Graph 2: OnUpdate → Min(Float5, Float1)
        int ev2    = nativeAddNode(NODE_EVENT_ON_UPDATE, 450, 330);
        int float5b = nativeAddNode(NODE_FLOAT_VALUE,   80, 450);
        int float1  = nativeAddNode(NODE_FLOAT_VALUE,  450, 510);
        int min    = nativeAddNode(NODE_MATH_MIN,      640, 330);

        nativeSetFloat(float5b, "value", 5.0f);
        nativeSetFloat(float1,  "value", 1.0f);

        nativeConnect(ev2,     "exec",  min, "exec");
        nativeConnect(float5b, "value", min, "A");
        nativeConnect(float1,  "value", min, "B");
    }
}
