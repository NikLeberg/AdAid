package ch.bfh.adaid.gui.helper;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import ch.bfh.adaid.R;
import ch.bfh.adaid.service.A11yService;

/**
 * Builds a notification that tells the a11y service to snapshot the current view layout.
 * <p>
 * Source:
 * <a href="https://developer.android.com/training/notify-user/build-notification">Create a notification</a>
 *
 * @author Niklaus Leuenberger
 */
public class RuleHelperNotificationBuilder {

    public static final String CHANNEL_ID = "ch.bfh.adaid.gui.rule.RuleHelperNotificationBuilder";
    public static final int NOTIFICATION_ID = 1;

    /**
     * Build a notification for the view id finder.
     *
     * @param context The context of the current activity.
     * @return The built notification. Call notify() on it to activate.
     */
    @SuppressLint("LaunchActivityFromNotification")
    public static Notification build(Context context) {
        // Create the notification channel first.
        createNotificationChannel(context);
        // Set parameters for notification builder: Big text style, not dismissible, auto cancel.
        CharSequence title = context.getString(R.string.rule_helper_notification_title);
        CharSequence text = context.getString(R.string.rule_helper_notification_text);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true);
        // Set intent that tells the a11y service to snapshot the current view layout. Lint
        // complains because content intents should start the UI or the app and not a service. But
        // here it is intended because the service needs to snapshot the current screen before it
        // will then start the activity. From a user perspective, the notification is still starting
        // the app.
        PendingIntent takeSnapshotIntent = PendingIntent.getService(context, 0,
                A11yService.getTakeSnapshotIntent(context),
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(takeSnapshotIntent);
        // Allow the user to dismiss the notification. Instructs the a11y service to stop recording.
        PendingIntent stopRecordingIntent = PendingIntent.getService(context, 1,
                A11yService.getRecordingCommandIntent(context, false),
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        builder.setDeleteIntent(stopRecordingIntent);
        return builder.build();
    }

    /**
     * Create a general notification channel. Required for SDK 26+.
     *
     * @param context The context of the current activity.
     */
    private static void createNotificationChannel(Context context) {
        // Create the notification channel. Multiple calls are allowed.
        CharSequence name = context.getString(R.string.rule_helper_notification_channel_name);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        // Register the channel with the system. Changes to priority are not possible after this.
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.createNotificationChannel(channel);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private RuleHelperNotificationBuilder() {
    }
}
