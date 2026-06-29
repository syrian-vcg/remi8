# 🎮 REMI8 Script Graph - الدليل الكامل

<div align="center">

**محرك Script Graph المرئي + C++ JNI + remiscript v2**

</div>

---

## 🏗️ البنية الكاملة

```
remi8-scriptgraph/
│
├── app/src/main/
│   │
│   ├── cpp/                              ← C++ Engine
│   │   ├── CMakeLists.txt               ← إعداد CMake
│   │   └── remiscript_engine.cpp        ← محرك C++ الأساسي
│   │       ├── ScriptNode (struct)      ← بنية العقدة
│   │       ├── ScriptGraph (class)      ← إدارة Graph
│   │       │   ├── addNode()
│   │       │   ├── connect()
│   │       │   ├── evaluate()           ← تقييم C++ سريع
│   │       │   ├── executeEvent()
│   │       │   └── exportToRemiScript() ← توليد كود
│   │       └── JNI Functions            ← Java↔C++ Bridge
│   │
│   ├── java/com/remi8/
│   │   │
│   │   ├── scriptgraph/                 ← Script Graph System
│   │   │   ├── ScriptGraphEngine.java   ← JNI Bridge (Java side)
│   │   │   ├── ScriptGraphView.java     ← Canvas رسم العقد
│   │   │   ├── ScriptGraphActivity.java ← الشاشة الرئيسية
│   │   │   └── nodes/
│   │   │       └── ScriptNode.java      ← نموذج بيانات العقدة
│   │   │
│   │   ├── remiscript/                  ← لغة remiscript v2
│   │   │   ├── parser/
│   │   │   │   └── RemiScriptLexer.java ← المحلل اللغوي
│   │   │   └── interpreter/
│   │   │       ├── RemiScriptInterpreter.java ← المفسّر v2
│   │   │       ├── RemiScriptParser.java
│   │   │       ├── ASTNode.java
│   │   │       └── ParsedScript.java
│   │   │
│   │   ├── engine/                      ← محرك اللعبة
│   │   │   ├── core/ (GameEngine, GameObject, GameView...)
│   │   │   ├── renderer/ (Renderer2D, Camera2D, Particles...)
│   │   │   ├── physics/ (PhysicsEngine, PhysicsBody)
│   │   │   ├── audio/ (AudioManager)
│   │   │   ├── input/ (InputManager)
│   │   │   └── scene/ (SceneManager, Scene, TileMap)
│   │   │
│   │   └── editor/                      ← محرر المشاريع
│   │       ├── EditorActivity.java
│   │       ├── ScriptEditorActivity.java
│   │       └── ProjectManager.java
│   │
│   └── res/
│       ├── layout/
│       │   └── activity_script_graph.xml
│       └── values/
│           ├── strings.xml
│           └── styles.xml
│
└── app/build.gradle                     ← CMake + JNI config
```

---

## 🔗 تدفق البيانات

```
┌─────────────────────────────────────────────────────────┐
│                    Script Graph UI                       │
│  ScriptGraphActivity                                     │
│  ┌──────────────────┐   ┌──────────────────────────┐    │
│  │  ScriptGraphView │   │  Node Palette            │    │
│  │  (Canvas/Touch)  │   │  + / Drag / Connect      │    │
│  └────────┬─────────┘   └──────────────────────────┘    │
└───────────┼─────────────────────────────────────────────┘
            │ addNode / connect / evaluate
            ▼
┌─────────────────────────────────────────────────────────┐
│              ScriptGraphEngine.java (JNI)                │
│  Java bridge → native methods                            │
└───────────────────────┬─────────────────────────────────┘
                        │ JNI calls
                        ▼
┌─────────────────────────────────────────────────────────┐
│           remiscript_engine.cpp (C++)                    │
│                                                          │
│  ScriptGraph::evaluate()  → توليد نتائج رياضية         │
│  ScriptGraph::executeEvent() → تنفيذ سلسلة exec        │
│  ScriptGraph::exportToRemiScript() → توليد كود عربي    │
└───────────────────────┬─────────────────────────────────┘
                        │ نتائج
                        ▼
┌─────────────────────────────────────────────────────────┐
│        RemiScriptInterpreter v2 (Java)                   │
│                                                          │
│  Graph.تشغيل("OnUpdate")  ← من remiscript               │
│  Graph.قيمة(nodeId, "O")  ← قراءة نتيجة عقدة          │
│  Graph.تصدير()            ← تصدير كـ remiscript        │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 أنواع العقد المدعومة

| العقدة | النوع | الـ Pins | الوصف |
|--------|-------|----------|-------|
| **On Update** | Event | exec→ | يُطلق كل frame |
| **On Start** | Event | exec→ | يُطلق عند البداية |
| **Mathf Abs** | Math | →exec,F / exec,O→ | القيمة المطلقة |
| **Mathf Min** | Math | →exec,A,B / exec,O→ | الأصغر |
| **Mathf Max** | Math | →exec,A,B / exec,O→ | الأكبر |
| **Mathf Sqrt** | Math | →exec,F / exec,O→ | الجذر التربيعي |
| **Mathf Sin/Cos** | Math | →exec,F / exec,O→ | المثلثات |
| **Subtract** | Math | →A,B / A-B→ | A - B |
| **Add** | Math | →A,B / A+B→ | A + B |
| **Multiply** | Math | →A,B / A×B→ | A × B |
| **Divide** | Math | →A,B / A÷B→ | A ÷ B |
| **Float** | Value | value→ | ثابت رقمي |
| **Print** | Debug | →exec,Value / exec→ | طباعة |

---

## 🎮 استخدام Script Graph من remiscript

```remiscript
// تشغيل Graph مباشرة
دالة تحديث(دت) {
    // تشغيل حدث OnUpdate في الـ Graph
    Graph.تشغيل("OnUpdate")
    
    // قراءة نتيجة عقدة معينة (id=3 وهي Abs)
    متغير نتيجة = Graph.قيمة(3, "O")
    طباعة("نتيجة Abs:", نتيجة)
}

// تصدير Graph كـ remiscript
دالة بداية() {
    متغير كود = Graph.تصدير()
    طباعة(كود)
}
```

---

## 🔧 إضافة عقدة مخصصة

### 1. في C++ (`remiscript_engine.cpp`):
```cpp
// في enum NodeType
CUSTOM_LERP = 23,

// في evaluate()
case NodeType::CUSTOM_LERP: {
    float a = getInput(node, "A", vars);
    float b = getInput(node, "B", vars);
    float t = getInput(node, "T", vars);
    return a + (b - a) * t;
}
```

### 2. في Java (`ScriptGraphEngine.java`):
```java
public static final int NODE_CUSTOM_LERP = 23;
```

### 3. في `ScriptNode.java` (setupPins):
```java
case 23: // CUSTOM_LERP
    inputs.add(new Pin("A", PinType.FLOAT, true));
    inputs.add(new Pin("B", PinType.FLOAT, true));
    inputs.add(new Pin("T", PinType.FLOAT, true));
    outputs.add(new Pin("O", PinType.FLOAT, false));
    break;
```

---

## ⚙️ إعداد المشروع

```bash
# 1. انسخ الملفات الجديدة للمشروع الأصلي
cp -r remi8-scriptgraph/app/src/main/cpp          remi8-main/app/src/main/
cp -r remi8-scriptgraph/app/src/main/java/com/remi8/scriptgraph  \
      remi8-main/app/src/main/java/com/remi8/

# 2. استبدل المفسّر والـ build.gradle
cp remi8-scriptgraph/app/build.gradle             remi8-main/app/
cp remi8-scriptgraph/app/src/main/java/com/remi8/remiscript/interpreter/RemiScriptInterpreter.java \
   remi8-main/app/src/main/java/com/remi8/remiscript/interpreter/

# 3. بناء المشروع
cd remi8-main
./gradlew assembleDebug
```

---

## 📱 التحكم في الواجهة

| الإجراء | التأثير |
|---------|---------|
| **سحب العقدة** | تحريك في Canvas |
| **Pinch** | تكبير/تصغير |
| **سحب Output Pin** | بدء ربط wire |
| **إفلات على Input Pin** | إكمال الربط |
| **نقر مزدوج على Float** | تعديل القيمة |
| **▶ RUN** | تنفيذ Graph من C++ |
| **📤** | تصدير كـ remiscript |
| **＋** | فتح لوحة الـ Palette |
| **📋** | تحميل مثال الصورة |
