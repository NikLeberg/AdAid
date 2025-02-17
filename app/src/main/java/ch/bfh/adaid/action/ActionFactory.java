package ch.bfh.adaid.action;

import android.accessibilityservice.AccessibilityService;

/**
 * Factory class for creating actions of a specific type.
 *
 * @author Niklaus Leuenberger
 */
public class ActionFactory {

    /**
     * Create an action of the given type.
     *
     * @param type    The type of the action.
     * @param service The accessibility service.
     * @return The action of the given type.
     */
    public static Action buildAction(ActionType type, AccessibilityService service) {
        switch (type) {
            case ACTION_SWIPE_LEFT:
                return new SwipeAction(service, SwipeAction.Direction.LEFT);
            case ACTION_SWIPE_RIGHT:
                return new SwipeAction(service, SwipeAction.Direction.RIGHT);
            case ACTION_SWIPE_UP:
                return new SwipeAction(service, SwipeAction.Direction.UP);
            case ACTION_SWIPE_DOWN:
                return new SwipeAction(service, SwipeAction.Direction.DOWN);
            case ACTION_CLICK:
                return new ClickAction(service);
            case ACTION_MUTE:
                return new MuteAction(service);
            case ACTION_BLOCK:
                return new BlockAction(service);
            case ACTION_BACK:
                return new BackAction(service);
            default:
                throw new IllegalArgumentException("Unknown ActionType: " + type);
        }
    }
}
