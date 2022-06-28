package ch.bfh.adaid.action;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Action to swipe in LEFT/RIGHT/UP/DOWN direction over the screen.
 *
 * @author Niklaus Leuenberger
 */
public class SwipeAction extends Action {
    private static final String TAG = "SwipeAction";

    /**
     * Possible directions of a swipe.
     */
    public enum Direction {
        LEFT, RIGHT, UP, DOWN
    }

    /**
     * Flag to check if the static variables have been initialized.
     */
    private static boolean isInitialized = false;

    /**
     * The width and height of the screen. Set in initialize().
     */
    private static int width, height;

    /**
     * Delay after which swipes will be performed after dispatch.
     * Necessary as otherwise swipes seem to not be dispatched correctly.
     */
    private static final long SWIPE_DISPATCH_DELAY = 200; // ms

    /**
     * Duration of swipes.
     */
    private static final long SWIPE_DURATION = 50; // ms

    /**
     * Initialize static variables.
     *
     * @param context The context of the application.
     */
    public static void initialize(Context context) {
        // Get the current screen size.
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;
        isInitialized = true;
    }

    /**
     * Throws an exception if the static variables (i.e. display size) have not been initialized.
     */
    private static void throwIfNotInitialized() {
        if (!isInitialized) {
            throw new IllegalStateException("SwipeAction class not initialized. Call initialize() first.");
        }
    }

    /**
     * Build swipe gesture for the given direction.
     *
     * @param direction Direction of the swipe to build.
     * @return The built gesture.
     */
    private static GestureDescription buildSwipe(Direction direction) {
        throwIfNotInitialized();
        // Swipes move over 50 % of the screen. E.g. a left swipe starts at 3/4 of the screen width
        // and ends at 1/4 of the screen width.
        final int middleY = height / 2;
        final int top = height / 4;
        final int bottom = top * 3;
        final int middleX = width / 2;
        final int left = width / 4;
        final int right = left * 3;
        switch (direction) {
            case LEFT:
                return buildGestureDescription(right, middleY, left, middleY);
            case RIGHT:
                return buildGestureDescription(left, middleY, right, middleY);
            case UP:
                return buildGestureDescription(middleX, bottom, middleX, top);
            case DOWN:
                return buildGestureDescription(middleX, top, middleX, bottom);
            default:
                throw new IllegalArgumentException("Unknown direction: " + direction);
        }
    }

    /**
     * Build a swipe gesture description with the specified start and end coordinates.
     *
     * @param startX The X coordinate of the start point.
     * @param startY The Y coordinate of the start point.
     * @param endX   The X coordinate of the end point.
     * @param endY   The Y coordinate of the end point.
     * @return The built gesture.
     */
    private static GestureDescription buildGestureDescription(int startX, int startY, int endX, int endY) {
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        return new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path,
                        SWIPE_DISPATCH_DELAY, SWIPE_DURATION))
                .build();
    }

    /**
     * Swipe gesture to be performed when action is executed.
     */
    private final GestureDescription swipeGesture;

    /**
     * Construct a new swipe action with the given direction.
     *
     * @param service   The accessibility service. Used to dispatch gestures.
     * @param direction The direction of the swipe.
     */
    public SwipeAction(AccessibilityService service, Direction direction) {
        super(service);
        throwIfNotInitialized();
        swipeGesture = buildSwipe(direction);
    }

    /**
     * Perform the swipe gesture.
     *
     * @param node Not used.
     */
    @Override
    public void triggerSeen(AccessibilityNodeInfo node) {
        // TODO: (BUG) Sometimes the gesture is dispatched but not executed.
        // TODO: Detect if user is already performing a gesture and don't start a new one.
        Log.d(TAG, "Dispatching gesture.");
        if (!service.dispatchGesture(swipeGesture, null, null)) {
            Log.e(TAG, "Failed to dispatch gesture.");
        }
    }

    /**
     * Trigger on gone missing of node. Do nothing.
     */
    @Override
    public void triggerGone() {
        // Do nothing.
    }
}
