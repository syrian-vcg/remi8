package com.remi8.engine.renderer;

import android.graphics.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * نظام الجسيمات - REMI8
 * يُنتج تأثيرات بصرية: انفجارات، نجوم، دخان، مطر
 */
public class ParticleSystem {

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // إعدادات الانبعاث
    private float emitX, emitY;
    private float emitRate = 10f;      // جسيمات/ثانية
    private float emitTimer = 0f;
    private boolean active = true;
    private boolean oneShot = false;   // انفجار واحد ثم إيقاف

    // نطاق الجسيمات
    private float minSpeed = 50, maxSpeed = 200;
    private float minLife = 0.5f, maxLife = 2f;
    private float minSize = 4, maxSize = 16;
    private float gravity = 200f;
    private float spread = 360f;       // زاوية الانتشار بالدرجات
    private float direction = -90f;    // الاتجاه الرئيسي (أعلى افتراضياً)

    // الألوان
    private int startColor = Color.YELLOW;
    private int endColor = Color.RED;
    private float alphaDecay = 1.0f;

    // نوع الشكل
    public enum Shape { CIRCLE, SQUARE, STAR }
    private Shape shape = Shape.CIRCLE;

    public ParticleSystem(float x, float y) {
        this.emitX = x;
        this.emitY = y;
    }

    /**
     * تحديث الجسيمات
     */
    public void update(float dt) {
        // انبعاث جسيمات جديدة
        if (active && !oneShot) {
            emitTimer += dt;
            float interval = 1f / emitRate;
            while (emitTimer >= interval) {
                emit();
                emitTimer -= interval;
            }
        }

        // تحديث الجسيمات الموجودة
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= dt;
            if (p.life <= 0) {
                it.remove();
                continue;
            }

            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += gravity * dt;
            p.rotation += p.rotSpeed * dt;
            p.size *= 0.99f;

            // تدرج اللون والشفافية
            float ratio = 1f - (p.life / p.maxLife);
            int sA = Color.alpha(startColor), sR = Color.red(startColor),
                sG = Color.green(startColor), sB = Color.blue(startColor);
            int eA = Color.alpha(endColor), eR = Color.red(endColor),
                eG = Color.green(endColor), eB = Color.blue(endColor);

            p.currentColor = Color.argb(
                (int) (sA + (eA - sA) * ratio),
                (int) (sR + (eR - sR) * ratio),
                (int) (sG + (eG - sG) * ratio),
                (int) (sB + (eB - sB) * ratio)
            );
        }
    }

    /**
     * إنتاج جسيم واحد
     */
    private void emit() {
        Particle p = new Particle();
        p.x = emitX + (random.nextFloat() - 0.5f) * 10;
        p.y = emitY + (random.nextFloat() - 0.5f) * 10;

        float angle = (float) Math.toRadians(direction + (random.nextFloat() - 0.5f) * spread);
        float speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);
        p.vx = (float) (Math.cos(angle) * speed);
        p.vy = (float) (Math.sin(angle) * speed);

        p.maxLife = minLife + random.nextFloat() * (maxLife - minLife);
        p.life = p.maxLife;
        p.size = minSize + random.nextFloat() * (maxSize - minSize);
        p.rotation = random.nextFloat() * 360f;
        p.rotSpeed = (random.nextFloat() - 0.5f) * 360f;
        p.currentColor = startColor;

        particles.add(p);
    }

    /**
     * إطلاق انفجار فوري
     */
    public void burst(int count) {
        for (int i = 0; i < count; i++) emit();
    }

    /**
     * رندرة الجسيمات
     */
    public void render(Canvas canvas) {
        for (Particle p : particles) {
            paint.setColor(p.currentColor);
            canvas.save();
            canvas.translate(p.x, p.y);
            canvas.rotate(p.rotation);

            switch (shape) {
                case CIRCLE:
                    canvas.drawCircle(0, 0, p.size / 2, paint);
                    break;
                case SQUARE:
                    canvas.drawRect(-p.size / 2, -p.size / 2, p.size / 2, p.size / 2, paint);
                    break;
                case STAR:
                    drawStar(canvas, p.size / 2, paint);
                    break;
            }
            canvas.restore();
        }
    }

    private void drawStar(Canvas canvas, float r, Paint p) {
        Path path = new Path();
        float innerR = r * 0.4f;
        for (int i = 0; i < 5; i++) {
            float outerAngle = (float) Math.toRadians(i * 72 - 90);
            float innerAngle = (float) Math.toRadians(i * 72 + 36 - 90);
            if (i == 0) path.moveTo((float) Math.cos(outerAngle) * r, (float) Math.sin(outerAngle) * r);
            else path.lineTo((float) Math.cos(outerAngle) * r, (float) Math.sin(outerAngle) * r);
            path.lineTo((float) Math.cos(innerAngle) * innerR, (float) Math.sin(innerAngle) * innerR);
        }
        path.close();
        canvas.drawPath(path, p);
    }

    /**
     * تأثير جاهز: انفجار
     */
    public static ParticleSystem createExplosion(float x, float y) {
        ParticleSystem ps = new ParticleSystem(x, y);
        ps.startColor = Color.YELLOW;
        ps.endColor = Color.parseColor("#e63946");
        ps.minSpeed = 100; ps.maxSpeed = 400;
        ps.minSize = 6; ps.maxSize = 20;
        ps.minLife = 0.3f; ps.maxLife = 1f;
        ps.gravity = 100f;
        ps.spread = 360f;
        ps.burst(40);
        return ps;
    }

    /**
     * تأثير جاهز: نجوم
     */
    public static ParticleSystem createStars(float x, float y) {
        ParticleSystem ps = new ParticleSystem(x, y);
        ps.startColor = Color.YELLOW;
        ps.endColor = Color.WHITE;
        ps.shape = Shape.STAR;
        ps.minSpeed = 50; ps.maxSpeed = 150;
        ps.minSize = 10; ps.maxSize = 25;
        ps.minLife = 1f; ps.maxLife = 2f;
        ps.gravity = -50f;
        ps.spread = 360f;
        ps.burst(15);
        return ps;
    }

    /**
     * تأثير جاهز: دخان
     */
    public static ParticleSystem createSmoke(float x, float y) {
        ParticleSystem ps = new ParticleSystem(x, y);
        ps.startColor = Color.argb(180, 100, 100, 100);
        ps.endColor = Color.argb(0, 150, 150, 150);
        ps.minSpeed = 20; ps.maxSpeed = 60;
        ps.minSize = 15; ps.maxSize = 40;
        ps.minLife = 1.5f; ps.maxLife = 3f;
        ps.gravity = -30f;
        ps.spread = 60f;
        ps.direction = -90f;
        ps.emitRate = 5f;
        return ps;
    }

    // Setters
    public void setPosition(float x, float y) { emitX = x; emitY = y; }
    public void setActive(boolean a) { active = a; }
    public void setColors(int start, int end) { startColor = start; endColor = end; }
    public void setSpeed(float min, float max) { minSpeed = min; maxSpeed = max; }
    public void setLife(float min, float max) { minLife = min; maxLife = max; }
    public void setSize(float min, float max) { minSize = min; maxSize = max; }
    public void setGravity(float g) { gravity = g; }
    public void setSpread(float s) { spread = s; }
    public void setDirection(float d) { direction = d; }
    public void setShape(Shape s) { shape = s; }
    public void setEmitRate(float r) { emitRate = r; }
    public boolean isAlive() { return !particles.isEmpty() || active; }
    public int getParticleCount() { return particles.size(); }

    // كلاس الجسيم
    private static class Particle {
        float x, y, vx, vy;
        float size, rotation, rotSpeed;
        float life, maxLife;
        int currentColor;
    }
}
