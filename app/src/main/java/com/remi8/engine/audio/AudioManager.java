package com.remi8.engine.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * مدير الصوت - REMI8
 * يدير مؤثرات الصوت والموسيقى الخلفية
 */
public class AudioManager {

    private static final String TAG = "REMI8_Audio";

    private final Context context;
    private SoundPool soundPool;
    private MediaPlayer musicPlayer;

    // الأصوات المحملة
    private final Map<String, Integer> soundEffects = new HashMap<>();
    private final Map<String, Integer> streamIds = new HashMap<>();

    // إعدادات الصوت
    private float masterVolume = 1.0f;
    private float musicVolume = 0.7f;
    private float sfxVolume = 1.0f;
    private boolean muted = false;

    // حالة الموسيقى
    private String currentMusic = "";
    private boolean musicPaused = false;

    public AudioManager(Context context) {
        this.context = context;
        initSoundPool();
    }

    private void initSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        soundPool = new SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(attributes)
            .build();

        Log.i(TAG, "تم تهيئة مدير الصوت");
    }

    /**
     * تحميل مؤثر صوتي من المجلد assets
     */
    public void loadSound(String name, String assetPath) {
        try {
            android.content.res.AssetFileDescriptor afd =
                context.getAssets().openFd(assetPath);
            int soundId = soundPool.load(afd, 1);
            soundEffects.put(name, soundId);
            Log.d(TAG, "تم تحميل الصوت: " + name);
        } catch (IOException e) {
            Log.e(TAG, "فشل تحميل الصوت: " + name + " - " + e.getMessage());
        }
    }

    /**
     * تشغيل مؤثر صوتي
     */
    public int playSound(String name) {
        return playSound(name, false, 1.0f);
    }

    public int playSound(String name, boolean loop, float volume) {
        if (muted || !soundEffects.containsKey(name)) return -1;

        Integer soundId = soundEffects.get(name);
        if (soundId == null) return -1;

        float vol = sfxVolume * masterVolume * volume;
        int streamId = soundPool.play(soundId, vol, vol, 1, loop ? -1 : 0, 1.0f);
        streamIds.put(name + "_" + streamId, streamId);
        return streamId;
    }

    /**
     * إيقاف مؤثر صوتي
     */
    public void stopSound(int streamId) {
        soundPool.stop(streamId);
    }

    public void stopSound(String name) {
        Integer soundId = soundEffects.get(name);
        if (soundId != null) soundPool.stop(soundId);
    }

    /**
     * تشغيل موسيقى خلفية (تكرار تلقائي)
     */
    public void playMusic(String assetPath) {
        playMusic(assetPath, true);
    }

    /**
     * تشغيل موسيقى خلفية
     */
    public void playMusic(String assetPath, boolean loop) {
        stopMusic();
        try {
            musicPlayer = new MediaPlayer();
            android.content.res.AssetFileDescriptor afd =
                context.getAssets().openFd(assetPath);
            musicPlayer.setDataSource(afd.getFileDescriptor(),
                                      afd.getStartOffset(), afd.getLength());
            musicPlayer.setLooping(loop);
            musicPlayer.setVolume(musicVolume * masterVolume, musicVolume * masterVolume);
            musicPlayer.prepare();
            musicPlayer.start();
            currentMusic = assetPath;
            musicPaused = false;
            Log.i(TAG, "تشغيل الموسيقى: " + assetPath);
        } catch (IOException e) {
            Log.e(TAG, "فشل تشغيل الموسيقى: " + e.getMessage());
        }
    }

    /**
     * إيقاف الموسيقى
     */
    public void stopMusic() {
        if (musicPlayer != null) {
            if (musicPlayer.isPlaying()) musicPlayer.stop();
            musicPlayer.release();
            musicPlayer = null;
            currentMusic = "";
        }
    }

    /**
     * تعليق جميع الأصوات
     */
    public void pauseAll() {
        soundPool.autoPause();
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
            musicPaused = true;
        }
    }

    /**
     * استئناف جميع الأصوات
     */
    public void resumeAll() {
        soundPool.autoResume();
        if (musicPlayer != null && musicPaused) {
            musicPlayer.start();
            musicPaused = false;
        }
    }

    /**
     * تحرير الموارد
     */
    public void release() {
        stopMusic();
        soundPool.release();
        Log.i(TAG, "تم تحرير موارد الصوت");
    }

    /**
     * إيقاف جميع الأصوات (مؤثرات + موسيقى)
     */
    public void stopAll() {
        soundPool.autoPause();
        stopMusic();
    }

    /**
     * تعيين مستوى صوت الموسيقى والمؤثرات معاً
     */
    public void setVolume(float music, float sfx) {
        setMusicVolume(music);
        setSfxVolume(sfx);
    }

    // إعدادات الصوت
    public void setMasterVolume(float v) {
        masterVolume = Math.max(0, Math.min(1, v));
        updateMusicVolume();
    }

    public void setMusicVolume(float v) {
        musicVolume = Math.max(0, Math.min(1, v));
        updateMusicVolume();
    }

    public void setSfxVolume(float v) {
        sfxVolume = Math.max(0, Math.min(1, v));
    }

    public void setMuted(boolean m) {
        muted = m;
        if (m) pauseAll(); else resumeAll();
    }

    private void updateMusicVolume() {
        if (musicPlayer != null) {
            float vol = musicVolume * masterVolume;
            musicPlayer.setVolume(vol, vol);
        }
    }

    public float getMasterVolume() { return masterVolume; }
    public float getMusicVolume() { return musicVolume; }
    public float getSfxVolume() { return sfxVolume; }
    public boolean isMuted() { return muted; }
    public boolean isMusicPlaying() { return musicPlayer != null && musicPlayer.isPlaying(); }
    public String getCurrentMusic() { return currentMusic; }
}
