// SoundManager.kt
package com.chess.chesspuzzle

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

object SoundManager {

    private const val TAG = "SoundManager"

    private lateinit var soundPool: SoundPool
    private var successSoundId: Int = 0
    private var failureSoundId: Int = 0
    private var soundPoolLoaded: Boolean = false

    fun initialize(context: Context) {
        if (::soundPool.isInitialized && soundPoolLoaded) {
            Log.d(TAG, "$TAG: SoundPool already initialized and sounds loaded.")
            return
        } else if (::soundPool.isInitialized && !soundPoolLoaded) {
            Log.d(TAG, "$TAG: SoundPool already initialized, but sounds not loaded yet. Skipping re-init.")
            return
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        var loadedCount = 0
        val totalSoundsToLoad = 2

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedCount++
                Log.d(TAG, "$TAG: Sound with ID $sampleId loaded successfully. Loaded $loadedCount/$totalSoundsToLoad")
                if (loadedCount == totalSoundsToLoad) {
                    soundPoolLoaded = true
                    Log.d(TAG, "$TAG: All sounds loaded successfully!")
                }
            } else {
                Log.e(TAG, "$TAG: Error loading sound with ID $sampleId. Status: $status")
            }
        }

        successSoundId = soundPool.load(context, R.raw.succes, 1)
        failureSoundId = soundPool.load(context, R.raw.failed, 1)

        Log.d(TAG, "$TAG: SoundPool initialized. Initiating sound loading...")
    }

    fun release() {
        if (::soundPool.isInitialized) {
            soundPool.release()
            soundPoolLoaded = false
            Log.d(TAG, "$TAG: SoundPool released.")
        }
    }

    fun playSound(isSuccess: Boolean) {
        if (!::soundPool.isInitialized) {
            Log.e(TAG, "$TAG: SoundPool not initialized. Cannot play sound.")
            return
        }

        if (soundPoolLoaded) {
            val soundIdToPlay = if (isSuccess) successSoundId else failureSoundId
            if (soundIdToPlay != 0) {
                soundPool.play(soundIdToPlay, 1.0f, 1.0f, 0, 0, 1.0f)
                Log.d(TAG, "$TAG: Playing ${if (isSuccess) "success" else "failure"} sound.")
            } else {
                Log.e(TAG, "$TAG: Sound ID is 0. Sound not loaded.")
            }
        } else {
            Log.w(TAG, "$TAG: SoundPool is initialized but sounds are still loading. Cannot play sound.")
        }
    }
}