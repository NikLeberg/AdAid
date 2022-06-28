package ch.bfh.adaid.action;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Action to click on the matched node.
 *
 * @author Niklaus Leuenberger
 */
public class ClickAction extends Action {
    private static final String TAG = "ClickAction";

    /**
     * How many levels in the parent chain will be searched for a clickable node.
     */
    private static final int FIND_CLICKABLE_RECURSION_LIMIT = 3;

    /**
     * Construct a new click action.
     *
     * @param service The accessibility service.
     */
    public ClickAction(AccessibilityService service) {
        super(service);
    }

    /**
     * Perform the action.
     *
     * @param node Node to execute the action on. It or a parent thereof will be clicked.
     */
    @Override
    public void triggerSeen(AccessibilityNodeInfo node) {
        Log.d(TAG, "Executing click action");
        AccessibilityNodeInfo clickableNode = findClickableNode(node, 0);
        if (clickableNode == null) {
            Log.e(TAG, "No clickable node found for " + node.getViewIdResourceName());
            return;
        }
        clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    /**
     * Trigger on gone missing of node. Do nothing.
     */
    @Override
    public void triggerGone() {
        // Do nothing.
    }

    /**
     * The triggering node may not be clickable. This method will find the first clickable node
     * along the parent chain.
     *
     * @param node           The node to start from.
     * @param recursionLevel The current recursion level.
     * @return The first clickable node or null if max recursion level was reached.
     */
    private AccessibilityNodeInfo findClickableNode(AccessibilityNodeInfo node, int recursionLevel) {
        if (node.isClickable()) {
            Log.d(TAG, "Found clickable node " + node.getViewIdResourceName() + " at recursion level " + recursionLevel);
            return node;
        }
        if (recursionLevel > FIND_CLICKABLE_RECURSION_LIMIT) {
            Log.e(TAG, "Reached max recursion level while searching clickable node.");
            return null;
        }
        AccessibilityNodeInfo parent = node.getParent();
        if (parent == null) {
            return null;
        }
        return findClickableNode(parent, recursionLevel + 1);
    }
}
