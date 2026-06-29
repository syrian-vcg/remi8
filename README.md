# 🎮 REMI8 - محرك الألعاب ثنائية الأبعاد

<div align="center">

![REMI8 Logo](app/src/main/res/drawable/ic_launcher.xml)

**محرك ألعاب 2D كامل للأندرويد بلغة remiscript العربية**

[![Build Status](https://github.com/yourusername/REMI8/actions/workflows/build.yml/badge.svg)](https://github.com/yourusername/REMI8/actions)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-11-orange)](https://java.com)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

</div>

---

## ✨ المميزات

| الميزة | الوصف |
|--------|-------|
| 🔴 **محرك اللعبة** | حلقة لعبة 60 FPS بخيط منفصل |
| ✏️ **remiscript** | لغة برمجة عربية مخصصة للألعاب |
| ⚡ **الفيزياء** | جاذبية، تصادمات AABB، ارتداد |
| 🎵 **الصوت** | مؤثرات + موسيقى خلفية (SoundPool + MediaPlayer) |
| 🗺️ **TileMap** | خرائط بلاطات للمستويات |
| ✨ **Particles** | نظام جسيمات (انفجارات، نجوم، دخان) |
| 🎬 **Sprites** | رسوم متحركة frame-by-frame |
| 📷 **Camera2D** | كاميرا متقدمة مع تتبع واهتزاز |
| 💾 **Save System** | حفظ وتحميل بيانات اللعبة |
| 📦 **تصدير APK** | تصدير اللعبة كـ APK + GitHub Actions |
| 📱 **Android 8+** | يدعم API 26 وما فوق |

---

## 🚀 تثبيت المشروع

### المتطلبات
- Android Studio Hedgehog أو أحدث
- Java 11+
- Android SDK 34
- Gradle 8.4+

### الخطوات
```bash
git clone https://github.com/yourusername/REMI8.git
cd REMI8
./gradlew assembleDebug
```

---

## 📝 لغة remiscript

لغة برمجة مخصصة للألعاب، تدعم **العربية والإنجليزية**.

### مثال أساسي

```remiscript
// متغيرات اللعبة
متغير السرعة = 300
متغير النقاط = 0

// دالة البداية
دالة بداية() {
    طباعة("مرحباً في REMI8!")
    هذا.تعيين_لون("#e63946")
}

// دالة التحديث (60 FPS)
دالة تحديث(دت) {
    إذا (مدخل.زر("يمين")) {
        هذا.س += السرعة * دت
    }
    إذا (مدخل.زر("يسار")) {
        هذا.س -= السرعة * دت
    }
    إذا (مدخل.زر("قفز") و هذا.علىالأرض) {
        هذا.قفز(500)
        صوت.شغّل("قفز")
    }
}

// دالة التصادم
دالة تصادم(الآخر) {
    إذا (الآخر.وسم == "عملة") {
        النقاط += 10
        الآخر.تعطيل()
    }
}
```

### الكلمات المحجوزة العربية

| الفئة | الكلمات |
|-------|---------|
| **التحكم** | `إذا` `وإلا` `طالما` `كرر` `لكل` `في` |
| **الدوال** | `دالة` `إرجاع` `إيقاف` `استمر` |
| **المتغيرات** | `متغير` `ثابت` |
| **المنطق** | `و` `أو` `ليس` `صحيح` `خطأ` `فارغ` |
| **الكائن الحالي** | `هذا.س` `هذا.ص` `هذا.قفز()` `هذا.دفع()` |

### واجهات برمجة remiscript

```remiscript
// ─── المدخلات ───
مدخل.زر("يمين")      // هل الزر مضغوط؟
مدخل.إيماءة()        // اتجاه السحب: "يمين"/"يسار"/"أعلى"/"أسفل"
مدخل.جويستيك_س()     // قيمة الجويستيك (-1 إلى 1)

// ─── الصوت ───
صوت.شغّل("اسم_الصوت")
صوت.موسيقى("ملف.mp3")
صوت.إيقاف()

// ─── المشهد ───
مشهد.ابحث("اسم_الكائن")
مشهد.تبديل("اسم_المشهد")
مشهد.إضافة(كائن)

// ─── الرياضيات ───
رياضيات.جذر(9)        // 3
رياضيات.مطلق(-5)      // 5
رياضيات.أقصى(10, 20)  // 20
رياضيات.عشوائي(1, 6)  // عدد عشوائي

// ─── هذا (الكائن الحالي) ───
هذا.س              // الموضع الأفقي
هذا.ص              // الموضع الرأسي
هذا.علىالأرض       // هل على الأرض؟
هذا.قفز(500)        // القفز بقوة 500
هذا.دفع(300, 0)     // دفع أفقي
هذا.تعيين_لون("#ff0000")
هذا.تعيين_نص("نقاط: 10")
هذا.إخفاء()
هذا.إظهار()
هذا.تعطيل()
```

---

## 🏗️ هيكل المشروع

```
REMI8/
├── app/src/main/java/com/remi8/
│   ├── engine/
│   │   ├── core/           # GameEngine, GameObject, GameActivity
│   │   ├── renderer/       # Renderer2D, SpriteComponent, ParticleSystem, Camera2D
│   │   ├── physics/        # PhysicsEngine, PhysicsBody
│   │   ├── audio/          # AudioManager
│   │   ├── input/          # InputManager
│   │   └── scene/          # SceneManager, Scene, TileMap
│   ├── remiscript/
│   │   ├── parser/         # RemiScriptLexer (المحلل اللغوي)
│   │   └── interpreter/    # RemiScriptParser, RemiScriptInterpreter, ASTNode
│   ├── editor/             # EditorActivity, EditorView, ScriptEditorActivity
│   └── export/             # ExportActivity
├── .github/workflows/
│   └── build.yml           # GitHub Actions - بناء APK تلقائي
└── assets/scripts/
    └── مثال_لعبة.remi8s    # مثال سكربت عربي
```

---

## 🔧 GitHub Actions

يتضمن المشروع ملف `.github/workflows/build.yml` يقوم بـ:

1. ✅ **فحص الكود** (Lint) عند كل push
2. 🔨 **بناء APK Debug** تلقائياً
3. 📦 **بناء APK Release** عند الدفع لـ main
4. 🏷️ **إنشاء Release** تلقائي عند إنشاء Tag

### إعداد التوقيع (اختياري)
أضف في `Settings > Secrets`:
```
KEYSTORE_BASE64   - Keystore مشفّر بـ Base64
KEYSTORE_PASSWORD - كلمة مرور Keystore
KEY_ALIAS         - اسم المفتاح
KEY_PASSWORD      - كلمة مرور المفتاح
```

---

## 📱 الأجهزة المدعومة

| الإصدار | API | الدعم |
|---------|-----|-------|
| Android 8.0 Oreo | 26 | ✅ |
| Android 9.0 Pie | 28 | ✅ |
| Android 10 | 29 | ✅ |
| Android 11 | 30 | ✅ |
| Android 12 | 31 | ✅ |
| Android 13 | 33 | ✅ |
| Android 14 | 34 | ✅ |

---

## 📄 الرخصة

MIT License - حرية الاستخدام والتعديل والتوزيع

---

<div align="center">

**REMI8 Engine** | صُنع بـ ❤️ للمطورين العرب

</div>
