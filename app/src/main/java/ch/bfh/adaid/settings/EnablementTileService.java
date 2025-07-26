package ch.bfh.adaid.settings;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import ch.bfh.adaid.service.A11yService;

/**
 * Quick settings tile that toggles the a11y service on or off.
 *
 * @author Niklaus Leuenberger
 */
public class EnablementTileService extends TileService {
    private static final String TAG = "EnablementTileService";

    /**
     * Called when the system adds the tile for the first time.
     */
    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Tile tile = getQsTile();
        boolean a11yEnabled = A11yService.isServiceEnabled(this);
        tile.setState(a11yEnabled ? Tile.STATE_ACTIVE : Tile.STATE_UNAVAILABLE);
        tile.updateTile();
    }

    /**
     * Called when the system wants to check the state of the quick tile.
     */
    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        boolean a11yEnabled = A11yService.isServiceEnabled(this);
        int state = tile.getState();
        if (!a11yEnabled) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else if (state == Tile.STATE_UNAVAILABLE) {
            tile.setState(Tile.STATE_ACTIVE);
            sendOnOff(true);
        }
        tile.updateTile();
    }

    /**
     * Called when user clicks on the tile.
     */
    @Override
    public void onClick() {
        super.onClick();
        // Toggle the state.
        Tile tile = getQsTile();
        int state = tile.getState();
        if (state == Tile.STATE_ACTIVE) {
            tile.setState(Tile.STATE_INACTIVE);
            sendOnOff(false);
        } else if (state == Tile.STATE_INACTIVE) {
            tile.setState(Tile.STATE_ACTIVE);
            sendOnOff(true);
        }
        tile.updateTile();
    }

    /**
     * Called when user removes the tile.
     */
    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        // If Tile is removed, ensure that the service is enabled.
        sendOnOff(true);
    }

    /**
     * Send an on or off command to the a11y service.
     *
     * @param on True if the service should be turned on, false otherwise.
     */
    private void sendOnOff(boolean on) {
        Log.d(TAG, "sending " + (on ? "on" : "off") + " command to a11y service");
        Intent intent = A11yService.getQuickTileOnOffIntent(this, on);
        startService(intent);
    }
}
