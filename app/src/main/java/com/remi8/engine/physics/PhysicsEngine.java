package com.remi8.engine.physics;

import android.graphics.RectF;
import com.remi8.engine.core.GameObject;
import java.util.ArrayList;
import java.util.List;

/**
 * محرك الفيزياء ثنائي الأبعاد - REMI8
 * يدير الجاذبية، التصادمات، والحركة الفيزيائية
 */
public class PhysicsEngine {

    // إعدادات الفيزياء
    private float gravity = 980f;        // بكسل/ثانية²
    private float airResistance = 0.98f; // مقاومة الهواء
    private boolean enabled = true;

    // قائمة الأجسام الفيزيائية
    private final List<PhysicsBody> bodies = new ArrayList<>();

    // طبقات التصادم
    public static final int LAYER_DEFAULT = 1;
    public static final int LAYER_PLAYER = 2;
    public static final int LAYER_ENEMY = 4;
    public static final int LAYER_GROUND = 8;
    public static final int LAYER_TRIGGER = 16;

    // مستمع التصادم
    private CollisionListener collisionListener;

    public interface CollisionListener {
        void onCollisionEnter(PhysicsBody a, PhysicsBody b);
        void onCollisionExit(PhysicsBody a, PhysicsBody b);
        void onTriggerEnter(PhysicsBody trigger, PhysicsBody other);
    }

    public PhysicsEngine() {}

    /**
     * تحديث الفيزياء في كل إطار
     */
    public void update(float dt) {
        if (!enabled) return;

        // تحديث كل جسم
        for (PhysicsBody body : bodies) {
            if (!body.isActive() || body.isStatic()) continue;
            updateBody(body, dt);
        }

        // فحص التصادمات
        checkCollisions();
    }

    /**
     * تحديث جسم فيزيائي واحد
     */
    private void updateBody(PhysicsBody body, float dt) {
        // تطبيق الجاذبية
        if (body.isGravityEnabled()) {
            body.velocityY += gravity * dt;
        }

        // تطبيق مقاومة الهواء
        body.velocityX *= airResistance;

        // تحديث الموضع
        body.gameObject.x += body.velocityX * dt;
        body.gameObject.y += body.velocityY * dt;

        // تحديث الدوران
        if (body.angularVelocity != 0) {
            body.gameObject.rotation += body.angularVelocity * dt;
            body.angularVelocity *= 0.95f;
        }
    }

    /**
     * فحص التصادمات بين الأجسام
     */
    private void checkCollisions() {
        for (int i = 0; i < bodies.size(); i++) {
            PhysicsBody a = bodies.get(i);
            if (!a.isActive()) continue;

            for (int j = i + 1; j < bodies.size(); j++) {
                PhysicsBody b = bodies.get(j);
                if (!b.isActive()) continue;

                // فحص طبقات التصادم
                if ((a.collisionMask & b.collisionLayer) == 0) continue;
                if ((b.collisionMask & a.collisionLayer) == 0) continue;

                if (checkAABBCollision(a, b)) {
                    if (a.isTrigger || b.isTrigger) {
                        // مشغّل (trigger)
                        if (!a.currentCollisions.contains(b)) {
                            a.currentCollisions.add(b);
                            if (collisionListener != null) {
                                collisionListener.onTriggerEnter(a.isTrigger ? a : b, a.isTrigger ? b : a);
                            }
                        }
                    } else {
                        // تصادم صلب
                        resolveCollision(a, b);
                        if (!a.currentCollisions.contains(b)) {
                            a.currentCollisions.add(b);
                            b.currentCollisions.add(a);
                            if (collisionListener != null) {
                                collisionListener.onCollisionEnter(a, b);
                            }
                        }
                    }
                } else {
                    if (a.currentCollisions.contains(b)) {
                        a.currentCollisions.remove(b);
                        b.currentCollisions.remove(a);
                        if (collisionListener != null) {
                            collisionListener.onCollisionExit(a, b);
                        }
                    }
                }
            }
        }
    }

    /**
     * فحص تصادم AABB (Axis-Aligned Bounding Box)
     */
    private boolean checkAABBCollision(PhysicsBody a, PhysicsBody b) {
        RectF boundsA = a.gameObject.getBounds();
        RectF boundsB = b.gameObject.getBounds();
        return RectF.intersects(boundsA, boundsB);
    }

    /**
     * حل التصادم بين جسمين
     */
    private void resolveCollision(PhysicsBody a, PhysicsBody b) {
        if (a.isStatic() && b.isStatic()) return;

        RectF boundsA = a.gameObject.getBounds();
        RectF boundsB = b.gameObject.getBounds();

        // حساب التداخل
        float overlapX = Math.min(boundsA.right, boundsB.right) - Math.max(boundsA.left, boundsB.left);
        float overlapY = Math.min(boundsA.bottom, boundsB.bottom) - Math.max(boundsA.top, boundsB.top);

        if (overlapX < overlapY) {
            // فصل أفقي
            float separation = overlapX / 2f;
            if (!a.isStatic()) {
                a.gameObject.x += (boundsA.centerX() < boundsB.centerX() ? -separation : separation);
                a.velocityX *= -a.bounciness;
            }
            if (!b.isStatic()) {
                b.gameObject.x += (boundsB.centerX() < boundsA.centerX() ? -separation : separation);
                b.velocityX *= -b.bounciness;
            }
        } else {
            // فصل رأسي
            float separation = overlapY / 2f;
            if (!a.isStatic()) {
                if (boundsA.centerY() < boundsB.centerY()) {
                    a.gameObject.y -= separation;
                    a.isGrounded = true;
                } else {
                    a.gameObject.y += separation;
                }
                a.velocityY *= -a.bounciness;
            }
            if (!b.isStatic()) {
                if (boundsB.centerY() < boundsA.centerY()) {
                    b.gameObject.y -= separation;
                    b.isGrounded = true;
                } else {
                    b.gameObject.y += separation;
                }
                b.velocityY *= -b.bounciness;
            }
        }
    }

    /**
     * إضافة جسم فيزيائي
     */
    public PhysicsBody addBody(GameObject obj) {
        PhysicsBody body = new PhysicsBody(obj);
        bodies.add(body);
        return body;
    }

    /**
     * إزالة جسم فيزيائي
     */
    public void removeBody(PhysicsBody body) {
        bodies.remove(body);
    }

    public void removeBodyForObject(GameObject obj) {
        bodies.removeIf(b -> b.gameObject == obj);
    }

    public List<PhysicsBody> getBodies() { return bodies; }
    public float getGravity() { return gravity; }
    public void setGravity(float g) { gravity = g; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { enabled = e; }
    public void setCollisionListener(CollisionListener listener) { this.collisionListener = listener; }
}
