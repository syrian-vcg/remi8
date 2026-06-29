package com.remi8.remiscript.interpreter;

import android.graphics.Color;
import android.util.Log;

import com.remi8.engine.core.GameEngine;
import com.remi8.engine.core.GameObject;
import com.remi8.engine.physics.PhysicsBody;
import com.remi8.remiscript.parser.RemiScriptLexer;
import com.remi8.remiscript.parser.RemiScriptLexer.Token;
import com.remi8.remiscript.parser.RemiScriptLexer.TokenType;
import com.remi8.scriptgraph.ScriptGraphEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────
 *  RemiScriptInterpreter v2 - REMI8
 *  مفسّر لغة remiscript المطوّر
 *
 *  جديد في v2:
 *  • تكامل مع Script Graph (تنفيذ العقد البرمجية)
 *  • دوال رياضيات Mathf.Abs/Min/Max/Sqrt/Sin/Cos
 *  • دعم نظام الأحداث (OnUpdate / OnStart / OnCollision)
 *  • تنفيذ Graph من remiscript مباشرة
 *  • تحسينات الأداء وإضافة عمليات Bitwise
 * ─────────────────────────────────────────────────────────────
 */
public class RemiScriptInterpreter {

    private static final String TAG = "REMI8_Script_v2";

    private final GameEngine engine;

    // بيئة التنفيذ العالمية
    private final Map<String, Object> globalScope = new HashMap<>();

    // الدوال المعرّفة
    private final Map<String, ScriptFunction> functions = new HashMap<>();

    // السكربتات المحملة
    private final Map<String, ParsedScript> loadedScripts = new HashMap<>();

    // سكربتات الحدث
    private final Map<String, List<String>> eventScripts = new HashMap<>();

    // ── استثناءات التحكم ──────────────────────────────────────────────────
    private static class BreakException    extends RuntimeException {}
    private static class ContinueException extends RuntimeException {}
    private static class ReturnException   extends RuntimeException {
        final Object value;
        ReturnException(Object value) { this.value = value; }
    }

    // ─────────────────────────────────────────────────────────────────────

    public RemiScriptInterpreter(GameEngine engine) {
        this.engine = engine;
        registerBuiltins();
    }

    // ── تسجيل الدوال المدمجة ───────────────────────────────────────────────

    private void registerBuiltins() {

        // ── طباعة ────────────────────────────────────────────────────────
        globalScope.put("طباعة", (BuiltinFunction) args -> {
            StringBuilder sb = new StringBuilder();
            for (Object a : args) sb.append(toStr(a)).append(" ");
            Log.d(TAG, "[remiscript] " + sb.toString().trim());
            return null;
        });
        globalScope.put("print", globalScope.get("طباعة"));

        // ── رياضيات (Mathf) ───────────────────────────────────────────────
        globalScope.put("رياضيات", createMathObject());
        globalScope.put("Mathf",   globalScope.get("رياضيات"));
        globalScope.put("math",    globalScope.get("رياضيات"));

        // ── مدخلات ────────────────────────────────────────────────────────
        globalScope.put("مدخل", createInputObject());
        globalScope.put("input", globalScope.get("مدخل"));

        // ── صوت ───────────────────────────────────────────────────────────
        globalScope.put("صوت", createAudioObject());
        globalScope.put("audio", globalScope.get("صوت"));

        // ── مشهد ──────────────────────────────────────────────────────────
        globalScope.put("مشهد", createSceneObject());
        globalScope.put("scene", globalScope.get("مشهد"));

        // ── وقت ───────────────────────────────────────────────────────────
        globalScope.put("وقت", createTimeObject());
        globalScope.put("Time", globalScope.get("وقت"));

        // ── عشوائي ────────────────────────────────────────────────────────
        globalScope.put("عشوائي", (BuiltinFunction) args -> {
            if (args.size() >= 2) {
                float mn = toFloat(args.get(0)), mx = toFloat(args.get(1));
                return mn + (float)(Math.random() * (mx - mn));
            }
            return (float) Math.random();
        });
        globalScope.put("random", globalScope.get("عشوائي"));

        // ── Graph (Script Graph تشغيل من remiscript) ──────────────────────
        globalScope.put("Graph", createGraphObject());

        // ── نظام الأحداث ──────────────────────────────────────────────────
        globalScope.put("حدث", createEventObject());
    }

    // ── كائن الرياضيات ────────────────────────────────────────────────────

    private Map<String, Object> createMathObject() {
        Map<String, Object> m = new HashMap<>();
        m.put("PI", (float) Math.PI);
        m.put("ط",  (float) Math.PI);
        m.put("E",  (float) Math.E);

        m.put("مطلق",    (BuiltinFunction) a -> Math.abs(toFloat(a.get(0))));
        m.put("Abs",     m.get("مطلق"));
        m.put("abs",     m.get("مطلق"));

        m.put("جذر",     (BuiltinFunction) a -> (float) Math.sqrt(toFloat(a.get(0))));
        m.put("Sqrt",    m.get("جذر"));
        m.put("sqrt",    m.get("جذر"));

        m.put("أقصى",    (BuiltinFunction) a -> Math.max(toFloat(a.get(0)), toFloat(a.get(1))));
        m.put("Max",     m.get("أقصى"));
        m.put("max",     m.get("أقصى"));

        m.put("أدنى",    (BuiltinFunction) a -> Math.min(toFloat(a.get(0)), toFloat(a.get(1))));
        m.put("Min",     m.get("أدنى"));
        m.put("min",     m.get("أدنى"));

        m.put("تقريب",   (BuiltinFunction) a -> (float) Math.round(toFloat(a.get(0))));
        m.put("Round",   m.get("تقريب"));

        m.put("أرضية",   (BuiltinFunction) a -> (float) Math.floor(toFloat(a.get(0))));
        m.put("Floor",   m.get("أرضية"));

        m.put("سقف",     (BuiltinFunction) a -> (float) Math.ceil(toFloat(a.get(0))));
        m.put("Ceil",    m.get("سقف"));

        m.put("جيب",     (BuiltinFunction) a -> (float) Math.sin(Math.toRadians(toFloat(a.get(0)))));
        m.put("Sin",     m.get("جيب"));

        m.put("جيب_تمام",(BuiltinFunction) a -> (float) Math.cos(Math.toRadians(toFloat(a.get(0)))));
        m.put("Cos",     m.get("جيب_تمام"));

        m.put("ظل",      (BuiltinFunction) a -> (float) Math.tan(Math.toRadians(toFloat(a.get(0)))));
        m.put("Tan",     m.get("ظل"));

        m.put("قوة",     (BuiltinFunction) a -> (float) Math.pow(toFloat(a.get(0)), toFloat(a.get(1))));
        m.put("Pow",     m.get("قوة"));

        m.put("لوغاريتم",(BuiltinFunction) a -> (float) Math.log(toFloat(a.get(0))));
        m.put("Log",     m.get("لوغاريتم"));

        m.put("تثبيت",   (BuiltinFunction) a -> {
            float v = toFloat(a.get(0)), mn = toFloat(a.get(1)), mx = toFloat(a.get(2));
            return Math.max(mn, Math.min(mx, v));
        });
        m.put("Clamp",   m.get("تثبيت"));

        m.put("تمدد",    (BuiltinFunction) a -> {
            float v = toFloat(a.get(0)), mn = toFloat(a.get(1)), mx = toFloat(a.get(2));
            return mn + (mx - mn) * v;
        });
        m.put("Lerp",    m.get("تمدد"));

        m.put("إشارة",   (BuiltinFunction) a -> (float) Math.signum(toFloat(a.get(0))));
        m.put("Sign",    m.get("إشارة"));

        return m;
    }

    // ── كائن المدخلات ─────────────────────────────────────────────────────

    private Map<String, Object> createInputObject() {
        Map<String, Object> inp = new HashMap<>();

        inp.put("زر", (BuiltinFunction) a -> {
            if (a.isEmpty()) return false;
            return engine.getInputManager().isButtonPressed(toStr(a.get(0)));
        });
        inp.put("Button", inp.get("زر"));

        inp.put("زر_ضغط", (BuiltinFunction) a -> {
            if (a.isEmpty()) return false;
            return engine.getInputManager().isButtonJustPressed(toStr(a.get(0)));
        });
        inp.put("ButtonDown", inp.get("زر_ضغط"));

        inp.put("لمس", (BuiltinFunction) a ->
            !engine.getInputManager().getTouchPoints().isEmpty());
        inp.put("Touch", inp.get("لمس"));

        inp.put("محور_س", (BuiltinFunction) a ->
            engine.getInputManager().getAxis("x"));
        inp.put("محور_ص", (BuiltinFunction) a ->
            engine.getInputManager().getAxis("y"));

        return inp;
    }

    // ── كائن الصوت ────────────────────────────────────────────────────────

    private Map<String, Object> createAudioObject() {
        Map<String, Object> aud = new HashMap<>();

        aud.put("شغّل", (BuiltinFunction) a -> {
            if (!a.isEmpty()) engine.getAudioManager().playSound(toStr(a.get(0)));
            return null;
        });
        aud.put("play",   aud.get("شغّل"));

        aud.put("أوقف", (BuiltinFunction) a -> {
            engine.getAudioManager().stopAll();
            return null;
        });
        aud.put("stop",   aud.get("أوقف"));

        aud.put("موسيقى", (BuiltinFunction) a -> {
            if (!a.isEmpty()) engine.getAudioManager().playMusic(toStr(a.get(0)));
            return null;
        });
        aud.put("music",  aud.get("موسيقى"));

        aud.put("صوت_الخلفية", (BuiltinFunction) a -> {
            if (a.size() >= 2) {
                engine.getAudioManager().setVolume(toFloat(a.get(0)), toFloat(a.get(1)));
            }
            return null;
        });

        return aud;
    }

    // ── كائن المشهد ───────────────────────────────────────────────────────

    private Map<String, Object> createSceneObject() {
        Map<String, Object> sc = new HashMap<>();

        sc.put("تحميل", (BuiltinFunction) a -> {
            if (!a.isEmpty()) engine.getSceneManager().loadScene(toStr(a.get(0)));
            return null;
        });
        sc.put("load",    sc.get("تحميل"));

        sc.put("إعادة", (BuiltinFunction) a -> {
            engine.getSceneManager().reloadCurrentScene();
            return null;
        });
        sc.put("reload",  sc.get("إعادة"));

        sc.put("كائن", (BuiltinFunction) a -> {
            if (a.isEmpty()) return null;
            return engine.getSceneManager()
                         .getCurrentScene()
                         .findObject(toStr(a.get(0)));
        });
        sc.put("getObject", sc.get("كائن"));

        return sc;
    }

    // ── كائن الوقت ────────────────────────────────────────────────────────

    private Map<String, Object> createTimeObject() {
        Map<String, Object> t = new HashMap<>();
        t.put("الآن",      (BuiltinFunction) a -> (float)(System.currentTimeMillis() / 1000.0));
        t.put("now",       t.get("الآن"));
        t.put("ثوانٍ",     (BuiltinFunction) a -> (float)(System.nanoTime() / 1e9));
        t.put("seconds",   t.get("ثوانٍ"));
        return t;
    }

    // ── كائن Graph (تكامل Script Graph) ──────────────────────────────────

    private Map<String, Object> createGraphObject() {
        Map<String, Object> g = new HashMap<>();

        // تشغيل حدث في الـ Graph
        g.put("تشغيل", (BuiltinFunction) a -> {
            String event = a.isEmpty() ? "OnUpdate" : toStr(a.get(0));
            return ScriptGraphEngine.nativeExecuteEvent(event);
        });
        g.put("run", g.get("تشغيل"));

        // تقييم عقدة بـ ID
        g.put("قيمة", (BuiltinFunction) a -> {
            if (a.size() < 2) return 0f;
            int id = (int) toFloat(a.get(0));
            String pin = toStr(a.get(1));
            return ScriptGraphEngine.nativeEvaluate(id, pin);
        });
        g.put("eval", g.get("قيمة"));

        // تصدير Graph كـ remiscript
        g.put("تصدير", (BuiltinFunction) a ->
            ScriptGraphEngine.nativeExportRemiScript());
        g.put("export", g.get("تصدير"));

        return g;
    }

    // ── كائن الأحداث ─────────────────────────────────────────────────────

    private Map<String, Object> createEventObject() {
        Map<String, Object> ev = new HashMap<>();

        ev.put("تسجيل", (BuiltinFunction) a -> {
            if (a.size() >= 2) {
                String name = toStr(a.get(0));
                String script = toStr(a.get(1));
                eventScripts.computeIfAbsent(name, k -> new ArrayList<>()).add(script);
            }
            return null;
        });
        ev.put("on", ev.get("تسجيل"));

        ev.put("إطلاق", (BuiltinFunction) a -> {
            if (a.isEmpty()) return null;
            String name = toStr(a.get(0));
            List<String> scripts = eventScripts.get(name);
            if (scripts != null) {
                for (String s : scripts) runScript(s, "event_" + name);
            }
            return null;
        });
        ev.put("emit", ev.get("إطلاق"));

        return ev;
    }

    // ── تشغيل Script ──────────────────────────────────────────────────────

    public void runScript(String code, String name) {
        try {
            RemiScriptLexer lexer = new RemiScriptLexer(code);
            List<Token> tokens = lexer.tokenize();
            RemiScriptParser parser = new RemiScriptParser(tokens);
            List<ASTNode> ast = parser.parseProgram();
            ParsedScript parsed = new ParsedScript(name, code, ast);
            loadedScripts.put(name, parsed);
            executeBlock(parsed.ast, new HashMap<>(globalScope));
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تنفيذ " + name + ": " + e.getMessage(), e);
        }
    }

    // ── تحديث كل إطار ─────────────────────────────────────────────────────

    /**
     * يُستدعى من GameEngine في كل إطار لتشغيل سكربتات وأحداث OnUpdate
     */
    public void update(float dt) {
        Map<String, Object> args = new HashMap<>();
        args.put("دت", dt);
        args.put("dt", dt);
        callEvent("OnUpdate", args);
        List<String> scripts = eventScripts.get("OnUpdate");
        if (scripts != null) {
            for (String s : scripts) runScript(s, "event_OnUpdate");
        }
    }

    public void callEvent(String eventName, Map<String, Object> args) {
        if (functions.containsKey(eventName)) {
            List<Object> argList = new ArrayList<>(args.values());
            ScriptFunction fn = functions.get(eventName);
            Map<String, Object> scope = new HashMap<>(globalScope);
            scope.putAll(args);
            try {
                executeBlock(fn.body, scope);
            } catch (ReturnException ignored) {}
        }
        // تشغيل حدث Graph أيضاً
        ScriptGraphEngine.nativeExecuteEvent(eventName);
    }

    // ── تنفيذ Block ───────────────────────────────────────────────────────

    private Object executeBlock(List<ASTNode> stmts, Map<String, Object> scope) {
        for (ASTNode s : stmts) execute(s, scope);
        return null;
    }

    private Object execute(ASTNode node, Map<String, Object> scope) {
        switch (node.type) {

            case "برنامج":
                return executeBlock(node.children, scope);

            case "تعبير":
                return evaluate(node.children.get(0), scope);

            case "دالة": {
                ScriptFunction fn = new ScriptFunction(
                    node.value, node.params, node.block, new HashMap<>(scope));
                functions.put(node.value, fn);
                globalScope.put(node.value, fn);
                return null;
            }

            case "إذا": {
                boolean cond = isTruthy(evaluate(node.children.get(0), scope));
                if (cond) {
                    executeBlock(node.block, scope);
                } else if (node.elseBlock != null) {
                    executeBlock(node.elseBlock, scope);
                }
                return null;
            }

            case "طالما": {
                while (isTruthy(evaluate(node.children.get(0), scope))) {
                    try {
                        executeBlock(node.block, scope);
                    } catch (BreakException e)    { break; }
                      catch (ContinueException e) { }
                }
                return null;
            }

            case "لكل": {
                String var = node.value;
                Object iter = evaluate(node.children.get(0), scope);
                List<?> list = iter instanceof List ? (List<?>) iter : new ArrayList<>();
                for (Object item : list) {
                    scope.put(var, item);
                    try {
                        executeBlock(node.block, scope);
                    } catch (BreakException e)    { break; }
                      catch (ContinueException e) { }
                }
                return null;
            }

            case "كرر": {
                int count = (int) toFloat(evaluate(node.children.get(0), scope));
                for (int i = 0; i < count; i++) {
                    scope.put("i", (float) i);
                    try {
                        executeBlock(node.block, scope);
                    } catch (BreakException e)    { break; }
                      catch (ContinueException e) { }
                }
                return null;
            }

            case "إيقاف":    throw new BreakException();
            case "استمر":    throw new ContinueException();
            case "إرجاع":
                throw new ReturnException(
                    node.children.isEmpty() ? null : evaluate(node.children.get(0), scope));

            case "تعيين": {
                ASTNode target = node.children.get(0);
                Object val = evaluate(node.children.get(1), scope);
                if (target.type.equals("وصول_عضو")) {
                    Object obj = evaluate(target.children.get(0), scope);
                    setMemberProperty(obj, target.value, val);
                } else {
                    String name = target.value;
                    if (scope.containsKey(name)) scope.put(name, val);
                    else globalScope.put(name, val);
                }
                return null;
            }

            case "متغير":
            case "ثابت": {
                Object val = node.children.isEmpty() ? null
                           : evaluate(node.children.get(0), scope);
                scope.put(node.value, val);
                return null;
            }

            default:
                return evaluate(node, scope);
        }
    }

    // ── تقييم التعبيرات ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Object evaluate(ASTNode node, Map<String, Object> scope) {
        switch (node.type) {

            case "عدد":   return Float.parseFloat(node.value);
            case "نص":    return node.value;
            case "صحيح":  return true;
            case "خطأ":   return false;
            case "فارغ":  return null;

            case "معرف": {
                String name = node.value;
                if (scope.containsKey(name))       return scope.get(name);
                if (globalScope.containsKey(name)) return globalScope.get(name);
                if (functions.containsKey(name))   return functions.get(name);
                return null;
            }

            case "وصول_عضو": {
                Object obj = evaluate(node.children.get(0), scope);
                String member = node.value;
                if (obj instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) obj;
                    // setter/getter
                    if (map.containsKey("_get_" + member) &&
                        map.get("_get_" + member) instanceof BuiltinFunction) {
                        return ((BuiltinFunction) map.get("_get_" + member)).call(new ArrayList<>());
                    }
                    return map.get(member);
                }
                return null;
            }

            case "قائمة": {
                List<Object> list = new ArrayList<>();
                for (ASTNode c : node.children) list.add(evaluate(c, scope));
                return list;
            }

            case "قاموس": {
                Map<String, Object> dict = new HashMap<>();
                for (int i = 0; i < node.children.size() - 1; i += 2) {
                    String key = node.children.get(i).value;
                    Object val = evaluate(node.children.get(i + 1), scope);
                    dict.put(key, val);
                }
                return dict;
            }

            case "فهرس": {
                Object obj = evaluate(node.children.get(0), scope);
                Object idx = evaluate(node.children.get(1), scope);
                if (obj instanceof List && idx instanceof Number) {
                    int i = ((Number) idx).intValue();
                    List<?> list = (List<?>) obj;
                    return (i >= 0 && i < list.size()) ? list.get(i) : null;
                }
                if (obj instanceof Map) return ((Map<?, ?>) obj).get(toStr(idx));
                return null;
            }

            case "استدعاء": {
                Object callee;
                Object thisObj = null;

                ASTNode calleeNode = node.children.get(0);
                if (calleeNode.type.equals("وصول_عضو")) {
                    thisObj = evaluate(calleeNode.children.get(0), scope);
                    String methodName = calleeNode.value;
                    callee = (thisObj instanceof Map)
                           ? ((Map<?, ?>) thisObj).get(methodName)
                           : null;
                } else {
                    callee = evaluate(calleeNode, scope);
                }

                List<Object> args = new ArrayList<>();
                for (int i = 1; i < node.children.size(); i++)
                    args.add(evaluate(node.children.get(i), scope));

                if (callee instanceof BuiltinFunction)
                    return ((BuiltinFunction) callee).call(args);
                if (callee instanceof ScriptFunction)
                    return callFunction((ScriptFunction) callee, args, scope);
                return null;
            }

            // ── زائد_زائد / ناقص_ناقص ───────────────────────────────────
            case "زائد_زائد_بعدي": {
                String v = node.children.get(0).value;
                float old = toFloat(getVar(v, scope));
                setVar(v, old + 1f, scope);
                return old;
            }
            case "ناقص_ناقص_بعدي": {
                String v = node.children.get(0).value;
                float old = toFloat(getVar(v, scope));
                setVar(v, old - 1f, scope);
                return old;
            }
            case "زائد_زائد_قبلي": {
                String v = node.children.get(0).value;
                float nv = toFloat(getVar(v, scope)) + 1f;
                setVar(v, nv, scope);
                return nv;
            }
            case "ناقص_ناقص_قبلي": {
                String v = node.children.get(0).value;
                float nv = toFloat(getVar(v, scope)) - 1f;
                setVar(v, nv, scope);
                return nv;
            }

            // ── عمليات ثنائية ──────────────────────────────────────────
            case "زائد":        return add(evaluate(node.children.get(0), scope), evaluate(node.children.get(1), scope));
            case "ناقص":        return toFloat(evaluate(node.children.get(0), scope)) - toFloat(evaluate(node.children.get(1), scope));
            case "ضرب":         return toFloat(evaluate(node.children.get(0), scope)) * toFloat(evaluate(node.children.get(1), scope));
            case "قسمة": {
                float b = toFloat(evaluate(node.children.get(1), scope));
                return b != 0 ? toFloat(evaluate(node.children.get(0), scope)) / b : 0f;
            }
            case "باقي":        return toFloat(evaluate(node.children.get(0), scope)) % toFloat(evaluate(node.children.get(1), scope));
            case "يساوي_يساوي": return equals(evaluate(node.children.get(0), scope), evaluate(node.children.get(1), scope));
            case "لا_يساوي":    return !equals(evaluate(node.children.get(0), scope), evaluate(node.children.get(1), scope));
            case "أكبر":        return toFloat(evaluate(node.children.get(0), scope)) > toFloat(evaluate(node.children.get(1), scope));
            case "أصغر":        return toFloat(evaluate(node.children.get(0), scope)) < toFloat(evaluate(node.children.get(1), scope));
            case "أكبر_يساوي":  return toFloat(evaluate(node.children.get(0), scope)) >= toFloat(evaluate(node.children.get(1), scope));
            case "أصغر_يساوي":  return toFloat(evaluate(node.children.get(0), scope)) <= toFloat(evaluate(node.children.get(1), scope));
            case "و":           return isTruthy(evaluate(node.children.get(0), scope)) && isTruthy(evaluate(node.children.get(1), scope));
            case "أو":          return isTruthy(evaluate(node.children.get(0), scope)) || isTruthy(evaluate(node.children.get(1), scope));
            case "ليس":         return !isTruthy(evaluate(node.children.get(0), scope));
            case "سالب":        return -toFloat(evaluate(node.children.get(0), scope));

            // ── Bitwise (جديد في v2) ────────────────────────────────────
            case "و_بتي":       return (float)((int)toFloat(evaluate(node.children.get(0), scope)) & (int)toFloat(evaluate(node.children.get(1), scope)));
            case "أو_بتي":      return (float)((int)toFloat(evaluate(node.children.get(0), scope)) | (int)toFloat(evaluate(node.children.get(1), scope)));
            case "أو_حصري":     return (float)((int)toFloat(evaluate(node.children.get(0), scope)) ^ (int)toFloat(evaluate(node.children.get(1), scope)));
            case "إزاحة_يسار":  return (float)((int)toFloat(evaluate(node.children.get(0), scope)) << (int)toFloat(evaluate(node.children.get(1), scope)));
            case "إزاحة_يمين":  return (float)((int)toFloat(evaluate(node.children.get(0), scope)) >> (int)toFloat(evaluate(node.children.get(1), scope)));

            default: return null;
        }
    }

    // ── أدوات مساعدة ──────────────────────────────────────────────────────

    private Object callFunction(ScriptFunction fn, List<Object> args,
                                Map<String, Object> callerScope) {
        Map<String, Object> fnScope = new HashMap<>(fn.closure);
        for (int i = 0; i < fn.params.size(); i++)
            fnScope.put(fn.params.get(i), i < args.size() ? args.get(i) : null);
        try {
            return executeBlock(fn.body, fnScope);
        } catch (ReturnException r) {
            return r.value;
        }
    }

    private Object getVar(String name, Map<String, Object> scope) {
        if (scope.containsKey(name))       return scope.get(name);
        if (globalScope.containsKey(name)) return globalScope.get(name);
        return null;
    }

    private void setVar(String name, Object val, Map<String, Object> scope) {
        if (scope.containsKey(name)) scope.put(name, val);
        else globalScope.put(name, val);
    }

    @SuppressWarnings("unchecked")
    private void setMemberProperty(Object obj, String member, Object value) {
        if (!(obj instanceof Map)) return;
        Map<String, Object> map = (Map<String, Object>) obj;
        String setter = "_set_" + member;
        if (map.containsKey(setter) && map.get(setter) instanceof BuiltinFunction) {
            ((BuiltinFunction) map.get(setter)).call(List.of(value));
        } else {
            map.put(member, value);
        }
    }

    private float toFloat(Object val) {
        if (val instanceof Number) return ((Number) val).floatValue();
        if (val instanceof Boolean) return (Boolean) val ? 1f : 0f;
        if (val instanceof String) {
            try { return Float.parseFloat((String) val); }
            catch (Exception e) { return 0f; }
        }
        return 0f;
    }

    private String toStr(Object val) {
        if (val == null)         return "فارغ";
        if (val instanceof Boolean) return (Boolean) val ? "صحيح" : "خطأ";
        if (val instanceof Float) {
            float f = (Float) val;
            if (f == Math.floor(f) && !Float.isInfinite(f))
                return String.valueOf((int) f);
            return String.valueOf(f);
        }
        return val.toString();
    }

    private boolean isTruthy(Object val) {
        if (val == null)         return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).floatValue() != 0;
        if (val instanceof String) return !((String) val).isEmpty();
        if (val instanceof List)   return !((List<?>) val).isEmpty();
        return true;
    }

    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number)
            return ((Number) a).floatValue() == ((Number) b).floatValue();
        return a.equals(b);
    }

    private Object add(Object a, Object b) {
        if (a instanceof String || b instanceof String)
            return toStr(a) + toStr(b);
        return toFloat(a) + toFloat(b);
    }

    // ── الواجهات الداخلية ─────────────────────────────────────────────────

    public interface BuiltinFunction {
        Object call(List<Object> args);
    }

    public static class ScriptFunction {
        public final String            name;
        public final List<String>      params;
        public final List<ASTNode>     body;
        public final Map<String, Object> closure;

        public ScriptFunction(String name, List<String> params,
                               List<ASTNode> body, Map<String, Object> closure) {
            this.name    = name;
            this.params  = params;
            this.body    = body;
            this.closure = closure;
        }
    }

    public Map<String, Object> getGlobalScope() { return globalScope; }
    public GameEngine           getEngine()      { return engine; }
}
