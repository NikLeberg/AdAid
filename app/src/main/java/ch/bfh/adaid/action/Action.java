package ch.bfh.adaid.action;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Base class for all actions.
 *
 * @author Niklaus Leuenberger
 */
public abstract class Action {

    protected final AccessibilityService service;

    /**
     * Default constructor for all actions. Save a reference to the accessibility service.
     *
     * @param service The accessibility service.
     */
    public Action(AccessibilityService service) {
        this.service = service;
    }

    /**
     * Trigger an action now because node that matches the rule was found.
     *
     * @param node Node that matched the rule.
     */
    public abstract void triggerSeen(AccessibilityNodeInfo node);

    /**
     * Trigger the action now because node that matches the rule was removed.
     */
    public abstract void triggerGone();
}
