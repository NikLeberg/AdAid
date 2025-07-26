package ch.bfh.adaid.service;

import android.accessibilityservice.AccessibilityService;

import ch.bfh.adaid.action.Action;
import ch.bfh.adaid.action.ActionFactory;
import ch.bfh.adaid.db.Rule;

/**
 * Class to hold additional information for each rule. This additional info is only relevant for
 * the a11y service. Because of that it is not part of the Rule class.
 * This class sadly can't be a child class of Rule. Because an existing rule instance can't be
 * casted or converted into a RuleWithExtras instance. This would only work the other way around. So
 * this behaves like a wrapper class with a reference to the actual Rule instance.
 * <p>
 * With the methods and attributes of this class a seen and gone rule trigger mechanisms is created:
 * <ul>
 *     <li>First seen trigger. If a rule i.e. the view it searches for is seen for the first time,
 *         then the triggerSeen of the action is performed.</li>
 *     <li>Gone trigger. If a rule i.e. the view it searches for is not seen anymore, then the
 *         triggerGone of the action is performed.</li>
 * </ul>
 *
 * @author Niklaus Leuenberger
 */
public class RuleWithExtras {
    /**
     * The actual rule for which additional information is stored.
     */
    public final Rule r; // short handle so one does not have to write "rule.rule.<>"

    /**
     * Flag if the rule was triggered from the last event (before the one currently processing).
     */
    private boolean triggeredByLastEvent;

    /**
     * Action that corresponds to the one as defined in the rule.
     */
    public final Action action;

    /**
     * Default constructor.
     *
     * @param rule    The rule for which additional information is stored.
     * @param service The a11y service that is used to trigger the action.
     */
    RuleWithExtras(Rule rule, AccessibilityService service) {
        r = rule; // the rule itself
        action = ActionFactory.buildAction(rule.actionType, service);
    }

    /**
     * Checks if the rule was triggered from the last event.
     * <p>
     * If the event before the one currently processing also triggered this rule, then it shouldn't
     * be triggered again.
     *
     * @return true if the rule was triggered from the last event, false otherwise
     */
    public boolean wasTriggeredByLastEvent() {
        return triggeredByLastEvent;
    }

    /**
     * Sets the triggered by current event flag.
     *
     * @param triggered true if the rule was triggered from the last event, false otherwise
     */
    public void setTriggeredByCurrentEvent(boolean triggered) {
        triggeredByLastEvent = triggered;
    }
}
