package com.remi8.remiscript.interpreter;

import android.graphics.Color;
import android.util.Log;

import com.remi8.engine.core.GameEngine;
import com.remi8.engine.core.GameObject;
import com.remi8.engine.physics.PhysicsBody;
import com.remi8.remiscript.parser.RemiScriptLexer;
import com.remi8.remiscript.parser.RemiScriptLexer.Token;
import com.remi8.remiscript.parser.RemiScriptLexer.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * مفسّر لغة remiscript - REMI8
 * لغة برمجة عربية لتطوير الألعاب ثنائية الأبعاد
 *
 * مثال على remiscript:
 * ────────────────────
 * // تحريك اللاعب
 * دالة تحديث(دت) {
 *     إذا (مدخل.زر("يمين")) {
 *         اللاعب.س += 200 * دت
 *     }
 *     إذا (مدخل.زر("يسار")) {
 *         اللاعب.س -= 200 * دت
 *     }
 *     إذا (مدخل.زر("قفز") و اللاعب.علىالأرض) {
 *         اللاعب.قفز(500)
 *     }
 * }
 */
public class RemiScriptInterpreter {

    private static final String TAG = "REMI8_Script";

    private final GameEngine engine;

    // بيئة التنفيذ العالمية
    private final Map<String, Object> globalScope = new HashMap<>();

    // الدوال المعرّفة
    private final Map<String, ScriptFunction> functions = new HashMap<>();

    // السكربتات المحملة
    private final Map<String, ParsedScript> loadedScripts = new HashMap<>();

    // سكربتات الحدث
    private final Map<String, List<String>> eventScripts = new HashMap<>();

    // استثناء الإيقاف والإرجاع
    private static class BreakException extends RuntimeException {}
    private static class ContinueException extends RuntimeException {}
    private static class ReturnException extends RuntimeException {
        final Object value;
        ReturnException(Object value) { this.value = value; }
    }

    public RemiScriptInterpreter(GameEngine engine) {
        this.engine = engine;
        registerBuiltins();
    }

    /**
     * تسجيل الدوال المدمجة
     */
    private void registerBuiltins() {
        // دوال الطباعة
        globalScope.put("طباعة", (BuiltinFunction) args -> {
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) sb.append(toString(arg)).append(" ");
            Log.d(TAG, "[remiscript] " + sb.toString().trim());
            return null;
        });

        globalScope.put("print", globalScope.get("طباعة"));

        // الرياضيات
        globalScope.put("رياضيات", createMathObject());
        globalScope.put("math", globalScope.get("رياضيات"));

        // مدير المدخلات
        globalScope.put("مدخل", createInputObject());
        globalScope.put("input", globalScope.get("مدخل"));

        // مدير الصوت
        globalScope.put("صوت", createAudioObject());
        globalScope.put("audio", globalScope.get("صوت"));

        // مدير المشاهد
        globalScope.put("مشهد", createSceneObject());
        globalScope.put("scene", globalScope.get("مشهد"));

        // دوال الوقت
        globalScope.put("وقت", createTimeObject());

        // دوال عشوائية
        globalScope.put("عشوائي", (BuiltinFunction) args -> {
            if (args.size() >= 2) {
                float min = toFloat(args.get(0));
                float max = toFloat(args.get(1));
                return min + (float) (Math.random() * (max - min));
            }
            return (float) Math.random();
        });
        globalScope.put("random", globalScope.get("عشوائي"));
    }

    /**
     * كائن الرياضيات المدمج
     */
    private Map<String, Object> createMathObject() {
        Map<String, Object> math = new HashMap<>();
        math.put("pi", (float) Math.PI);
        math.put("ط", (float) Math.PI);
        math.put("جذر", (BuiltinFunction) args -> (float) Math.sqrt(toFloat(args.get(0))));
        math.put("مطلق", (BuiltinFunction) args -> Math.abs(toFloat(args.get(0))));
        math.put("أقصى", (BuiltinFunction) args -> Math.max(toFloat(args.get(0)), toFloat(args.get(1))));
        math.put("أدنى", (BuiltinFunction) args -> Math.min(toFloat(args.get(0)), toFloat(args.get(1))));
        math.put("تقريب", (BuiltinFunction) args -> (float) Math.round(toFloat(args.get(0))));
        math.put("جيب", (BuiltinFunction) args -> (float) Math.sin(Math.toRadians(toFloat(args.get(0)))));
        math.put("جيب_تمام", (BuiltinFunction) args -> (float) Math.cos(Math.toRadians(toFloat(args.get(0)))));
        math.put("قوة", (BuiltinFunction) args -> (float) Math.pow(toFloat(args.get(0)), toFloat(args.get(1))));
        math.put("sqrt", math.get("جذر"));
        math.put("abs", math.get("مطلق"));
        math.put("max", math.get("أقصى"));
        math.put("min", math.get("أدنى"));
        math.put("round", math.get("تقريب"));
        math.put("sin", math.get("جيب"));
        math.put("cos", math.get("جيب_تمام"));
        math.put("pow", math.get("قوة"));
        return math;
    }

    /**
     * كائن المدخلات المدمج
     */
    private Map<String, Object> createInputObject() {
        Map<String, Object> input = new HashMap<>();
        input.put("زر", (BuiltinFunction) args -> {
            if (args.isEmpty()) return false;
            return engine.getInputManager().isButtonPressed(toString(args.get(0)));
        });
        input.put("لمس", (BuiltinFunction) args -> {
            return !engine.getInputManager().getTouchPoints().isEmpty();
        });
        input.put("إيماءة", (BuiltinFunction) args -> {
            return engine.getInputManager().getLastSwipeDirection();
        });
        input.put("جويستيك_س", (BuiltinFunction) args -> engine.getInputManager().getJoystickX());
        input.put("جويستيك_ص", (BuiltinFunction) args -> engine.getInputManager().getJoystickY());
        input.put("button", input.get("زر"));
        input.put("touch", input.get("لمس"));
        return input;
    }

    /**
     * كائن الصوت المدمج
     */
    private Map<String, Object> createAudioObject() {
        Map<String, Object> audio = new HashMap<>();
        audio.put("شغّل", (BuiltinFunction) args -> {
            if (!args.isEmpty()) engine.getAudioManager().playSound(toString(args.get(0)));
            return null;
        });
        audio.put("موسيقى", (BuiltinFunction) args -> {
            if (!args.isEmpty()) engine.getAudioManager().playMusic(toString(args.get(0)), true);
            return null;
        });
        audio.put("إيقاف", (BuiltinFunction) args -> {
            engine.getAudioManager().stopMusic();
            return null;
        });
        audio.put("صوت_رئيسي", (BuiltinFunction) args -> {
            if (!args.isEmpty()) engine.getAudioManager().setMasterVolume(toFloat(args.get(0)));
            return null;
        });
        audio.put("play", audio.get("شغّل"));
        audio.put("music", audio.get("موسيقى"));
        audio.put("stop", audio.get("إيقاف"));
        return audio;
    }

    /**
     * كائن المشهد المدمج
     */
    private Map<String, Object> createSceneObject() {
        Map<String, Object> scene = new HashMap<>();
        scene.put("ابحث", (BuiltinFunction) args -> {
            if (args.isEmpty()) return null;
            return engine.getSceneManager().findObject(toString(args.get(0)));
        });
        scene.put("تبديل", (BuiltinFunction) args -> {
            if (!args.isEmpty()) engine.getSceneManager().switchScene(toString(args.get(0)));
            return null;
        });
        scene.put("إضافة", (BuiltinFunction) args -> {
            if (!args.isEmpty() && args.get(0) instanceof GameObject) {
                engine.getSceneManager().addObject((GameObject) args.get(0));
            }
            return null;
        });
        scene.put("find", scene.get("ابحث"));
        scene.put("switch", scene.get("تبديل"));
        return scene;
    }

    /**
     * كائن الوقت المدمج
     */
    private Map<String, Object> createTimeObject() {
        Map<String, Object> time = new HashMap<>();
        time.put("الآن", (BuiltinFunction) args -> (float) (System.currentTimeMillis() / 1000.0));
        time.put("إطارات", (BuiltinFunction) args -> engine.getCurrentFPS());
        time.put("now", time.get("الآن"));
        time.put("fps", time.get("إطارات"));
        return time;
    }

    /**
     * تنفيذ كود remiscript
     */
    public Object execute(String code, Map<String, Object> localScope) {
        try {
            RemiScriptLexer lexer = new RemiScriptLexer(code);
            List<Token> tokens = lexer.tokenize();
            RemiScriptParser parser = new RemiScriptParser(tokens);
            List<ASTNode> statements = parser.parseProgram();
            return executeBlock(statements, localScope != null ? localScope : new HashMap<>());
        } catch (ReturnException r) {
            return r.value;
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تنفيذ remiscript: " + e.getMessage());
            return null;
        }
    }

    /**
     * تنفيذ ملف سكربت محمّل
     */
    public void executeScript(String name, String code) {
        try {
            RemiScriptLexer lexer = new RemiScriptLexer(code);
            List<Token> tokens = lexer.tokenize();
            RemiScriptParser parser = new RemiScriptParser(tokens);
            List<ASTNode> statements = parser.parseProgram();
            executeBlock(statements, globalScope);
            Log.d(TAG, "تم تنفيذ السكربت: " + name);
        } catch (Exception e) {
            Log.e(TAG, "خطأ في السكربت '" + name + "': " + e.getMessage());
        }
    }

    /**
     * تحديث كل إطار - تشغيل دالة تحديث() في السكربتات
     */
    public void update(float deltaTime) {
        // تشغيل دالة تحديث في السكربتات المرتبطة بكائنات اللعبة
        if (engine.getSceneManager().getActiveScene() != null) {
            for (GameObject obj : engine.getSceneManager().getActiveScene().getObjects()) {
                for (String scriptName : obj.getAttachedScripts()) {
                    callObjectUpdate(obj, scriptName, deltaTime);
                }
            }
        }
    }

    private void callObjectUpdate(GameObject obj, String scriptName, float deltaTime) {
        ScriptFunction updateFunc = functions.get(scriptName + ".تحديث");
        if (updateFunc == null) updateFunc = functions.get(scriptName + ".update");
        if (updateFunc != null) {
            Map<String, Object> scope = new HashMap<>(globalScope);
            scope.put("هذا", createGameObjectProxy(obj));
            scope.put("this", scope.get("هذا"));
            scope.put("دت", deltaTime);
            scope.put("dt", deltaTime);
            try {
                callFunction(updateFunc, List.of((Object) deltaTime), scope);
            } catch (Exception e) {
                Log.e(TAG, "خطأ في دالة التحديث: " + e.getMessage());
            }
        }
    }

    /**
     * إنشاء وكيل (proxy) لكائن اللعبة في remiscript
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createGameObjectProxy(GameObject obj) {
        Map<String, Object> proxy = new HashMap<>();

        // الخصائص الأساسية
        proxy.put("س", obj.x);
        proxy.put("ص", obj.y);
        proxy.put("عرض", obj.width);
        proxy.put("ارتفاع", obj.height);
        proxy.put("دوران", obj.rotation);
        proxy.put("نشط", obj.isActive());
        proxy.put("مرئي", obj.isVisible());
        proxy.put("اسم", obj.getName());
        proxy.put("x", obj.x);
        proxy.put("y", obj.y);

        // الجسم الفيزيائي
        PhysicsBody body = (PhysicsBody) obj.getComponent("physics");
        if (body != null) {
            proxy.put("سرعة_س", body.velocityX);
            proxy.put("سرعة_ص", body.velocityY);
            proxy.put("علىالأرض", body.isGrounded);
            proxy.put("velX", body.velocityX);
            proxy.put("velY", body.velocityY);
            proxy.put("grounded", body.isGrounded);

            proxy.put("قفز", (BuiltinFunction) args -> {
                float force = args.isEmpty() ? 500f : toFloat(args.get(0));
                body.jump(force);
                return null;
            });
            proxy.put("دفع", (BuiltinFunction) args -> {
                float fx = args.size() > 0 ? toFloat(args.get(0)) : 0;
                float fy = args.size() > 1 ? toFloat(args.get(1)) : 0;
                body.applyImpulse(fx, fy);
                return null;
            });
            proxy.put("jump", proxy.get("قفز"));
            proxy.put("push", proxy.get("دفع"));
        }

        // دوال التحكم
        proxy.put("تحديث_س", (BuiltinFunction) args -> {
            if (!args.isEmpty()) obj.x = toFloat(args.get(0));
            return obj.x;
        });
        proxy.put("تحديث_ص", (BuiltinFunction) args -> {
            if (!args.isEmpty()) obj.y = toFloat(args.get(0));
            return obj.y;
        });
        proxy.put("تعيين_لون", (BuiltinFunction) args -> {
            if (!args.isEmpty()) obj.setProperty("color", toString(args.get(0)));
            return null;
        });
        proxy.put("تعيين_نص", (BuiltinFunction) args -> {
            if (!args.isEmpty()) obj.setProperty("text", toString(args.get(0)));
            return null;
        });
        proxy.put("إخفاء", (BuiltinFunction) args -> { obj.setVisible(false); return null; });
        proxy.put("إظهار", (BuiltinFunction) args -> { obj.setVisible(true); return null; });
        proxy.put("تفعيل", (BuiltinFunction) args -> { obj.setActive(true); return null; });
        proxy.put("تعطيل", (BuiltinFunction) args -> { obj.setActive(false); return null; });
        proxy.put("خاصية", (BuiltinFunction) args -> {
            if (args.size() >= 2) obj.setProperty(toString(args.get(0)), args.get(1));
            else if (args.size() == 1) return obj.getProperty(toString(args.get(0)));
            return null;
        });
        proxy.put("تصادم", (BuiltinFunction) args -> {
            if (!args.isEmpty() && args.get(0) instanceof Map) {
                return false; // سيتم تطويره
            }
            return false;
        });
        proxy.put("hide", proxy.get("إخفاء"));
        proxy.put("show", proxy.get("إظهار"));

        return proxy;
    }

    // ─── تنفيذ الكتل والعبارات ───

    private Object executeBlock(List<ASTNode> statements, Map<String, Object> scope) {
        Object result = null;
        for (ASTNode stmt : statements) {
            result = executeStatement(stmt, scope);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object executeStatement(ASTNode node, Map<String, Object> scope) {
        if (node == null) return null;

        switch (node.type) {
            case "متغير":
            case "ثابت": {
                Object value = node.children.size() > 0 ? evaluate(node.children.get(0), scope) : null;
                scope.put(node.value, value);
                return null;
            }

            case "تعيين": {
                Object value = evaluate(node.children.get(1), scope);
                String varName = node.children.get(0).value;
                if (varName.contains(".")) {
                    setNestedProperty(varName, value, scope);
                } else {
                    if (scope.containsKey(varName)) scope.put(varName, value);
                    else globalScope.put(varName, value);
                }
                return value;
            }

            case "إذا": {
                Object condition = evaluate(node.children.get(0), scope);
                if (isTruthy(condition)) {
                    return executeBlock(node.block, new HashMap<>(scope));
                } else if (node.elseBlock != null) {
                    return executeBlock(node.elseBlock, new HashMap<>(scope));
                }
                return null;
            }

            case "طالما": {
                while (isTruthy(evaluate(node.children.get(0), scope))) {
                    try {
                        executeBlock(node.block, new HashMap<>(scope));
                    } catch (BreakException e) {
                        break;
                    } catch (ContinueException e) {
                        // استمر
                    }
                }
                return null;
            }

            case "لكل": {
                String varName = node.value;
                Object iterable = evaluate(node.children.get(0), scope);
                if (iterable instanceof List) {
                    for (Object item : (List<?>) iterable) {
                        Map<String, Object> loopScope = new HashMap<>(scope);
                        loopScope.put(varName, item);
                        try {
                            executeBlock(node.block, loopScope);
                        } catch (BreakException e) {
                            break;
                        } catch (ContinueException e) {
                            // استمر
                        }
                    }
                }
                return null;
            }

            case "دالة": {
                ScriptFunction func = new ScriptFunction(node.value, node.params, node.block, new HashMap<>(scope));
                functions.put(node.value, func);
                scope.put(node.value, func);
                return null;
            }

            case "إرجاع": {
                Object value = node.children.isEmpty() ? null : evaluate(node.children.get(0), scope);
                throw new ReturnException(value);
            }

            case "إيقاف":
                throw new BreakException();

            case "استمر":
                throw new ContinueException();

            case "استدعاء":
                return evaluate(node, scope);

            case "تعبير":
                return evaluate(node.children.get(0), scope);

            default:
                return evaluate(node, scope);
        }
    }

    @SuppressWarnings("unchecked")
    private Object evaluate(ASTNode node, Map<String, Object> scope) {
        if (node == null) return null;

        switch (node.type) {
            case "عدد":
                try { return Float.parseFloat(node.value); }
                catch (NumberFormatException e) { return 0f; }

            case "نص":
                return node.value;

            case "صحيح":
                return true;

            case "خطأ":
                return false;

            case "فارغ":
                return null;

            case "قائمة": {
                List<Object> list = new ArrayList<>();
                for (ASTNode child : node.children) list.add(evaluate(child, scope));
                return list;
            }

            case "قاموس": {
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i + 1 < node.children.size(); i += 2) {
                    String key = node.children.get(i).value;
                    Object val = evaluate(node.children.get(i + 1), scope);
                    map.put(key, val);
                }
                return map;
            }

            case "معرف": {
                String name = node.value;
                if (scope.containsKey(name)) return scope.get(name);
                if (globalScope.containsKey(name)) return globalScope.get(name);
                return null;
            }

            case "وصول_عضو": {
                Object obj = evaluate(node.children.get(0), scope);
                String member = node.value;
                if (obj instanceof Map) return ((Map<String, Object>) obj).get(member);
                if (obj instanceof GameObject) {
                    Map<String, Object> proxy = createGameObjectProxy((GameObject) obj);
                    return proxy.get(member);
                }
                return null;
            }

            case "فهرس": {
                Object obj = evaluate(node.children.get(0), scope);
                Object idx = evaluate(node.children.get(1), scope);
                if (obj instanceof List && idx instanceof Number) {
                    int i = ((Number) idx).intValue();
                    List<?> list = (List<?>) obj;
                    return (i >= 0 && i < list.size()) ? list.get(i) : null;
                }
                if (obj instanceof Map) return ((Map<?, ?>) obj).get(toString(idx));
                return null;
            }

            case "استدعاء": {
                Object callee;
                Object thisObj = null;

                if (node.children.get(0).type.equals("وصول_عضو")) {
                    thisObj = evaluate(node.children.get(0).children.get(0), scope);
                    String methodName = node.children.get(0).value;
                    if (thisObj instanceof Map) {
                        callee = ((Map<?, ?>) thisObj).get(methodName);
                    } else {
                        callee = null;
                    }
                } else {
                    callee = evaluate(node.children.get(0), scope);
                }

                List<Object> args = new ArrayList<>();
                for (int i = 1; i < node.children.size(); i++) {
                    args.add(evaluate(node.children.get(i), scope));
                }

                if (callee instanceof BuiltinFunction) {
                    return ((BuiltinFunction) callee).call(args);
                }
                if (callee instanceof ScriptFunction) {
                    return callFunction((ScriptFunction) callee, args, scope);
                }
                return null;
            }

            // عمليات ثنائية
            case "زائد": return add(evaluate(node.children.get(0), scope), evaluate(node.children.get(1), scope));
            case "ناقص": return toFloat(evaluate(node.children.get(0), scope)) - toFloat(evaluate(node.children.get(1), scope));
            case "ضرب":  return toFloat(evaluate(node.children.get(0), scope)) * toFloat(evaluate(node.children.get(1), scope));
            case "قسمة": {
                float b = toFloat(evaluate(node.children.get(1), scope));
                return b != 0 ? toFloat(evaluate(node.children.get(0), scope)) / b : 0f;
            }
            case "باقي": return toFloat(evaluate(node.children.get(0), scope)) % toFloat(evaluate(node.children.get(1), scope));
            case "يساوي_يساوي": return equals(evaluate(node.children.get(0), scope), evaluate(node.children.get(1), scope));
            case "لا_يساوي": return !equals(evaluate(node.children.get(0), scope), evaluate(node.children.get(1), scope));
            case "أكبر": return toFloat(evaluate(node.children.get(0), scope)) > toFloat(evaluate(node.children.get(1), scope));
            case "أصغر": return toFloat(evaluate(node.children.get(0), scope)) < toFloat(evaluate(node.children.get(1), scope));
            case "أكبر_يساوي": return toFloat(evaluate(node.children.get(0), scope)) >= toFloat(evaluate(node.children.get(1), scope));
            case "أصغر_يساوي": return toFloat(evaluate(node.children.get(0), scope)) <= toFloat(evaluate(node.children.get(1), scope));
            case "و": return isTruthy(evaluate(node.children.get(0), scope)) && isTruthy(evaluate(node.children.get(1), scope));
            case "أو":  return isTruthy(evaluate(node.children.get(0), scope)) || isTruthy(evaluate(node.children.get(1), scope));
            case "ليس": return !isTruthy(evaluate(node.children.get(0), scope));
            case "سالب": return -toFloat(evaluate(node.children.get(0), scope));

            default: return null;
        }
    }

    private Object callFunction(ScriptFunction func, List<Object> args, Map<String, Object> callerScope) {
        Map<String, Object> funcScope = new HashMap<>(func.closure);
        for (int i = 0; i < func.params.size(); i++) {
            funcScope.put(func.params.get(i), i < args.size() ? args.get(i) : null);
        }
        try {
            return executeBlock(func.body, funcScope);
        } catch (ReturnException r) {
            return r.value;
        }
    }

    @SuppressWarnings("unchecked")
    private void setNestedProperty(String path, Object value, Map<String, Object> scope) {
        String[] parts = path.split("\\.");
        Object obj = scope.containsKey(parts[0]) ? scope.get(parts[0]) : globalScope.get(parts[0]);
        if (obj instanceof Map && parts.length == 2) {
            ((Map<String, Object>) obj).put(parts[1], value);
        }
    }

    // ─── أدوات مساعدة ───

    private float toFloat(Object val) {
        if (val instanceof Number) return ((Number) val).floatValue();
        if (val instanceof Boolean) return (Boolean) val ? 1f : 0f;
        if (val instanceof String) {
            try { return Float.parseFloat((String) val); } catch (Exception e) { return 0f; }
        }
        return 0f;
    }

    private String toString(Object val) {
        if (val == null) return "فارغ";
        if (val instanceof Boolean) return (Boolean) val ? "صحيح" : "خطأ";
        if (val instanceof Float) {
            float f = (Float) val;
            if (f == Math.floor(f)) return String.valueOf((int) f);
            return String.valueOf(f);
        }
        return val.toString();
    }

    private boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).floatValue() != 0;
        if (val instanceof String) return !((String) val).isEmpty();
        if (val instanceof List) return !((List<?>) val).isEmpty();
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
            return toString(a) + toString(b);
        return toFloat(a) + toFloat(b);
    }

    // الواجهات الداخلية
    public interface BuiltinFunction {
        Object call(List<Object> args);
    }

    public static class ScriptFunction {
        final String name;
        final List<String> params;
        final List<ASTNode> body;
        final Map<String, Object> closure;

        ScriptFunction(String name, List<String> params, List<ASTNode> body, Map<String, Object> closure) {
            this.name = name; this.params = params; this.body = body; this.closure = closure;
        }
    }

    public Map<String, Object> getGlobalScope() { return globalScope; }
    public GameEngine getEngine() { return engine; }
}
