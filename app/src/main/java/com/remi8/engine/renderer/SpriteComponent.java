package com.remi8.engine.renderer;

import android.graphics.*;
import java.util.ArrayList;
import java.util.List;

/**
 * مكوّن الرسومات المتحركة (Sprite) - REMI8
 * يدير تشغيل الأنيميشن frame-by-frame
 */
public class SpriteComponent {

    // بيانات الصورة
    private Bitmap spriteSheet;
    private int frameWidth, frameHeight;
    private int totalColumns, totalRows;

    // الأنيميشن
    private final List<Animation> animations = new ArrayList<>();
    private Animation currentAnim;
    private int currentFrame = 0;
    private float frameTimer = 0f;
    private boolean playing = false;
    private boolean loop = true;

    // الفلتر والشفافية
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private float alpha = 1.0f;
    private int tintColor = Color.WHITE;
    private boolean flipX = false, flipY = false;

    public SpriteComponent(Bitmap sheet, int frameW, int frameH) {
        this.spriteSheet = sheet;
        this.frameWidth = frameW;
        this.frameHeight = frameH;
        if (sheet != null) {
            totalColumns = sheet.getWidth() / frameW;
            totalRows = sheet.getHeight() / frameH;
        }
    }

    /**
     * إضافة أنيميشن جديد
     */
    public void addAnimation(String name, int[] frames, float fps) {
        animations.add(new Animation(name, frames, fps));
    }

    /**
     * تشغيل أنيميشن بالاسم
     */
    public void play(String name) {
        play(name, true);
    }

    public void play(String name, boolean loop) {
        for (Animation anim : animations) {
            if (anim.name.equals(name)) {
                if (currentAnim != anim) {
                    currentAnim = anim;
                    currentFrame = 0;
                    frameTimer = 0f;
                }
                this.loop = loop;
                playing = true;
                return;
            }
        }
    }

    /**
     * إيقاف الأنيميشن
     */
    public void stop() {
        playing = false;
        currentFrame = 0;
    }

    public void pause() {
        playing = false;
    }

    public void resume() {
        playing = true;
    }

    /**
     * تحديث الأنيميشن
     */
    public void update(float dt) {
        if (!playing || currentAnim == null) return;

        frameTimer += dt;
        float frameDuration = 1f / currentAnim.fps;

        if (frameTimer >= frameDuration) {
            frameTimer -= frameDuration;
            currentFrame++;

            if (currentFrame >= currentAnim.frames.length) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    currentFrame = currentAnim.frames.length - 1;
                    playing = false;
                }
            }
        }
    }

    /**
     * رسم الإطار الحالي على Canvas
     */
    public void draw(Canvas canvas, float x, float y, float w, float h) {
        if (spriteSheet == null) return;

        int frameIndex = currentAnim != null ?
            currentAnim.frames[Math.min(currentFrame, currentAnim.frames.length - 1)] :
            0;

        int col = frameIndex % totalColumns;
        int row = frameIndex / totalColumns;

        Rect src = new Rect(
            col * frameWidth,
            row * frameHeight,
            (col + 1) * frameWidth,
            (row + 1) * frameHeight
        );

        paint.setAlpha((int) (alpha * 255));
        paint.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.MULTIPLY));

        canvas.save();
        if (flipX || flipY) {
            canvas.scale(flipX ? -1 : 1, flipY ? -1 : 1, x + w / 2, y + h / 2);
        }

        canvas.drawBitmap(spriteSheet, src, new RectF(x, y, x + w, y + h), paint);
        canvas.restore();
    }

    // ─── كلاس الأنيميشن ───
    public static class Animation {
        public final String name;
        public final int[] frames;
        public final float fps;

        public Animation(String name, int[] frames, float fps) {
            this.name = name;
            this.frames = frames;
            this.fps = fps;
        }
    }

    // Getters & Setters
    public void setAlpha(float a) { alpha = Math.max(0, Math.min(1, a)); }
    public void setTint(int color) { tintColor = color; }
    public void setFlipX(boolean f) { flipX = f; }
    public void setFlipY(boolean f) { flipY = f; }
    public boolean isPlaying() { return playing; }
    public String getCurrentAnimName() { return currentAnim != null ? currentAnim.name : ""; }
    public int getCurrentFrame() { return currentFrame; }
    public void setSpriteSheet(Bitmap sheet) { this.spriteSheet = sheet; }
}
