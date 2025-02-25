package ch.bfh.adaid.action;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;

/**
 * Action to block content on screen i.e. overlay a black box.
 *
 * Source for basic overlay functionality: https://github.com/thbecker/android-accessibility-overlay
 *
 * @author Niklaus Leuenberger
 */
public class BlockAction extends Action {
    private static final String TAG = "BlockAction";

    private final WindowManager windowManager;
    private final LinearLayout overlay;
    private final WindowManager.LayoutParams layoutParams;
    private OverlayUpdateHandler updater;

    /**
     * Construct a new block action.
     *
     * @param service The accessibility service. Used to change service configuration.
     */
    public BlockAction(AccessibilityService service) {
        super(service);
        windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        // Construct the basic overlay as LinearLayout. The size and position gets set with
        // LayoutParams that are created and changed dynamically.
        overlay = new LinearLayout(service.getBaseContext());
        overlay.setBackgroundColor(Color.BLACK);
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                PixelFormat.TRANSLUCENT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
    }

    /**
     * Display an overlay over the node.
     *
     * @param node Node that will be overlayed.
     */
    @Override
    public void triggerSeen(AccessibilityNodeInfo node) {
        // only take action when updater hasn't been initialised yet
        if(updater == null){
            showOverlay();
            updater = new OverlayUpdateHandler(this, node);
            updater.sendEmptyMessage(OverlayUpdateHandler.RUN);
        }
    }

    /**
     * Trigger on gone missing of node. Ignored because
     * the update handler does a better job at it.
     */
    @Override
    public void triggerGone() {
        Log.v(TAG, "ignoring triggerGone call");
    }

    /**
     * Show the overlay and add it to the window manager.
     */
    private void showOverlay() {
        overlay.setVisibility(View.VISIBLE);
        windowManager.addView(overlay, layoutParams);
    }

    /**
     * Update the position and size of the overlay.
     *
     * @param node a11y node for corresponding view that should be blocked.
     */
    private void updateOverlay(AccessibilityNodeInfo node) {
        Rect boundsInScreen = new Rect();
        node.getBoundsInScreen(boundsInScreen);
        layoutParams.x = boundsInScreen.left;
        layoutParams.y = boundsInScreen.top;
        layoutParams.width = boundsInScreen.width();
        layoutParams.height = boundsInScreen.height();
        windowManager.updateViewLayout(overlay, layoutParams);
    }

    /**
     * Remove the overlay from the window manager.
     */
    private void removeOverlay() {
        windowManager.removeView(overlay);
        updater = null;
    }

    /**
     * Handler to call the update of overlay every few ms.
     *
     * Source: https://stackoverflow.com/a/13100626/16034014
     */
    private static class OverlayUpdateHandler extends Handler {

        /**
         * Rate at witch the overlay should be updated.
         */
        private static final long BLOCK_OVERLAY_UPDATE_RATE = 10; // ms

        /**
         * Message id to trigger or stop endless loop.
         */
        protected static final int RUN = 0;

        /**
         * Access the outer class only through a weak reference. This eliminates a leak as discussed
         * here: https://stackoverflow.com/q/11407943/16034014
         */
        private final WeakReference<BlockAction> blockActionReference;

        private final AccessibilityNodeInfo nodeToBlock;

        /**
         * Construct a new update handler that updates the a11y node and the overlay.
         *
         * @param blockAction Reference to the owning block action.
         * @param nodeToBlock a11y node that gets updated every few ms.
         */
        public OverlayUpdateHandler(BlockAction blockAction, AccessibilityNodeInfo nodeToBlock) {
            super(Looper.getMainLooper());
            this.blockActionReference = new WeakReference<>(blockAction);
            this.nodeToBlock = nodeToBlock;
        }

        /**
         * Message handler, updates the a11y node and overlay.
         *
         * @param msg Ignored, should always be RUN (= 0).
         */
        @Override
        public void handleMessage(Message msg) {
            // A11y nodes can be refreshed outside of the usual service events. This is used here to
            // continuously update the position and size of the node and place the overlay
            // accordingly. If a node is no longer visible the refresh returns false and the update
            // handler loop is stopped.
            BlockAction action = blockActionReference.get();
            if (action != null) {
                if (nodeToBlock.refresh()) {
                    action.updateOverlay(nodeToBlock);
                    sendEmptyMessageDelayed(RUN, BLOCK_OVERLAY_UPDATE_RATE);
                } else {
                    removeMessages(RUN);
                    action.removeOverlay();
                }
            } else {
                removeMessages(RUN);
            }
        }
    }
}
