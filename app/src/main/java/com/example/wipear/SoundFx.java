package com.example.wipear;

import android.media.AudioManager;
import android.media.ToneGenerator;

/**
 * Tiny sound-effect helper. Uses the built-in {@link ToneGenerator}
 * so no audio asset files are required.
 */
public final class SoundFx {

    private static boolean muted = false;

    public static void setMuted(boolean m) {
        muted = m;
    }

    public static boolean isMuted() {
        return muted;
    }

    private ToneGenerator gen;

    public SoundFx() {
        try {
            gen = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (RuntimeException e) {
            gen = null; // some devices can fail to allocate; just stay silent
        }
    }

    public void keep() {
        play(ToneGenerator.TONE_PROP_BEEP, 90);
    }

    public void trash() {
        play(ToneGenerator.TONE_PROP_NACK, 120);
    }

    public void deleted() {
        play(ToneGenerator.TONE_PROP_ACK, 150);
    }

    private void play(int tone, int durationMs) {
        if (muted) {
            return;
        }
        if (gen != null) {
            try {
                gen.startTone(tone, durationMs);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void release() {
        if (gen != null) {
            gen.release();
            gen = null;
        }
    }
}
