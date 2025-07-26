package ch.bfh.adaid.settings;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import ch.bfh.adaid.service.A11yService;

/**
 * A Quick Tile that when clicked commands the A11yService to immediately take the next valid
 * snapshot.
 *
 * Order of actions:
 *  1. Quick Tile is clicked, Intent is sent to A11yService.
 *  2. A11yService enables "next snapshot" mode and starts recording the screen layout.
 *  3. Once a valid (i.e. not the launcher) recording has been made it is immediately forwarded to
 *     the rule helper.
 *  4. Rule helper is brought to the foreground and displays the captured snapshot as usual.
 *
 * @author Niklaus Leuenberger
 */
public class SnapshotTileService extends TileService {
    private static final String TAG = "SnapshotTileService";

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
        }
        tile.updateTile();
    }

    /**
     * Called when user clicks on the tile.
     */
    @Override
    public void onClick() {
        super.onClick();
        doSnapshot();
    }

    /**
     * Send a "take snapshot" command to the a11y service.
     */
    private void doSnapshot() {
        Log.d(TAG, "sending snapshot command to a11y service");
        Intent intent = A11yService.getTakeNextSnapshotIntent(this);
        startService(intent);
    }
}
