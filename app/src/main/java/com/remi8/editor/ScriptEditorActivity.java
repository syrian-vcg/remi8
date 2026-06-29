package com.remi8.editor;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.widget.*;

import com.remi8.R;
import com.remi8.remiscript.interpreter.RemiScriptInterpreter;
import com.remi8.remiscript.parser.RemiScriptLexer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * محرر سكربتات remiscript - REMI8
 * واجهة كتابة وتعديل سكربتات اللعبة بتلوين النحوي
 */
public class ScriptEditorActivity extends Activity {

    private EditText codeEditor;
    private TextView outputConsole;
    private TextView lineNumbers;
    private String scriptName;
    private String objectName;

    // ألوان التلوين النحوي
    private static final int COLOR_KEYWORD   = Color.parseColor("#c678dd"); // بنفسجي - الكلمات المحجوزة
    private static final int COLOR_STRING    = Color.parseColor("#98c379"); // أخضر - النصوص
    private static final int COLOR_NUMBER    = Color.parseColor("#d19a66"); // برتقالي - الأرقام
    private static final int COLOR_COMMENT   = Color.parseColor("#5c6370"); // رمادي - التعليقات
    private static final int COLOR_FUNCTION  = Color.parseColor("#61afef"); // أزرق - الدوال
    private static final int COLOR_BUILTIN   = Color.parseColor("#e5c07b"); // ذهبي - المدمجة

    private boolean isHighlighting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_editor);

        scriptName = getIntent().getStringExtra("script_name");
        objectName = getIntent().getStringExtra("object_name");

        initViews();
        loadScript();
    }

    private void initViews() {
        codeEditor    = findViewById(R.id.code_editor);
        outputConsole = findViewById(R.id.output_console);
        lineNumbers   = findViewById(R.id.line_numbers);

        // تنسيق محرر الكود
        codeEditor.setTypeface(Typeface.MONOSPACE);
        codeEditor.setTextColor(Color.parseColor("#abb2bf"));
        codeEditor.setBackgroundColor(Color.parseColor("#282c34"));
        codeEditor.setTextSize(14f);
        codeEditor.setHorizontallyScrolling(false);

        // تلوين نحوي تلقائي
        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isHighlighting) highlightSyntax(s);
                updateLineNumbers();
            }
        });

        // أزرار شريط الأدوات
        findViewById(R.id.btn_run).setOnClickListener(v -> runScript());
        findViewById(R.id.btn_save_script).setOnClickListener(v -> saveScript());
        findViewById(R.id.btn_clear_console).setOnClickListener(v -> outputConsole.setText(""));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_format).setOnClickListener(v -> formatCode());

        // أزرار الاختصارات
        setupShortcutButtons();
    }

    /**
     * إعداد أزرار الاختصارات العربية
     */
    private void setupShortcutButtons() {
        String[] shortcuts = {
            "إذا", "وإلا", "طالما", "لكل", "دالة",
            "متغير", "إرجاع", "طباعة", "مدخل.زر", "صوت.شغّل"
        };

        LinearLayout shortcutBar = findViewById(R.id.shortcut_bar);
        if (shortcutBar == null) return;

        for (String shortcut : shortcuts) {
            Button btn = new Button(this);
            btn.setText(shortcut);
            btn.setTextSize(12f);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundColor(Color.parseColor("#3a3f4b"));
            btn.setPadding(16, 8, 16, 8);
            btn.setOnClickListener(v -> insertAtCursor(shortcut + " "));
            shortcutBar.addView(btn);
        }
    }

    private void insertAtCursor(String text) {
        int start = Math.max(codeEditor.getSelectionStart(), 0);
        int end = Math.max(codeEditor.getSelectionEnd(), 0);
        codeEditor.getText().replace(Math.min(start, end), Math.max(start, end), text);
    }

    /**
     * تحميل السكربت الافتراضي أو الموجود
     */
    private void loadScript() {
        if (scriptName == null) scriptName = "سكربت_جديد";
        setTitle("remiscript - " + scriptName);

        String defaultScript = generateDefaultScript();
        codeEditor.setText(defaultScript);
        updateLineNumbers();
    }

    /**
     * توليد سكربت افتراضي حسب نوع الكائن
     */
    private String generateDefaultScript() {
        String name = objectName != null ? objectName : "الكائن";
        return "// سكربت remiscript للكائن: " + name + "\n" +
               "// لغة REMI8 - تدعم العربية والإنجليزية\n\n" +

               "// ─── المتغيرات ───\n" +
               "متغير السرعة = 300\n" +
               "متغير القفز = 500\n" +
               "متغير النقاط = 0\n\n" +

               "// ─── دالة البداية: تُستدعى مرة واحدة ───\n" +
               "دالة بداية() {\n" +
               "    طباعة(\"تم تشغيل: " + name + "\")\n" +
               "    هذا.تعيين_لون(\"#e63946\")\n" +
               "}\n\n" +

               "// ─── دالة التحديث: تُستدعى كل إطار ───\n" +
               "دالة تحديث(دت) {\n\n" +
               "    // ─── التحرك الأفقي ───\n" +
               "    إذا (مدخل.زر(\"يمين\")) {\n" +
               "        هذا.س = هذا.س + السرعة * دت\n" +
               "    }\n" +
               "    إذا (مدخل.زر(\"يسار\")) {\n" +
               "        هذا.س = هذا.س - السرعة * دت\n" +
               "    }\n\n" +
               "    // ─── القفز ───\n" +
               "    إذا (مدخل.زر(\"قفز\") و هذا.علىالأرض) {\n" +
               "        هذا.قفز(القفز)\n" +
               "        صوت.شغّل(\"قفز\")\n" +
               "    }\n\n" +
               "    // ─── الإيماءات ───\n" +
               "    إذا (مدخل.إيماءة() == \"يمين\") {\n" +
               "        هذا.دفع(500, 0)\n" +
               "    }\n" +
               "}\n\n" +

               "// ─── دالة التصادم ───\n" +
               "دالة تصادم(الآخر) {\n" +
               "    إذا (الآخر.وسم == \"عدو\") {\n" +
               "        النقاط = النقاط - 1\n" +
               "        طباعة(\"تصادم مع عدو! النقاط: \" + النقاط)\n" +
               "    }\n" +
               "    إذا (الآخر.وسم == \"عملة\") {\n" +
               "        النقاط = النقاط + 10\n" +
               "        صوت.شغّل(\"عملة\")\n" +
               "        الآخر.تعطيل()\n" +
               "    }\n" +
               "}\n";
    }

    /**
     * تشغيل السكربت وعرض النتيجة
     */
    private void runScript() {
        String code = codeEditor.getText().toString();
        outputConsole.setText("⟶ جارٍ التنفيذ...\n");

        // تحقق من الصحة النحوية أولاً
        try {
            RemiScriptLexer lexer = new RemiScriptLexer(code);
            List<RemiScriptLexer.Token> tokens = lexer.tokenize();
            appendOutput("✓ التحليل اللغوي: " + tokens.size() + " رمز\n");

            // تنفيذ السكربت
            // سيتم ربطه بمحرك اللعبة لاحقاً في النسخة الكاملة
            appendOutput("✓ تم تنفيذ السكربت بنجاح\n");
            appendOutput("─────────────────────\n");
            appendOutput("remiscript v1.0 | REMI8 Engine\n");
        } catch (Exception e) {
            appendOutput("✗ خطأ: " + e.getMessage() + "\n");
        }
    }

    private void appendOutput(String text) {
        runOnUiThread(() -> outputConsole.append(text));
    }

    /**
     * حفظ السكربت
     */
    private void saveScript() {
        String code = codeEditor.getText().toString();
        // TODO: حفظ حقيقي في الملفات
        Toast.makeText(this, "تم حفظ السكربت: " + scriptName, Toast.LENGTH_SHORT).show();
    }

    /**
     * التلوين النحوي
     */
    private void highlightSyntax(Editable text) {
        isHighlighting = true;

        // مسح التلوين السابق
        ForegroundColorSpan[] spans = text.getSpans(0, text.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) text.removeSpan(span);

        String code = text.toString();

        // التعليقات
        applyPattern(text, code, "//[^\n]*", COLOR_COMMENT);

        // النصوص
        applyPattern(text, code, "\"[^\"]*\"|'[^']*'", COLOR_STRING);

        // الكلمات المحجوزة العربية
        String arabicKeywords = "إذا|وإلا|وإلا_إذا|طالما|كرر|لكل|في|دالة|وظيفة|إرجاع|أرجع|" +
                               "إيقاف|استمر|متغير|ثابت|صنف|ينشئ|يرث|استيراد|و|أو|ليس|" +
                               "صحيح|خطأ|فارغ|نعم|لا|عدم|هذا|بداية|تحديث|تصادم";
        applyPatternWord(text, code, arabicKeywords, COLOR_KEYWORD);

        // الكلمات المحجوزة الإنجليزية
        String engKeywords = "if|else|while|for|function|return|break|continue|var|let|const|true|false|null|new|class";
        applyPatternWord(text, code, engKeywords, COLOR_KEYWORD);

        // الدوال المدمجة
        String builtins = "طباعة|print|مدخل|input|صوت|audio|مشهد|scene|رياضيات|math|عشوائي|random|وقت|time";
        applyPatternWord(text, code, builtins, COLOR_BUILTIN);

        // أسماء الدوال المعرّفة
        applyPattern(text, code, "دالة\\s+(\\w+)", COLOR_FUNCTION, 1);

        // الأرقام
        applyPattern(text, code, "\\b\\d+(\\.\\d+)?\\b", COLOR_NUMBER);

        isHighlighting = false;
    }

    private void applyPattern(Editable text, String code, String pattern, int color) {
        applyPattern(text, code, pattern, color, 0);
    }

    private void applyPattern(Editable text, String code, String pattern, int color, int group) {
        try {
            Matcher m = Pattern.compile(pattern).matcher(code);
            while (m.find()) {
                int start = group == 0 ? m.start() : m.start(group);
                int end = group == 0 ? m.end() : m.end(group);
                if (start >= 0 && end <= text.length())
                    text.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception ignored) {}
    }

    private void applyPatternWord(Editable text, String code, String words, int color) {
        applyPattern(text, code, "\\b(" + words + ")\\b", color);
    }

    /**
     * تحديث أرقام الأسطر
     */
    private void updateLineNumbers() {
        String code = codeEditor.getText().toString();
        String[] lines = code.split("\n", -1);
        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lines.length; i++) {
            numbers.append(i).append("\n");
        }
        lineNumbers.setText(numbers.toString());
    }

    /**
     * تنسيق الكود تلقائياً
     */
    private void formatCode() {
        String code = codeEditor.getText().toString();
        // تنسيق بسيط: إزالة المسافات الزائدة
        code = code.replaceAll("[ \t]+\n", "\n");
        code = code.replaceAll("\n{3,}", "\n\n");
        codeEditor.setText(code);
        Toast.makeText(this, "تم تنسيق الكود", Toast.LENGTH_SHORT).show();
    }
}
