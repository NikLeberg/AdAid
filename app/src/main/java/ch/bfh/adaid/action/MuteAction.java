package ch.bfh.adaid.action;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Action to mute the music audio stream while the triggering node is visible.
 * <p>
 * Note that muting can be easily dismissed / deactivated if the user presses a volume +/- button.
 *
 * @author Niklaus Leuenberger
 */
public class MuteAction extends Action {
    private static final String TAG = "MuteAction";

    private final AudioManager audioManager;

    /**
     * Construct a new audio action with the given mute flag.
     *
     * @param service The accessibility service. Used to get AudioManager service.
     */
    public MuteAction(AccessibilityService service) {
        super(service);
        audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * Mute the music stream.
     *
     * @param node Not used.
     */
    @Override
    public void triggerSeen(AccessibilityNodeInfo node) {
        Log.d(TAG, "Muting music stream");
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
    }

    /**
     * Un-mute the music stream.
     */
    public void triggerGone() {
        Log.d(TAG, "Un-muting music stream");
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
    }
}
