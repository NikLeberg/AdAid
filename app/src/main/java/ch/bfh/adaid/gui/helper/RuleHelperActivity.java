package ch.bfh.adaid.gui.helper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ch.bfh.adaid.R;
import ch.bfh.adaid.gui.rule.RuleActivity;
import ch.bfh.adaid.service.A11yService;

/**
 * Activity that implements a rule helper mechanism. On start it shows a dialog that allows the user
 * to enable the mechanism. On enabling, the activity minimizes the app and tells the a11y service
 * to start recording the screen layout. Also a notification is shown that when tapped tells the
 * a11y service to stop recording and send data to this minimized activity. On reception it creates
 * a list of the views as seen in the recording. When the user clicks on such a list item / view,
 * the data it contains is sent back to the previous rule (create / edit) activity.
 *
 * @author Niklaus Leuenberger
 */
public class RuleHelperActivity extends AppCompatActivity implements ViewTreeRecyclerViewAdapter.ItemClickListener {

    public static final String EXTRA_SNAPSHOT_KEY = "ch.bfh.adaid.gui.helper.RuleHelperActivity.EXTRA_SNAPSHOT_KEY";
    private AlertDialog dialog; // if non null, the dialog is shown.
    private FlattenedViewTree viewTree; // if non null, the view tree is shown in the recycler view.
    private ViewTreeRecyclerViewAdapter adapter;

    /**
     * Get an intent to start this activity.
     *
     * @param context The activity that called this activity.
     * @return created intent, use with startActivity(intent).
     */
    public static Intent getStartActivityIntent(Context context) {
        return new Intent(context, RuleHelperActivity.class);
    }

    /**
     * Get an intent to send data to this activity.
     *
     * @param context  The context that called this activity.
     * @param viewTree A snapshot of the flattened view tree hierarchy as seen by the a11y service.
     * @return created intent, use with startActivity(intent).
     */
    public static Intent getSetDataIntent(Context context, FlattenedViewTree viewTree) {
        Intent intent = new Intent(context, RuleHelperActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_SNAPSHOT_KEY, viewTree);
        return intent;
    }


    /*
     * Lifecycle methods of AppCompatActivity
     */

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rule_helper);

        // Set correct animation for this activity.
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        // Either this activity is created for the first time and shows a help dialog, or it already
        // received a view hierarchy snapshot from the a11y service and is showing it. If the later,
        // the intent of this activity contains the view tree snapshot.
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_SNAPSHOT_KEY)) {
            // If the intent contains a view tree snapshot, process it. This happens when the device
            // was rotated and the activity gets recreated.
            processSnapshotIntent(intent);
        } else {
            // If there is no view tree snapshot, show the help dialog for enabling the mechanism.
            // But only if the a11y service is enabled. Otherwise show a toast and close.
            if (!A11yService.isServiceEnabled(this)) {
                Toast.makeText(this, R.string.rule_helper_dialog_service_not_enabled, Toast.LENGTH_LONG).show();
                finish();
            } else {
                showHelpDialog();
            }
        }
    }

    /**
     * Called when this activity is about to exit the screen.
     */
    @Override
    public void finish() {
        super.finish();
        // Slide out this activity to the right and fade in the previous (rule edit / create).
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    /**
     * Called when the system stops the activity.
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Dismiss the help dialog if it is still showing to prevent window leakage.
        dismissHelpDialog();
    }

    /**
     * Called on the reception of a intent while the activity is already running.
     *
     * @param intent New intent from the a11y service with the captured view tree snapshot.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // If the intent contains a view tree snapshot, process it and store the intent in the
        // activity. This is so that it can be used in onCreate when the activity is recreated on
        // rotation.
        if (intent.hasExtra(EXTRA_SNAPSHOT_KEY)) {
            dismissHelpDialog();
            processSnapshotIntent(intent);
            setIntent(intent);
        }
    }

    /**
     * Called when the title bar is pressed.
     *
     * @param item The menu item that was pressed.
     * @return true if the menu item was handled, false otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // If the back button is pressed, finish this activity.
            finish(); // plays an animation
            return true;
        }
        // Unknown item, let super handle it.
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the user clicks on a view in the view tree that is managed by the adapter.
     *
     * @param position Position that was clicked.
     */
    @Override
    public void onItemClick(int position) {
        // Get the view tree node at the given position and send its data back to the rule activity.
        FlattenedViewTree.SimpleView view = adapter.getItem(position);
        setResult(RESULT_OK, RuleActivity.getResultIntent(view.id, view.text, viewTree.packageName));
        finish();
    }


    /*
     * Private helper methods.
     */

    /**
     * Show the initial help dialog to activate the help mechanism.
     */
    private void showHelpDialog() {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.rule_helper_dialog_message)
                .setPositiveButton(R.string.rule_helper_dialog_positive,
                        // Start the helper mechanism.
                        (dialog, which) -> minimizeToNotification())
                .setNegativeButton(R.string.rule_helper_dialog_negative,
                        // Close this activity, user did not want to activate mechanism.
                        (dialog, which) -> finish())
                // TODO: implement logic for last stored snapshot
//                .setNeutralButton(R.string.rule_helper_dialog_neutral, (dialog, which) -> {
//                })
                .setCancelable(false)
                .setOnDismissListener(dialog -> this.dialog = null);
        dialog = builder.create();
        // TODO: implement logic for last stored snapshot
        // Disable the neutral button if no last snapshot is available. The button is disabled in
        // the onShow callback because it needs to have been created before it can be disabled.
        // Source: https://stackoverflow.com/q/568855/16034014
//        dialog.setOnShowListener((d) -> {
//            Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
//            if (button != null) {
//                button.setEnabled(false);
//            }
//        });
        // Show the dialog.
        dialog.show();
    }

    /**
     * Dismiss the help dialog if it is showing.
     */
    private void dismissHelpDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * Minimizes the activity to a notification and sets the a11y service to take snapshots.
     */
    private void minimizeToNotification() {
        // Create and activate notification.
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(RuleHelperNotificationBuilder.NOTIFICATION_ID,
                RuleHelperNotificationBuilder.build(this));
        // Minimize app / go to the home screen.
        Intent minimize = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(minimize);
        // Tell the a11y service to start recording snapshots.
        Intent startRecording = A11yService.getStartRecordingIntent(this);
        startService(startRecording);
    }

    /**
     * Processes a view tree snapshot intent. Received either directly from the a11y service or from
     * the activity on recreation due to device rotation.
     *
     * @param intent Intent with the view tree snapshot.
     */
    private void processSnapshotIntent(Intent intent) {

        // Get view tree from intent.
        viewTree = (FlattenedViewTree) intent.getSerializableExtra(EXTRA_SNAPSHOT_KEY);
        if (viewTree == null) {
            throw new IllegalArgumentException("No view tree given.");
        }

        // Set the name and icon of the app.
        ((TextView) findViewById(R.id.packageName)).setText(viewTree.packageName);
        try {
            Drawable icon = getPackageManager().getApplicationIcon(viewTree.packageName);
            ((ImageView) findViewById(R.id.packageIcon)).setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore and don't show any icon.
            Log.e("RuleHelperActivity", "Could not find icon for given package " + viewTree.packageName);
        }

        // Set up the RecyclerView with an Adapter that has the view tree as a list of views.
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ViewTreeRecyclerViewAdapter(this, viewTree);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));

        // Make the header visible as it has been filled with data now.
        findViewById(R.id.headerContentContainer).setVisibility(View.VISIBLE);
    }
}
