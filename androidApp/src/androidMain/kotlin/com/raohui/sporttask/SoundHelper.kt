package com.raohui.sporttask

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundHelper(context: Context) {
    
    private val soundPool: SoundPool
    private var startSoundId: Int = 0
    private var endSoundId: Int = 0
    private var countdownSoundId: Int = 0
    
    private val prefs = context.getSharedPreferences("sport_task_prefs", Context.MODE_PRIVATE)
    
    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // 加载音效（使用系统默认音效）
        // 实际项目中可以添加自定义音效文件
    }
    
    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean("sound_enabled", true)
    }
    
    fun playStartSound() {
        if (isSoundEnabled()) {
            // 播放开始音效
            soundPool.play(startSoundId, 1f, 1f, 1, 0, 1f)
        }
    }
    
    fun playEndSound() {
        if (isSoundEnabled()) {
            // 播放结束音效
            soundPool.play(endSoundId, 1f, 1f, 1, 0, 1f)
        }
    }
    
    fun playCountdownSound() {
        if (isSoundEnabled()) {
            // 播放倒计时音效
            soundPool.play(countdownSoundId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }
    
    fun release() {
        soundPool.release()
    }
}

class VibrationHelper(context: Context) {
    
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    private val prefs = context.getSharedPreferences("sport_task_prefs", Context.MODE_PRIVATE)
    
    fun isVibrationEnabled(): Boolean {
        return prefs.getBoolean("vibration_enabled", false)
    }
    
    fun vibrateShort() {
        if (isVibrationEnabled() && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        }
    }
    
    fun vibrateMedium() {
        if (isVibrationEnabled() && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        }
    }
    
    fun vibrateLong() {
        if (isVibrationEnabled() && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }
    
    fun vibratePattern() {
        if (isVibrationEnabled() && vibrator.hasVibrator()) {
            val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }
}
