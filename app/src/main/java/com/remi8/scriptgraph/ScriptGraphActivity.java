package com.remi8.scriptgraph;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.remi8.R;
import com.remi8.scriptgraph.nodes.ScriptNode;
import com.remi8.scriptgraph.nodes.ScriptNode.Pin;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ─────────────────────────────────────────────────────────────────
 *  ScriptGraphActivity
 *  الشاشة الرئيسية لـ Script Graph المرئي
 *
 *  تجمع بين:
 *  • ScriptGraphView  (Canvas رسم العقد)
 *  • ScriptGraphEngine (C++ JNI محرك التنفيذ)
 *  • NodePalette      (لوحة إضافة العقد)
 *  • شريط أدوات (تشغيل / تصدير / مسح)
 * ─────────────────────────────────────────────────────────────────
 */
public class ScriptGraphActivity extends Activity
        implements ScriptGraphView.GraphListener {

    // ── Views ─────────────────────────────────────────────────────────────

    private ScriptGraphView graphView;
    private TextView        tvStatus;
    private TextView        tvConsole;
    private ScrollView      scrollConsole;
    private LinearLayout    palettePanel;
    private boolean         paletteVisible = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تهيئة C++ Engine
        ScriptGraphEngine.nativeInit();

        setContentView(R.layout.activity_script_graph);
        bindViews();
        buildToolbar();
        buildPalette();
        loadExampleGraph();
    }

    // ── Init ──────────────────────────────────────────────────────────────

    private void bindViews() {
        graphView     = findViewById(R.id.graph_view);
        tvStatus      = findViewById(R.id.tv_status);
        tvConsole     = findViewById(R.id.tv_console);
        scrollConsole = findViewById(R.id.scroll_console);
        palettePanel  = findViewById(R.id.palette_panel);

        graphView.setGraphListener(this);
    }

    private void buildToolbar() {
        // زر تشغيل
        findViewById(R.id.btn_run).setOnClickListener(v -> runGraph());
        // زر تصدير remiscript
        findViewById(R.id.btn_export).setOnClickListener(v -> exportRemiScript());
        // زر إضافة عقدة
        findViewById(R.id.btn_add_node).setOnClickListener(v -> togglePalette());
        // زر مسح
        findViewById(R.id.btn_clear).setOnClickListener(v -> clearGraph());
        // زر توسيط
        findViewById(R.id.btn_center).setOnClickListener(v -> graphView.centerView());
        // زر المثال
        findViewById(R.id.btn_example).setOnClickListener(v -> loadExampleGraph());
    }

    private void buildPalette() {
        // تعريف الفئات والعقد
        String[][] palette = {
            // {label, category, type_id}
            {"On Update",  "Event", String.valueOf(ScriptGraphEngine.NODE_EVENT_ON_UPDATE)},
            {"On Start",   "Event", String.valueOf(ScriptGraphEngine.NODE_EVENT_ON_START)},
            {"Mathf Abs",  "Math",  String.valueOf(ScriptGraphEngine.NODE_MATH_ABS)},
            {"Mathf Min",  "Math",  String.valueOf(ScriptGraphEngine.NODE_MATH_MIN)},
            {"Mathf Max",  "Math",  String.valueOf(ScriptGraphEngine.NODE_MATH_MAX)},
            {"Mathf Sqrt", "Math",  String.valueOf(ScriptGraphEngine.NODE_MATH_SQRT)},
            {"Mathf Sin",  "Math",  String.valueOf(ScriptGraphEngine.NODE_MATH_SIN)},
            {"Mathf Cos",  "Math",  String.valueOf(ScriptGraphEngine.NODE_MATH_COS)},
            {"Subtract",   "Math",  String.valueOf(ScriptGraphEngine.NODE_OP_SUBTRACT)},
            {"Add",        "Math",  String.valueOf(ScriptGraphEngine.NODE_OP_ADD)},
            {"Multiply",   "Math",  String.valueOf(ScriptGraphEngine.NODE_OP_MULTIPLY)},
            {"Divide",     "Math",  String.valueOf(ScriptGraphEngine.NODE_OP_DIVIDE)},
            {"Float",      "Value", String.valueOf(ScriptGraphEngine.NODE_FLOAT_VALUE)},
            {"Print",      "Debug", String.valueOf(ScriptGraphEngine.NODE_PRINT)},
        };

        String lastCat = "";
        for (String[] entry : palette) {
            String lbl = entry[0], cat = entry[1];
            int type = Integer.parseInt(entry[2]);

            // فاصل الفئة
            if (!cat.equals(lastCat)) {
                TextView catLabel = new TextView(this);
                catLabel.setText("── " + cat + " ──");
                catLabel.setTextColor(0xFF5C6370);
                catLabel.setTextSize(11f);
                catLabel.setPadding(12, 8, 12, 2);
                palettePanel.addView(catLabel);
                lastCat = cat;
            }

            Button btn = new Button(this);
            btn.setText(iconForCat(cat) + "  " + lbl);
            btn.setTextColor(0xFFABB2BF);
            btn.setTextSize(13f);
            btn.setBackgroundColor(0xFF23272E);
            btn.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 44);
            lp.setMargins(8, 2, 8, 2);
            btn.setLayoutParams(lp);

            final int finalType = type;
            final String finalLabel = lbl;
            final String finalCat   = cat;

            btn.setOnClickListener(v -> {
                // إضافة عقدة في وسط الشاشة
                float cx = (graphView.getWidth()  / 2f - graphView.getPanX()) / graphView.getScaleF();
                float cy = (graphView.getHeight() / 2f - graphView.getPanY()) / graphView.getScaleF();
                addNodeToGraph(finalType, finalLabel, finalCat, cx, cy);
                togglePalette();
            });

            palettePanel.addView(btn);
        }
    }

    // ── Graph Operations ──────────────────────────────────────────────────

    /**
     * بناء مثال الصورة:
     *  Graph1: OnUpdate → Subtract(5,3) → Abs  → نتيجة 2
     *  Graph2: OnUpdate → Min(5,1)             → نتيجة 1
     */
    private void loadExampleGraph() {
        graphView.clearAll();
        ScriptGraphEngine.nativeInit();
        ScriptGraphEngine.buildExampleFromImage();

        // ── Graph 1: Abs(Subtract(5,3)) ──

        // OnUpdate
        ScriptNode ev1 = addNodeToGraph(
                ScriptGraphEngine.NODE_EVENT_ON_UPDATE, "On Update", "Event", 60, 80);
        // Float 5
        ScriptNode f5a = addNodeToGraph(
                ScriptGraphEngine.NODE_FLOAT_VALUE, "Float", "Value", 60, 220);
        f5a.getOutputs().get(0).value = 5f;
        syncFloatDisplay(f5a, 5f);

        // Float 3
        ScriptNode f3 = addNodeToGraph(
                ScriptGraphEngine.NODE_FLOAT_VALUE, "Float", "Value", 60, 310);
        f3.getOutputs().get(0).value = 3f;
        syncFloatDisplay(f3, 3f);

        // Subtract
        ScriptNode sub = addNodeToGraph(
                ScriptGraphEngine.NODE_OP_SUBTRACT, "Subtract", "Math", 290, 240);

        // Abs
        ScriptNode abs = addNodeToGraph(
                ScriptGraphEngine.NODE_MATH_ABS, "Mathf Abs", "Math", 530, 80);

        // ربط
        connectNodes(ev1, "exec",   abs, "exec");
        connectNodes(f5a, "value",  sub, "A");
        connectNodes(f3,  "value",  sub, "B");
        connectNodes(sub, "A-B",    abs, "F");

        // ── Graph 2: Min(5,1) ──

        ScriptNode ev2 = addNodeToGraph(
                ScriptGraphEngine.NODE_EVENT_ON_UPDATE, "On Update", "Event", 450, 350);

        ScriptNode f5b = addNodeToGraph(
                ScriptGraphEngine.NODE_FLOAT_VALUE, "Float", "Value", 60, 460);
        f5b.getOutputs().get(0).value = 5f;
        syncFloatDisplay(f5b, 5f);

        ScriptNode f1 = addNodeToGraph(
                ScriptGraphEngine.NODE_FLOAT_VALUE, "Float", "Value", 450, 510);
        f1.getOutputs().get(0).value = 1f;
        syncFloatDisplay(f1, 1f);

        ScriptNode min = addNodeToGraph(
                ScriptGraphEngine.NODE_MATH_MIN, "Mathf Min", "Math", 650, 350);

        connectNodes(ev2, "exec",  min, "exec");
        connectNodes(f5b, "value", min, "A");
        connectNodes(f1,  "value", min, "B");

        graphView.invalidate();
        setStatus("✓ تم تحميل مثال الصورة");
    }

    private ScriptNode addNodeToGraph(int type, String label, String cat,
                                       float x, float y) {
        int nativeId = ScriptGraphEngine.nativeAddNode(type, x, y);
        ScriptNode node = new ScriptNode(nativeId, type, label, cat, x, y);
        graphView.addNode(node);
        return node;
    }

    private void connectNodes(ScriptNode from, String fromPin,
                               ScriptNode to,   String toPin) {
        ScriptGraphEngine.nativeConnect(from.id, fromPin, to.id, toPin);

        Pin fp = from.findPin(fromPin, false);
        Pin tp = to.findPin(toPin,     true);
        if (fp != null && tp != null) {
            graphView.addConnection(from, fp, to, tp);
        }
    }

    private void syncFloatDisplay(ScriptNode node, float value) {
        ScriptGraphEngine.nativeSetFloat(node.id, "value", value);
    }

    // ── Run ───────────────────────────────────────────────────────────────

    private void runGraph() {
        appendConsole("▶ تشغيل Graph...\n");

        // تقييم C++
        for (ScriptNode node : graphView.getNodes()) {
            if (node.nativeType == ScriptGraphEngine.NODE_MATH_ABS ||
                node.nativeType == ScriptGraphEngine.NODE_MATH_MIN ||
                node.nativeType == ScriptGraphEngine.NODE_MATH_MAX ||
                node.nativeType == ScriptGraphEngine.NODE_MATH_SQRT ||
                node.nativeType == ScriptGraphEngine.NODE_MATH_SIN  ||
                node.nativeType == ScriptGraphEngine.NODE_MATH_COS  ||
                node.nativeType == ScriptGraphEngine.NODE_OP_SUBTRACT ||
                node.nativeType == ScriptGraphEngine.NODE_OP_ADD     ||
                node.nativeType == ScriptGraphEngine.NODE_OP_MULTIPLY ||
                node.nativeType == ScriptGraphEngine.NODE_OP_DIVIDE) {

                float result = ScriptGraphEngine.nativeEvaluate(node.id, "O");
                appendConsole("  [" + node.label + " #" + node.id + "] → " + formatVal(result) + "\n");

                // تحديث قيمة output pin
                for (Pin p : node.outputs) {
                    if (!p.name.equals("exec")) p.value = result;
                }
            }
        }

        // تنفيذ حدث OnUpdate من C++
        String log = ScriptGraphEngine.nativeExecuteEvent("OnUpdate");
        if (!log.isEmpty()) appendConsole(log);

        appendConsole("✓ اكتمل التنفيذ\n─────\n");
        graphView.invalidate();
        setStatus("✓ تم التنفيذ");
    }

    // ── Export ────────────────────────────────────────────────────────────

    private void exportRemiScript() {
        String code = ScriptGraphEngine.nativeExportRemiScript();

        // عرض الكود في Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📜 تصدير remiscript");

        ScrollView scroll = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(code.isEmpty() ? "// لا يوجد كود للتصدير" : code);
        tv.setTextColor(0xFF98C379);
        tv.setBackgroundColor(0xFF282C34);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setTextSize(13f);
        tv.setPadding(16, 16, 16, 16);
        scroll.addView(tv);

        builder.setView(scroll);
        builder.setPositiveButton("نسخ", (d, w) -> {
            android.content.ClipboardManager cm =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("remiscript", code));
            Toast.makeText(this, "تم النسخ!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("إغلاق", null);
        builder.show();

        appendConsole("📤 تم التصدير كـ remiscript\n");
        setStatus("✓ تم التصدير");
    }

    // ── Palette Toggle ────────────────────────────────────────────────────

    private void togglePalette() {
        paletteVisible = !paletteVisible;
        palettePanel.setVisibility(paletteVisible ? View.VISIBLE : View.GONE);
    }

    private void clearGraph() {
        new AlertDialog.Builder(this)
            .setTitle("مسح Graph")
            .setMessage("هل تريد مسح جميع العقد والاتصالات؟")
            .setPositiveButton("مسح", (d, w) -> {
                graphView.clearAll();
                ScriptGraphEngine.nativeClear();
                tvConsole.setText("⟶ جاهز...\n");
                setStatus("تم المسح");
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    // ── GraphListener ─────────────────────────────────────────────────────

    @Override
    public void onNodeSelected(ScriptNode node) {
        setStatus("✎ " + node.label + " #" + node.id);
    }

    @Override
    public void onConnectionMade(ScriptNode from, Pin fromPin,
                                  ScriptNode to,   Pin toPin) {
        ScriptGraphEngine.nativeConnect(from.id, fromPin.name, to.id, toPin.name);
        graphView.addConnection(from, fromPin, to, toPin);
        appendConsole("🔗 " + from.label + "." + fromPin.name +
                      " → " + to.label + "." + toPin.name + "\n");
        setStatus("✓ تم الربط");
    }

    @Override
    public void onNodeDoubleTap(ScriptNode node) {
        // تعديل قيمة Float
        if (node.nativeType == ScriptGraphEngine.NODE_FLOAT_VALUE) {
            showFloatEditDialog(node);
        }
    }

    // ── Float Edit Dialog ─────────────────────────────────────────────────

    private void showFloatEditDialog(ScriptNode node) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("تعديل قيمة Float");

        EditText et = new EditText(this);
        float cur = node.outputs.isEmpty() ? 0f : node.outputs.get(0).value;
        et.setText(formatVal(cur));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL |
                        android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        et.setTextColor(0xFF61AFEF);
        et.setBackgroundColor(0xFF282C34);
        et.setPadding(24, 16, 24, 16);
        b.setView(et);

        b.setPositiveButton("تطبيق", (d, w) -> {
            try {
                float val = Float.parseFloat(et.getText().toString());
                ScriptGraphEngine.nativeSetFloat(node.id, "value", val);
                if (!node.outputs.isEmpty()) node.outputs.get(0).value = val;
                graphView.invalidate();
                appendConsole("✎ " + node.label + "#" + node.id + " = " + formatVal(val) + "\n");
            } catch (NumberFormatException ignored) {}
        });
        b.setNegativeButton("إلغاء", null);
        b.show();

        et.selectAll();
        et.requestFocus();
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private void setStatus(String msg) {
        tvStatus.setText(msg);
    }

    private void appendConsole(String text) {
        tvConsole.append(text);
        scrollConsole.post(() ->
                scrollConsole.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private String formatVal(float v) {
        if (v == Math.floor(v) && !Float.isInfinite(v))
            return String.valueOf((int) v);
        return String.format("%.3f", v);
    }

    private String iconForCat(String cat) {
        switch (cat) {
            case "Event":  return "◉";
            case "Math":   return "π";
            case "Value":  return "—";
            case "Debug":  return "▣";
            default:       return "·";
        }
    }
}
