package com.remi8.scriptgraph;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.remi8.scriptgraph.nodes.ScriptNode;
import com.remi8.scriptgraph.nodes.ScriptNode.Pin;
import com.remi8.scriptgraph.nodes.ScriptNode.PinType;

import java.util.ArrayList;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────
 *  ScriptGraphView
 *  واجهة رسم Script Graph المرئي على Canvas
 *
 *  يدعم:
 *  • رسم العقد بنمط Unreal Engine Blueprint
 *  • سحب وإفلات العقد
 *  • تكبير/تصغير (Pinch to Zoom)
 *  • ربط Pins بالسحب
 *  • تمييز الاتصالات بألوان Pin Type
 * ─────────────────────────────────────────────────────────────────
 */
public class ScriptGraphView extends View {

    // ── ثوابت التصميم (نمط Blueprint) ────────────────────────────────────

    private static final float PIN_RADIUS       = 7f;
    private static final float PIN_SPACING      = 24f;
    private static final float HEADER_HEIGHT    = 30f;
    private static final float CORNER_RADIUS    = 8f;
    private static final float NODE_MIN_WIDTH   = 160f;

    // ألوان
    private static final int BG_COLOR           = 0xFF1A1D23;
    private static final int GRID_COLOR         = 0xFF252830;
    private static final int GRID_MAJOR_COLOR   = 0xFF2D3140;
    private static final int NODE_BG_COLOR      = 0xFF23272E;
    private static final int NODE_BORDER_COLOR  = 0xFF3A3F4B;
    private static final int NODE_SEL_COLOR     = 0xFF4DA6FF;
    private static final int TEXT_COLOR         = 0xFFABB2BF;
    private static final int TEXT_HEADER_COLOR  = 0xFFFFFFFF;
    private static final int PIN_EXEC_COLOR     = 0xFFFFFFFF;
    private static final int PIN_FLOAT_COLOR    = 0xFF00C3FF;
    private static final int PIN_INT_COLOR      = 0xFF70DB70;
    private static final int PIN_BOOL_COLOR     = 0xFFFF6B6B;
    private static final int WIRE_COLOR_EXEC    = 0xFFFFFFFF;
    private static final int WIRE_COLOR_FLOAT   = 0xFF00C3FF;
    private static final int WIRE_COLOR_DRAGGING= 0xFFFFD700;

    // ── State ─────────────────────────────────────────────────────────────

    private final List<ScriptNode> nodes       = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    // Pan & Zoom
    private float panX = 0f, panY = 0f;
    private float scale = 1.0f;
    private static final float MIN_SCALE = 0.25f;
    private static final float MAX_SCALE = 3.0f;

    // Drag
    private ScriptNode draggingNode = null;
    private float dragOffX, dragOffY;

    // Wire dragging
    private ScriptNode  wireSrcNode  = null;
    private Pin         wireSrcPin   = null;
    private float       wireDragX, wireDragY;

    // ── Paints ───────────────────────────────────────────────────────────

    private final Paint paintBg      = new Paint();
    private final Paint paintGrid    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGridMaj = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintNodeBg  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintHeader  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBorder  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTextSm  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPin     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintWire    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintValue   = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Gesture
    private GestureDetector      gestureDetector;
    private ScaleGestureDetector scaleDetector;

    // Listener
    public interface GraphListener {
        void onNodeSelected(ScriptNode node);
        void onConnectionMade(ScriptNode from, Pin fromPin, ScriptNode to, Pin toPin);
        void onNodeDoubleTap(ScriptNode node);
    }
    private GraphListener listener;

    // ── Connection model ─────────────────────────────────────────────────

    public static class Connection {
        public ScriptNode fromNode, toNode;
        public Pin fromPin, toPin;
        public int color;

        public Connection(ScriptNode f, Pin fp, ScriptNode t, Pin tp) {
            fromNode = f; fromPin = fp; toNode = t; toPin = tp;
            color = pinColor(fp.type);
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────

    public ScriptGraphView(Context ctx) { super(ctx); init(); }
    public ScriptGraphView(Context ctx, AttributeSet a) { super(ctx, a); init(); }

    private void init() {
        setupPaints();

        gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float wx = toWorldX(e.getX());
                float wy = toWorldY(e.getY());
                ScriptNode n = nodeAt(wx, wy);
                if (n != null && listener != null) listener.onNodeDoubleTap(n);
                return true;
            }
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                float wx = toWorldX(e.getX());
                float wy = toWorldY(e.getY());
                ScriptNode n = nodeAt(wx, wy);
                nodes.forEach(nd -> nd.selected = false);
                if (n != null) {
                    n.selected = true;
                    if (listener != null) listener.onNodeSelected(n);
                }
                invalidate();
                return true;
            }
        });

        scaleDetector = new ScaleGestureDetector(getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector det) {
                float factor = det.getScaleFactor();
                scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
                invalidate();
                return true;
            }
        });
    }

    private void setupPaints() {
        paintBg.setColor(BG_COLOR);
        paintBg.setStyle(Paint.Style.FILL);

        paintGrid.setColor(GRID_COLOR);
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(0.5f);

        paintGridMaj.setColor(GRID_MAJOR_COLOR);
        paintGridMaj.setStyle(Paint.Style.STROKE);
        paintGridMaj.setStrokeWidth(1f);

        paintNodeBg.setColor(NODE_BG_COLOR);
        paintNodeBg.setStyle(Paint.Style.FILL);

        paintHeader.setStyle(Paint.Style.FILL);

        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(1.5f);

        paintText.setColor(TEXT_COLOR);
        paintText.setTextSize(14f);
        paintText.setTypeface(Typeface.DEFAULT);

        paintTextSm.setColor(TEXT_COLOR);
        paintTextSm.setTextSize(12f);

        paintPin.setStyle(Paint.Style.FILL);
        paintPin.setAntiAlias(true);

        paintWire.setStyle(Paint.Style.STROKE);
        paintWire.setStrokeWidth(2f);
        paintWire.setAntiAlias(true);

        paintValue.setColor(0xFF61AFEF);
        paintValue.setTextSize(18f);
        paintValue.setTypeface(Typeface.DEFAULT_BOLD);
        paintValue.setTextAlign(Paint.Align.CENTER);
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setGraphListener(GraphListener l) { this.listener = l; }

    public void addNode(ScriptNode node) {
        nodes.add(node);
        invalidate();
    }

    public void addConnection(ScriptNode from, Pin fromPin,
                               ScriptNode to,   Pin toPin) {
        fromPin.connectedNodeId = String.valueOf(to.id);
        fromPin.connectedPin    = toPin.name;
        connections.add(new Connection(from, fromPin, to, toPin));
        invalidate();
    }

    public void clearAll() {
        nodes.clear();
        connections.clear();
        invalidate();
    }

    public void centerView() {
        panX = 0; panY = 0; scale = 1f;
        invalidate();
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // خلفية
        canvas.drawRect(0, 0, getWidth(), getHeight(), paintBg);

        canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(scale, scale);

        drawGrid(canvas);
        drawConnections(canvas);
        drawDragWire(canvas);
        drawNodes(canvas);

        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        float step = 20f;
        float majorStep = 100f;

        int w = (int)((getWidth()  / scale) + majorStep);
        int h = (int)((getHeight() / scale) + majorStep);
        float ox = -panX / scale;
        float oy = -panY / scale;

        float startX = (float)(Math.floor(ox / step) * step);
        float startY = (float)(Math.floor(oy / step) * step);

        for (float x = startX; x < ox + w; x += step) {
            boolean major = (Math.abs(x % majorStep) < 1f);
            canvas.drawLine(x, oy, x, oy + h, major ? paintGridMaj : paintGrid);
        }
        for (float y = startY; y < oy + h; y += step) {
            boolean major = (Math.abs(y % majorStep) < 1f);
            canvas.drawLine(ox, y, ox + w, y, major ? paintGridMaj : paintGrid);
        }
    }

    private void drawConnections(Canvas canvas) {
        for (Connection c : connections) {
            float x1 = c.fromPin.screenX;
            float y1 = c.fromPin.screenY;
            float x2 = c.toPin.screenX;
            float y2 = c.toPin.screenY;

            paintWire.setColor(c.color);
            paintWire.setStrokeWidth(2.5f / scale);
            drawBezierWire(canvas, x1, y1, x2, y2);
        }
    }

    private void drawDragWire(Canvas canvas) {
        if (wireSrcPin == null) return;
        paintWire.setColor(WIRE_COLOR_DRAGGING);
        paintWire.setStrokeWidth(2.5f / scale);
        drawBezierWire(canvas,
                wireSrcPin.screenX, wireSrcPin.screenY,
                wireDragX, wireDragY);
    }

    private void drawBezierWire(Canvas canvas,
                                 float x1, float y1, float x2, float y2) {
        Path path = new Path();
        float cx = Math.abs(x2 - x1) * 0.5f;
        path.moveTo(x1, y1);
        path.cubicTo(x1 + cx, y1, x2 - cx, y2, x2, y2);
        canvas.drawPath(path, paintWire);
    }

    private void drawNodes(Canvas canvas) {
        for (ScriptNode node : nodes) {
            drawNode(canvas, node);
        }
    }

    private void drawNode(Canvas canvas, ScriptNode node) {
        float x = node.x, y = node.y, w = node.width, h = node.height;
        RectF rect = new RectF(x, y, x + w, y + h);

        // ظل
        Paint shadow = new Paint(paintNodeBg);
        shadow.setColor(0x55000000);
        RectF shadowRect = new RectF(x + 3, y + 3, x + w + 3, y + h + 3);
        canvas.drawRoundRect(shadowRect, CORNER_RADIUS, CORNER_RADIUS, shadow);

        // خلفية العقدة
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paintNodeBg);

        // Header
        RectF headerRect = new RectF(x, y, x + w, y + HEADER_HEIGHT);
        paintHeader.setColor(node.headerColor);
        canvas.drawRoundRect(headerRect, CORNER_RADIUS, CORNER_RADIUS, paintHeader);
        // Fix header bottom corners
        canvas.drawRect(x, y + HEADER_HEIGHT - CORNER_RADIUS, x + w, y + HEADER_HEIGHT, paintHeader);

        // عنوان العقدة
        Paint headerText = new Paint(paintText);
        headerText.setColor(TEXT_HEADER_COLOR);
        headerText.setTextSize(13f);
        headerText.setTypeface(Typeface.DEFAULT_BOLD);

        // Category icon + label
        String title = iconFor(node.category) + " " + node.label;
        canvas.drawText(title, x + 8f, y + 19f, headerText);

        // حد
        paintBorder.setColor(node.selected ? NODE_SEL_COLOR : NODE_BORDER_COLOR);
        paintBorder.setStrokeWidth((node.selected ? 2.5f : 1f) / scale);
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paintBorder);

        // رسم قيمة خاصة للـ Float Value
        if (node.nativeType == ScriptGraphEngine.NODE_FLOAT_VALUE) {
            String val = node.getDisplayValue();
            paintValue.setTextSize(22f);
            canvas.drawText(val, x + w / 2f, y + HEADER_HEIGHT + 30f, paintValue);
        }

        // رسم Pins
        float pinY = y + HEADER_HEIGHT + 16f;

        // Inputs
        for (int i = 0; i < node.inputs.size(); i++) {
            Pin p = node.inputs.get(i);
            float py = pinY + i * PIN_SPACING;
            p.screenX = x;
            p.screenY = py;

            // دائرة Pin
            paintPin.setColor(pinColor(p.type));
            boolean connected = p.connectedNodeId != null;
            if (connected) {
                canvas.drawCircle(x, py, PIN_RADIUS, paintPin);
            } else {
                Paint pinStroke = new Paint(paintPin);
                pinStroke.setStyle(Paint.Style.STROKE);
                pinStroke.setStrokeWidth(1.5f);
                canvas.drawCircle(x, py, PIN_RADIUS, pinStroke);
            }

            // اسم Pin
            canvas.drawText(p.name, x + PIN_RADIUS + 4f, py + 4f, paintTextSm);
        }

        // Outputs
        for (int i = 0; i < node.outputs.size(); i++) {
            Pin p = node.outputs.get(i);
            float py = pinY + i * PIN_SPACING;
            p.screenX = x + w;
            p.screenY = py;

            paintPin.setColor(pinColor(p.type));
            boolean connected = p.connectedNodeId != null;
            if (connected) {
                canvas.drawCircle(x + w, py, PIN_RADIUS, paintPin);
            } else {
                Paint pinStroke = new Paint(paintPin);
                pinStroke.setStyle(Paint.Style.STROKE);
                pinStroke.setStrokeWidth(1.5f);
                canvas.drawCircle(x + w, py, PIN_RADIUS, pinStroke);
            }

            // اسم Pin (محاذاة يمين)
            Paint rtl = new Paint(paintTextSm);
            rtl.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(p.name, x + w - PIN_RADIUS - 4f, py + 4f, rtl);
        }
    }

    // ── Touch Handling ────────────────────────────────────────────────────

    private float lastTouchX, lastTouchY;
    private boolean isPanning = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        float wx = toWorldX(event.getX());
        float wy = toWorldY(event.getY());

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                lastTouchX = event.getX();
                lastTouchY = event.getY();

                // اختبار Pin أولاً
                Pin pin = pinAt(wx, wy);
                if (pin != null) {
                    ScriptNode owner = pinOwner(pin);
                    if (owner != null && !pin.isInput) {
                        wireSrcNode = owner;
                        wireSrcPin  = pin;
                        wireDragX   = wx;
                        wireDragY   = wy;
                        return true;
                    }
                }

                // ثم اختبار العقدة
                ScriptNode n = nodeAt(wx, wy);
                if (n != null) {
                    draggingNode = n;
                    dragOffX = wx - n.x;
                    dragOffY = wy - n.y;
                    // رفع للأمام
                    nodes.remove(n);
                    nodes.add(n);
                    isPanning = false;
                } else {
                    isPanning = true;
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (wireSrcPin != null) {
                    wireDragX = wx;
                    wireDragY = wy;
                    invalidate();
                    return true;
                }
                if (draggingNode != null) {
                    draggingNode.x = wx - dragOffX;
                    draggingNode.y = wy - dragOffY;
                    invalidate();
                } else if (isPanning) {
                    panX += event.getX() - lastTouchX;
                    panY += event.getY() - lastTouchY;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (wireSrcPin != null) {
                    // محاولة ربط بـ Pin هدف
                    Pin targetPin = pinAt(wx, wy);
                    if (targetPin != null && targetPin.isInput && targetPin != wireSrcPin) {
                        ScriptNode targetNode = pinOwner(targetPin);
                        if (targetNode != null && listener != null) {
                            listener.onConnectionMade(wireSrcNode, wireSrcPin, targetNode, targetPin);
                        }
                    }
                    wireSrcNode = null;
                    wireSrcPin  = null;
                    invalidate();
                }
                draggingNode = null;
                isPanning    = false;
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    // ── Hit Testing ───────────────────────────────────────────────────────

    private ScriptNode nodeAt(float wx, float wy) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (nodes.get(i).contains(wx, wy)) return nodes.get(i);
        }
        return null;
    }

    private Pin pinAt(float wx, float wy) {
        float threshold = PIN_RADIUS * 2.5f;
        for (ScriptNode n : nodes) {
            for (Pin p : n.inputs) {
                if (dist(p.screenX, p.screenY, wx, wy) < threshold) return p;
            }
            for (Pin p : n.outputs) {
                if (dist(p.screenX, p.screenY, wx, wy) < threshold) return p;
            }
        }
        return null;
    }

    private ScriptNode pinOwner(Pin pin) {
        for (ScriptNode n : nodes) {
            for (Pin p : n.inputs)  if (p == pin) return n;
            for (Pin p : n.outputs) if (p == pin) return n;
        }
        return null;
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private float toWorldX(float sx) { return (sx - panX) / scale; }
    private float toWorldY(float sy) { return (sy - panY) / scale; }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static int pinColor(PinType type) {
        switch (type) {
            case EXEC:   return PIN_EXEC_COLOR;
            case FLOAT:  return PIN_FLOAT_COLOR;
            case INT:    return PIN_INT_COLOR;
            case BOOL:   return PIN_BOOL_COLOR;
            default:     return PIN_FLOAT_COLOR;
        }
    }

    private static String iconFor(String cat) {
        switch (cat) {
            case "Event":  return "◉";
            case "Math":   return "π";
            case "Value":  return "—";
            case "Flow":   return "◆";
            case "Debug":  return "▣";
            default:       return "·";
        }
    }

    public List<ScriptNode> getNodes() { return nodes; }
    public List<Connection> getConnections() { return connections; }
}
