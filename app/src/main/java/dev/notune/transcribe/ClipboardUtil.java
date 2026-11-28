package dev.notune.transcribe;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Build;
import android.os.PersistableBundle;

/** Clipboard helpers for transcribed text. */
final class ClipboardUtil {
    private ClipboardUtil() {}

    /**
     * A plain-text clip flagged as sensitive (Android 13+): the system hides
     * it from the copy preview, and keyboards that honour the flag keep it
     * out of their clipboard history and cloud clipboard sync. Dictations can
     * be private, so every transcription copy goes through this.
     */
    static ClipData sensitiveClip(CharSequence label, CharSequence text) {
        ClipData clip = ClipData.newPlainText(label, text);
        if (Build.VERSION.SDK_INT >= 33) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }
        return clip;
    }
}
