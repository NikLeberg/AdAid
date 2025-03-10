package ch.bfh.adaid.action;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Action to navigate back, behaves exactly as if the back button / gesture was triggered.
 *
 * @author Philipp Allweyer
 */
public class BackAction extends Action {
    private static final String TAG = "BackAction";

    /**
     * Construct a new back action.
     *
     * @param service The accessibility service.
     */
    public BackAction(AccessibilityService service) {
        super(service);
    }

    /**
     * Perform a GLOBAL_ACTION_BACK navigation.
     *
     * @param node Not used.
     */
    @Override
    public void triggerSeen(AccessibilityNodeInfo node) {
        Log.d(TAG, "Executing back action");
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    /**
     * Trigger on gone missing of node. Do nothing.
     */
    @Override
    public void triggerGone() {
        // Do nothing.
    }
}
