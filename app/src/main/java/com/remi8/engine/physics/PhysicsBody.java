package com.remi8.engine.physics;

import com.remi8.engine.core.GameObject;
import java.util.ArrayList;
import java.util.List;

/**
 * جسم فيزيائي مرتبط بكائن اللعبة
 */
public class PhysicsBody {

    public final GameObject gameObject;

    // السرعة
    public float velocityX = 0;
    public float velocityY = 0;
    public float angularVelocity = 0;

    // الخصائص
    public float mass = 1.0f;
    public float bounciness = 0.0f;  // 0=لا ارتداد، 1=ارتداد كامل
    public float friction = 0.5f;

    // الحالة
    private boolean active = true;
    private boolean staticBody = false;     // جسم ثابت (لا يتحرك)
    private boolean gravityEnabled = true;
    public boolean isTrigger = false;       // مشغّل (لا تصادم صلب)
    public boolean isGrounded = false;      // هل على الأرض؟

    // طبقات التصادم
    public int collisionLayer = PhysicsEngine.LAYER_DEFAULT;
    public int collisionMask = 0xFFFF; // يتصادم مع الكل

    // التصادمات الحالية
    public final List<PhysicsBody> currentCollisions = new ArrayList<>();

    public PhysicsBody(GameObject gameObject) {
        this.gameObject = gameObject;
    }

    /**
     * تطبيق قوة فورية
     */
    public void applyImpulse(float fx, float fy) {
        if (staticBody) return;
        velocityX += fx / mass;
        velocityY += fy / mass;
    }

    /**
     * تطبيق قوة مستمرة
     */
    public void applyForce(float fx, float fy, float dt) {
        if (staticBody) return;
        velocityX += (fx / mass) * dt;
        velocityY += (fy / mass) * dt;
    }

    /**
     * القفز
     */
    public void jump(float force) {
        if (isGrounded) {
            velocityY = -force;
            isGrounded = false;
        }
    }

    /**
     * إيقاف الحركة
     */
    public void stop() {
        velocityX = 0;
        velocityY = 0;
        angularVelocity = 0;
    }

    // Getters & Setters
    public boolean isActive() { return active; }
    public void setActive(boolean a) { active = a; }
    public boolean isStatic() { return staticBody; }
    public void setStatic(boolean s) { staticBody = s; }
    public boolean isGravityEnabled() { return gravityEnabled; }
    public void setGravityEnabled(boolean g) { gravityEnabled = g; }
}
