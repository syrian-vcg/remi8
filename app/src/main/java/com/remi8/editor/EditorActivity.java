package com.remi8.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.remi8.R;
import com.remi8.engine.core.GameActivity;
import com.remi8.engine.core.GameEngine;
import com.remi8.engine.core.GameObject;
import com.remi8.engine.scene.Scene;
import com.remi8.export.ExportActivity;
import com.remi8.remiscript.interpreter.RemiScriptInterpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * نشاط المحرر الرئيسي - REMI8
 * واجهة تطوير الألعاب ثنائية الأبعاد
 */
public class EditorActivity extends Activity {

    // المكونات الرئيسية
    private GameEngine gameEngine;
    private EditorView editorView;

    // عناصر الواجهة
    private TextView projectNameText;
    private TextView fpsText;
    private ListView objectListView;
    private LinearLayout propertiesPanel;
    private LinearLayout toolsPanel;

    // حالة المحرر
    private String currentProjectPath = null;
    private String currentProjectName = "مشروع_جديد";
    private boolean isPlaying = false;

    // قائمة الكائنات في المشهد الحالي
    private final List<GameObject> sceneObjects = new ArrayList<>();
    private ArrayAdapter<String> objectListAdapter;
    private final List<String> objectNames = new ArrayList<>();

    // الكائن المحدد
    private GameObject selectedObject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        initViews();
        initEngine();
        setupObjectList();
        setupDefaultScene();
    }

    private void initViews() {
        projectNameText = findViewById(R.id.project_name);
        fpsText = findViewById(R.id.fps_text);
        editorView = findViewById(R.id.editor_view);
        objectListView = findViewById(R.id.object_list);
        propertiesPanel = findViewById(R.id.properties_panel);
        toolsPanel = findViewById(R.id.tools_panel);

        // أزرار شريط الأدوات
        findViewById(R.id.btn_play).setOnClickListener(v -> togglePlay());
        findViewById(R.id.btn_stop).setOnClickListener(v -> stopPlay());
        findViewById(R.id.btn_new_scene).setOnClickListener(v -> newScene());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveProject());
        findViewById(R.id.btn_export).setOnClickListener(v -> exportGame());
        findViewById(R.id.btn_add_object).setOnClickListener(v -> showAddObjectDialog());
        findViewById(R.id.btn_script_editor).setOnClickListener(v -> openScriptEditor());
        findViewById(R.id.btn_settings).setOnClickListener(v -> openSettings());

        projectNameText.setText(currentProjectName);
    }

    private void initEngine() {
        gameEngine = new GameEngine(this);
        gameEngine.setGameName(currentProjectName);
        gameEngine.setListener(new GameEngine.GameEngineListener() {
            @Override
            public void onEngineStarted() {}
            @Override
            public void onEngineStopped() {}
            @Override
            public void onFPSUpdate(int fps) {
                runOnUiThread(() -> fpsText.setText("FPS: " + fps));
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                    Toast.makeText(EditorActivity.this, "خطأ: " + error, Toast.LENGTH_SHORT).show());
            }
        });

        editorView.setGameEngine(gameEngine);
    }

    private void setupObjectList() {
        objectListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, objectNames);
        objectListView.setAdapter(objectListAdapter);
        objectListView.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < sceneObjects.size()) {
                selectObject(sceneObjects.get(pos));
            }
        });
        objectListView.setOnItemLongClickListener((parent, view, pos, id) -> {
            if (pos < sceneObjects.size()) {
                showObjectContextMenu(sceneObjects.get(pos));
            }
            return true;
        });
    }

    private void setupDefaultScene() {
        Scene scene = gameEngine.getSceneManager().createEmptyScene("مشهد_1");
        gameEngine.getSceneManager().setActiveScene(scene);

        // إضافة كائنات افتراضية للتجربة
        GameObject ground = new GameObject("أرضية");
        ground.x = 0;
        ground.y = 600;
        ground.width = 1280;
        ground.height = 50;
        ground.setProperty("color", "#4a4a6a");
        ground.addTag("أرضية");
        addObjectToScene(ground);

        GameObject player = new GameObject("لاعب");
        player.x = 200;
        player.y = 500;
        player.width = 60;
        player.height = 80;
        player.setProperty("color", "#e63946");
        player.addTag("لاعب");
        addObjectToScene(player);

        gameEngine.start();
    }

    /**
     * إضافة كائن للمشهد وقائمة العرض
     */
    private void addObjectToScene(GameObject obj) {
        sceneObjects.add(obj);
        objectNames.add(obj.getName());
        gameEngine.getSceneManager().addObject(obj);
        objectListAdapter.notifyDataSetChanged();
    }

    /**
     * تشغيل / إيقاف مؤقت للمحاكاة
     */
    private void togglePlay() {
        if (!isPlaying) {
            isPlaying = true;
            gameEngine.resume();
            ((Button) findViewById(R.id.btn_play)).setText("⏸ إيقاف مؤقت");
            Toast.makeText(this, "جارٍ تشغيل اللعبة...", Toast.LENGTH_SHORT).show();
        } else {
            isPlaying = false;
            gameEngine.pause();
            ((Button) findViewById(R.id.btn_play)).setText("▶ تشغيل");
        }
    }

    private void stopPlay() {
        isPlaying = false;
        gameEngine.pause();
        ((Button) findViewById(R.id.btn_play)).setText("▶ تشغيل");
    }

    /**
     * تحديد كائن
     */
    private void selectObject(GameObject obj) {
        selectedObject = obj;
        editorView.setSelectedObject(obj);
        showObjectProperties(obj);
        Toast.makeText(this, "تم تحديد: " + obj.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * عرض خصائص الكائن
     */
    private void showObjectProperties(GameObject obj) {
        propertiesPanel.removeAllViews();

        // الاسم
        addPropertyField("الاسم", obj.getName(), value -> obj.setName(value));

        // الموضع
        addPropertyField("س (X)", String.valueOf(obj.x), value -> {
            try { obj.x = Float.parseFloat(value); } catch (Exception ignored) {}
        });
        addPropertyField("ص (Y)", String.valueOf(obj.y), value -> {
            try { obj.y = Float.parseFloat(value); } catch (Exception ignored) {}
        });

        // الحجم
        addPropertyField("العرض", String.valueOf(obj.width), value -> {
            try { obj.width = Float.parseFloat(value); } catch (Exception ignored) {}
        });
        addPropertyField("الارتفاع", String.valueOf(obj.height), value -> {
            try { obj.height = Float.parseFloat(value); } catch (Exception ignored) {}
        });

        // الدوران
        addPropertyField("الدوران", String.valueOf(obj.rotation), value -> {
            try { obj.rotation = Float.parseFloat(value); } catch (Exception ignored) {}
        });

        // اللون
        Object color = obj.getProperty("color");
        addPropertyField("اللون", color != null ? color.toString() : "#ffffff", value -> {
            obj.setProperty("color", value);
        });

        // النص
        Object text = obj.getProperty("text");
        addPropertyField("نص", text != null ? text.toString() : "", value -> {
            if (!value.isEmpty()) obj.setProperty("text", value);
        });

        // زر السكربت
        Button scriptBtn = new Button(this);
        scriptBtn.setText("✏ تحرير سكربت");
        scriptBtn.setOnClickListener(v -> openScriptEditorForObject(obj));
        propertiesPanel.addView(scriptBtn);

        // زر الفيزياء
        Button physicsBtn = new Button(this);
        physicsBtn.setText("⚙ إضافة فيزياء");
        physicsBtn.setOnClickListener(v -> addPhysicsToObject(obj));
        propertiesPanel.addView(physicsBtn);
    }

    private void addPropertyField(String label, String value, PropertyChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(4, 4, 4, 4);

        TextView lbl = new TextView(this);
        lbl.setText(label + ": ");
        lbl.setTextColor(Color.WHITE);
        lbl.setMinWidth(120);

        EditText input = new EditText(this);
        input.setText(value);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setBackgroundColor(Color.parseColor("#2a2a4a"));
        input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) listener.onChange(input.getText().toString());
        });

        row.addView(lbl);
        row.addView(input);
        propertiesPanel.addView(row);
    }

    private interface PropertyChangeListener {
        void onChange(String value);
    }

    /**
     * حوار إضافة كائن جديد
     */
    private void showAddObjectDialog() {
        String[] types = {"مستطيل فارغ", "نص", "لاعب", "عدو", "أرضية", "زر", "صورة"};
        new AlertDialog.Builder(this)
            .setTitle("إضافة كائن جديد")
            .setItems(types, (dialog, which) -> {
                GameObject obj = createObjectByType(types[which]);
                addObjectToScene(obj);
                selectObject(obj);
            })
            .show();
    }

    private GameObject createObjectByType(String type) {
        GameObject obj = new GameObject(type + "_" + (sceneObjects.size() + 1));
        switch (type) {
            case "مستطيل فارغ":
                obj.width = 100; obj.height = 100;
                obj.setProperty("color", "#4a90d9");
                break;
            case "نص":
                obj.width = 200; obj.height = 50;
                obj.setProperty("text", "نص هنا");
                obj.setProperty("textColor", "#ffffff");
                obj.setProperty("color", "#00000000");
                break;
            case "لاعب":
                obj.width = 60; obj.height = 80;
                obj.setProperty("color", "#e63946");
                obj.addTag("لاعب");
                obj.attachScript("حركة_اللاعب");
                break;
            case "عدو":
                obj.width = 60; obj.height = 60;
                obj.setProperty("color", "#f4a261");
                obj.addTag("عدو");
                break;
            case "أرضية":
                obj.width = 400; obj.height = 40;
                obj.setProperty("color", "#4a4a6a");
                obj.addTag("أرضية");
                break;
            case "زر":
                obj.width = 180; obj.height = 60;
                obj.setProperty("color", "#2a9d8f");
                obj.setProperty("text", "زر");
                break;
        }
        obj.x = 300; obj.y = 300;
        return obj;
    }

    private void showObjectContextMenu(GameObject obj) {
        new AlertDialog.Builder(this)
            .setTitle(obj.getName())
            .setItems(new String[]{"تعديل", "نسخ", "حذف"}, (dialog, which) -> {
                switch (which) {
                    case 0: selectObject(obj); break;
                    case 1: duplicateObject(obj); break;
                    case 2: deleteObject(obj); break;
                }
            }).show();
    }

    private void duplicateObject(GameObject original) {
        GameObject copy = new GameObject(original.getName() + "_نسخة");
        copy.x = original.x + 20;
        copy.y = original.y + 20;
        copy.width = original.width;
        copy.height = original.height;
        copy.rotation = original.rotation;
        for (String tag : original.getTags()) copy.addTag(tag);
        original.getProperties().forEach(copy::setProperty);
        addObjectToScene(copy);
    }

    private void deleteObject(GameObject obj) {
        sceneObjects.remove(obj);
        objectNames.remove(obj.getName());
        gameEngine.getSceneManager().removeObject(obj.getId());
        objectListAdapter.notifyDataSetChanged();
        if (selectedObject == obj) {
            selectedObject = null;
            propertiesPanel.removeAllViews();
        }
    }

    private void addPhysicsToObject(GameObject obj) {
        com.remi8.engine.physics.PhysicsBody body = gameEngine.getPhysicsEngine().addBody(obj);
        obj.addComponent("physics", body);
        Toast.makeText(this, "تمت إضافة الفيزياء لـ: " + obj.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * فتح محرر السكربت
     */
    private void openScriptEditor() {
        openScriptEditorForObject(selectedObject);
    }

    private void openScriptEditorForObject(GameObject obj) {
        Intent intent = new Intent(this, ScriptEditorActivity.class);
        if (obj != null) {
            intent.putExtra("object_name", obj.getName());
            String scriptName = obj.getAttachedScripts().isEmpty() ? obj.getName() + "_سكربت" :
                                obj.getAttachedScripts().get(0);
            intent.putExtra("script_name", scriptName);
        }
        startActivity(intent);
    }

    private void newScene() {
        new AlertDialog.Builder(this)
            .setTitle("مشهد جديد")
            .setMessage("هل تريد إنشاء مشهد جديد؟ سيتم فقدان التغييرات غير المحفوظة.")
            .setPositiveButton("نعم", (d, w) -> {
                sceneObjects.clear();
                objectNames.clear();
                objectListAdapter.notifyDataSetChanged();
                setupDefaultScene();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void saveProject() {
        Toast.makeText(this, "جارٍ حفظ المشروع...", Toast.LENGTH_SHORT).show();
        // TODO: تنفيذ حفظ حقيقي بـ JSON
        new ProjectManager(this).saveProject(gameEngine, currentProjectName, sceneObjects);
        Toast.makeText(this, "تم الحفظ ✓", Toast.LENGTH_SHORT).show();
    }

    private void exportGame() {
        Intent intent = new Intent(this, ExportActivity.class);
        intent.putExtra("project_name", currentProjectName);
        intent.putExtra("project_path", currentProjectPath);
        startActivity(intent);
    }

    private void openSettings() {
        new AlertDialog.Builder(this)
            .setTitle("إعدادات المشروع")
            .setMessage("اسم المشروع: " + currentProjectName +
                       "\nالإصدار: " + gameEngine.getGameVersion() +
                       "\nالدقة: " + gameEngine.getScreenWidth() + "x" + gameEngine.getScreenHeight())
            .setPositiveButton("إغلاق", null)
            .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameEngine != null) gameEngine.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameEngine != null && isPlaying) gameEngine.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameEngine != null) gameEngine.stop();
    }
}
