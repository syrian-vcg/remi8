package com.remi8.engine.core;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * كائن اللعبة الأساسي في REMI8
 * كل عنصر في المشهد هو GameObject
 */
public class GameObject {

    // معرف فريد
    private final String id;
    private String name;
    private boolean active = true;
    private boolean visible = true;

    // التحويل (الموضع، الحجم، الدوران)
    public float x = 0, y = 0;
    public float width = 64, height = 64;
    public float rotation = 0; // بالدرجات
    public float scaleX = 1, scaleY = 1;
    public float pivotX = 0.5f, pivotY = 0.5f; // نقطة التمحور (0-1)

    // الطبقة والترتيب
    private int layer = 0;
    private int zOrder = 0;

    // الوالد والأبناء
    private GameObject parent;
    private final List<GameObject> children = new ArrayList<>();

    // المكونات المرتبطة
    private final Map<String, Object> components = new HashMap<>();

    // الخصائص المخصصة (لـ remiscript)
    private final Map<String, Object> properties = new HashMap<>();

    // الوسوم
    private final List<String> tags = new ArrayList<>();

    // سكربتات مرتبطة
    private final List<String> attachedScripts = new ArrayList<>();

    public GameObject() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = "كائن_" + id;
    }

    public GameObject(String name) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
    }

    /**
     * تحديث الكائن في كل إطار
     */
    public void update(float deltaTime) {
        if (!active) return;
        for (GameObject child : children) {
            child.update(deltaTime);
        }
    }

    /**
     * الحصول على المستطيل الحدودي العالمي
     */
    public RectF getBounds() {
        float globalX = getGlobalX();
        float globalY = getGlobalY();
        return new RectF(
            globalX - (width * scaleX * pivotX),
            globalY - (height * scaleY * pivotY),
            globalX + (width * scaleX * (1 - pivotX)),
            globalY + (height * scaleY * (1 - pivotY))
        );
    }

    /**
     * التحقق من التصادم مع كائن آخر
     */
    public boolean intersects(GameObject other) {
        return getBounds().intersect(other.getBounds());
    }

    /**
     * التحقق إذا نقطة داخل الكائن
     */
    public boolean containsPoint(float px, float py) {
        return getBounds().contains(px, py);
    }

    /**
     * الحصول على الموضع العالمي
     */
    public float getGlobalX() {
        if (parent != null) return parent.getGlobalX() + x;
        return x;
    }

    public float getGlobalY() {
        if (parent != null) return parent.getGlobalY() + y;
        return y;
    }

    /**
     * إضافة كائن ابن
     */
    public void addChild(GameObject child) {
        if (child.parent != null) {
            child.parent.children.remove(child);
        }
        child.parent = this;
        children.add(child);
    }

    /**
     * إزالة كائن ابن
     */
    public void removeChild(GameObject child) {
        children.remove(child);
        child.parent = null;
    }

    /**
     * إضافة مكوّن
     */
    public void addComponent(String type, Object component) {
        components.put(type, component);
    }

    public Object getComponent(String type) {
        return components.get(type);
    }

    public boolean hasComponent(String type) {
        return components.containsKey(type);
    }

    /**
     * خصائص remiscript
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.getOrDefault(key, null);
    }

    public Object getProperty(String key, Object defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    /**
     * الوسوم
     */
    public void addTag(String tag) {
        if (!tags.contains(tag)) tags.add(tag);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    // Getters & Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public int getLayer() { return layer; }
    public void setLayer(int layer) { this.layer = layer; }
    public int getZOrder() { return zOrder; }
    public void setZOrder(int z) { this.zOrder = z; }
    public GameObject getParent() { return parent; }
    public List<GameObject> getChildren() { return children; }
    public List<String> getTags() { return tags; }
    public List<String> getAttachedScripts() { return attachedScripts; }
    public Map<String, Object> getProperties() { return properties; }

    public void attachScript(String scriptName) {
        if (!attachedScripts.contains(scriptName))
            attachedScripts.add(scriptName);
    }

    public void detachScript(String scriptName) {
        attachedScripts.remove(scriptName);
    }

    @Override
    public String toString() {
        return "GameObject{id=" + id + ", name=" + name + ", x=" + x + ", y=" + y + "}";
    }
}
