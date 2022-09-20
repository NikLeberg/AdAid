package ch.bfh.adaid.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

import ch.bfh.adaid.action.SwipeAction;
import ch.bfh.adaid.db.Rule;
import ch.bfh.adaid.db.RuleDataSource;
import ch.bfh.adaid.db.RuleObserver;
import ch.bfh.adaid.gui.helper.FlattenedViewTree;
import ch.bfh.adaid.gui.helper.RuleHelperActivity;

/**
 * Accessibility service that executes the rules and its actions.
 *
 * @author Niklaus Leuenberger
 */
public class A11yService extends AccessibilityService implements RuleObserver {

    private static final String TAG = "A11yService";
    private static final String EXTRA_RECORDING_COMMAND_KEY = "ch.bfh.adaid.service.A11yService.RECORDING_COMMAND";
    private static final String EXTRA_TAKE_SNAPSHOT_KEY = "ch.bfh.adaid.service.A11yService.TAKE_SNAPSHOT";

    /**
     * List of rules as they are stored in the database. Gets updated with the implemented observer
     * callbacks.
     */
    private final ArrayList<RuleWithExtras> rules = new ArrayList<>();

    /**
     * Flag to indicate if the service is currently recording the screen layout i.e. creating
     * FlattenedViewTree objects.
     */
    private boolean isRecording = false;

    /**
     * While recording the screen layout, this attribute reflects the most up to date recording.
     */
    private FlattenedViewTree lastViewTree;

    /**
     * Checks if this service is enabled in the accessibility settings.
     *
     * @param context Context of the application.
     * @return True if this accessibility service is enabled, false otherwise.
     */
    public static boolean isServiceEnabled(Context context) {
        final String serviceName = context.getPackageName() + "/" + A11yService.class.getCanonicalName();
        String enabledA11yServices = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledA11yServices != null && enabledA11yServices.contains(serviceName);
    }

    /**
     * Creates intent that takes the user to the accessibility settings and highlights the service.
     *
     * @param context Context of the application.
     * @return created intent, use with startActivity(intent).
     */
    public static Intent getOpenSettingIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        // Use undocumented android API to let the relevant setting item light up.
        // Source: https://stackoverflow.com/a/63214655/16034014
        final String serviceName = context.getPackageName() + "/" + A11yService.class.getCanonicalName();
        Bundle bundle = new Bundle();
        bundle.putString(":settings:fragment_args_key", serviceName);
        intent.putExtra(":settings:fragment_args_key", serviceName);
        intent.putExtra(":settings:show_fragment_args", bundle);
        return intent;
    }

    /**
     * Creates intent that tells this a11y service to start or stop the recording the screen layout.
     *
     * @param context Context of the application.
     * @param start   True if the recording should start, false if it should stop.
     * @return created intent, use with startService(intent).
     */
    public static Intent getRecordingCommandIntent(Context context, boolean start) {
        Intent intent = new Intent(context, A11yService.class);
        intent.putExtra(EXTRA_RECORDING_COMMAND_KEY, start);
        return intent;
    }

    /**
     * Creates intent that tells this a11y service to take a snapshot of the screen layout.
     * This also automatically stops the recording.
     *
     * @param context Context of the application.
     * @return created intent, use with startService(intent).
     */
    public static Intent getTakeSnapshotIntent(Context context) {
        Intent intent = getRecordingCommandIntent(context, false);
        intent.putExtra(EXTRA_TAKE_SNAPSHOT_KEY, true);
        return intent;
    }

    /**
     * Service lifecycle: The service is created by the system.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize possible actions.
        SwipeAction.initialize(getApplicationContext());
    }

    /**
     * Service lifecycle: The service received a start command, i.e. intent.
     * Used to communicate from activities -> service with intents.
     * <p>
     * Source: https://stackoverflow.com/a/41433717/16034014
     *
     * @param intent  Intent that was received.
     * @param flags   Flags that were set in the intent.
     * @param startId Unique integer representing a specific request to start.
     * @return value from super implementation
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // If we should take a snapshot and were recording before, send the recorded snapshot to
            // the helper activity. This in turn also brings the activity to the front.
            boolean takeSnapshot = intent.getBooleanExtra(EXTRA_TAKE_SNAPSHOT_KEY, false);
            if (takeSnapshot && isRecording && lastViewTree != null) {
                startActivity(RuleHelperActivity.getSetDataIntent(this, lastViewTree));
            }
            // If we should control the recording, set flag and also set the service options so that
            // either events for all packages are received or only those with existing rules.
            if (intent.hasExtra(EXTRA_RECORDING_COMMAND_KEY)) {
                isRecording = intent.getBooleanExtra(EXTRA_RECORDING_COMMAND_KEY, false);
                if (isRecording) {
                    listenToAllPackages();
                } else {
                    listenToPackagesWithRules();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Service lifecycle: The system connected to the service.
     */
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        // Connect to the rule database and register as observer.
        RuleDataSource data = new RuleDataSource(getApplicationContext());
        data.addObserver(this);
    }

    /**
     * Service lifecycle: The system produced an accessibility event.
     *
     * @param event The produced accessibility event.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Process only window content change events. The android system only ever sends events
        // according to the configuration in {@link a11y_service_config.xml}.
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        // Check wether this event has a source node root that is not null. Normally all window
        // content change events have a root node, but mysteriously some don't. So check this.
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Log.e(TAG, "Window root is null for event with source node: " + event.getSource());
            return;
        }
        // If we are recording the screen layout, take a snapshot of the current layout.
        if (isRecording) {
            doSnapshot(root);
        }
        // Process all rules for this event.
        processRulesForEvent(event.getPackageName().toString(), root);
    }

    /**
     * Service lifecycle: The system is interrupting the service.
     */
    @Override
    public void onInterrupt() {
        // This method is called when the system wants to interrupt the feedback this service is
        // providing. Will possibly be called many times over the lifecycle of the service. But as
        // there is no constant / lengthily feedback, nothing has to be interrupted.
    }

    /**
     * Update packages to listen for events in the accessibility service configuration.
     */
    private void listenToPackagesWithRules() {
        // The xml configuration {@link a11y_service_config.xml} has no default value for apps to
        // listen to. So we receive events for all apps. Optimize this by only listening to apps
        // that have rules.
        ArrayList<String> packages = new ArrayList<>();
        for (RuleWithExtras rule : rules) {
            if (!packages.contains(rule.r.appId)) {
                packages.add(rule.r.appId);
            }
        }
        Log.d(TAG, "updateListenedPackages: now listening to: " + packages);
        AccessibilityServiceInfo info = getServiceInfo();
        info.packageNames = packages.toArray(new String[0]);
        setServiceInfo(info);
    }

    /**
     * Set service configuration to listen for accessibility events of all packages.
     */
    private void listenToAllPackages() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.packageNames = null;
            setServiceInfo(info);
        }
    }

    /**
     * Process all rules for the given event.
     *
     * @param appId The currently opened app.
     * @param root  The root node of all accessibility nodes (i.e. the container view).
     */
    private void processRulesForEvent(String appId, AccessibilityNodeInfo root) {
        // Iterate over all rules and process them.
        for (RuleWithExtras rule : rules) {
            processRuleForEvent(rule, appId, root);
        }
    }

    /**
     * Process a single rule for the given event.
     *
     * @param rule  The rule to process.
     * @param appId The currently opened app.
     * @param root  The root node of all accessibility nodes (i.e. the container view).
     */
    private void processRuleForEvent(RuleWithExtras rule, String appId, AccessibilityNodeInfo root) {
        // Check if the rule applies to the current app.
        if (!rule.r.isMatchingAppId(appId)) {
            return;
        }
        // Search for the node(s) that match the rule.
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(rule.r.getCompleteViewId());
        // Actions only make sense if just one single node is found. So ignore events when multiple
        // nodes are found. But as this event is now ignored, also reset the triggered flag and
        // potentially run gone action if previous event contained the node i.e. triggered the rule.
        if (nodes.size() != 1) {
            if (rule.wasTriggeredByLastEvent()) {
                Log.d(TAG, "Triggering (gone) rule " + rule.r.name);
                rule.action.triggerGone();
            }
            rule.setTriggeredByCurrentEvent(false);
            return;
        }
        // Only process the rule it has not been triggered yet.
        if (rule.wasTriggeredByLastEvent()) {
            return;
        }
        // Exactly one node found and was not triggered before. Process the rule for found node.
        processRuleForNode(rule, nodes.get(0));
    }

    /**
     * Process a single rule for a single node.
     *
     * @param rule The rule to process.
     * @param node The node to process the rule for.
     */
    private void processRuleForNode(RuleWithExtras rule, AccessibilityNodeInfo node) {
        // Check if the node text matches (as regex).
        String viewText = node.getText() == null ? "" : node.getText().toString();
        if (!rule.r.isMatchingViewText(viewText)) {
            return;
        }
        // Conditions to trigger are met, process the relative path if set.
        node = processRelativePath(node, rule.r.relativePath);
        if (node == null) {
            Log.e(TAG, "Error while processing relative path: " + rule.r.relativePath);
            return;
        }
        // All conditions are met, execute seen action and mark it as triggered.
        Log.d(TAG, "Triggering (seen) rule " + rule.r.name);
        rule.setTriggeredByCurrentEvent(true);
        rule.action.triggerSeen(node);
    }

    /**
     * Take a snapshot of the current screen / view hierarchy.
     *
     * @param root The root node of all accessibility nodes (i.e. the container view).
     */
    private void doSnapshot(AccessibilityNodeInfo root) {
        // Convert node hierarchy to simplified and flattened list. But exclude the system ui.
        String packageName = root.getPackageName().toString();
        if (packageName.matches("com\\.android\\.system.*")) {
            Log.d(TAG, "Skipping snapshotting of system package: " + packageName);
            return;
        }
        lastViewTree = new FlattenedViewTree(root);
    }

    /**
     * Process the optional relative path of the rule.
     *
     * @param node         Node that triggered the rule, starting point of relative path.
     * @param relativePath The relative path from the rule.
     * @return The relative node. Or null on error.
     */
    public AccessibilityNodeInfo processRelativePath(AccessibilityNodeInfo node, String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return node;
        }
        for (String pathArg : relativePath.split("\\.")) {
            if (pathArg.equals("p")) {
                node = processRelativeParent(node);
            } else if (pathArg.startsWith("c[")) {
                node = processRelativeChild(node, pathArg);
            } else if (pathArg.equals("su")) {
                node = processRelativeSiblingUp(node);
            } else if (pathArg.equals("sd")) {
                node = processRelativeSiblingDown(node);
            } else {
                node = null;
            }
        }
        return node;
    }

    /**
     * Process the "p" relative path argument and move a parent up.
     *
     * @param node Child node.
     * @return Parent node. Or null on error.
     */
    public AccessibilityNodeInfo processRelativeParent(AccessibilityNodeInfo node) {
        return (node == null) ? null : node.getParent();
    }

    /**
     * Process the "c[nth]" relative path argument and move to the nth child.
     *
     * @param node Parent node.
     * @param arg  Relative path argument.
     * @return The child at position n. Or null on error.
     */
    public AccessibilityNodeInfo processRelativeChild(AccessibilityNodeInfo node, String arg) {
        if (node == null) {
            return null;
        }
        try {
            int i = Integer.parseInt(arg.replaceAll("[^0-9]", ""));
            if (i >= 0 && node.getChildCount() > i) {
                return node.getChild(i);
            }
        } catch (NumberFormatException ignore) {
        }
        return null;
    }

    /**
     * Process the "su" relative path argument and move to the sibling one up.
     *
     * @param node Origin node.
     * @return Sibling one up. Or null on error.
     */
    public AccessibilityNodeInfo processRelativeSiblingUp(AccessibilityNodeInfo node) {
        if (node != null) {
            // To get the sibling first go to the common parent and iterate over its children. When
            // we find ourself we go one child back witch is the searched sibling.
            AccessibilityNodeInfo p = processRelativeParent(node);
            for (int i = 1; p != null && i < p.getChildCount(); ++i) {
                if (node.equals(p.getChild(i))) {
                    return p.getChild(i - 1);
                }
            }
        }
        return null;
    }

    /**
     * Process the "sd" relative path argument and move to the sibling one down.
     *
     * @param node Origin node.
     * @return Sibling one down. Or null on error.
     */
    public AccessibilityNodeInfo processRelativeSiblingDown(AccessibilityNodeInfo node) {
        if (node != null) {
            // To get the sibling first go to the common parent and iterate over its children. When
            // we find ourself we go one child further witch is the searched sibling.
            AccessibilityNodeInfo p = processRelativeParent(node);
            for (int i = 0; p != null && i < p.getChildCount() - 1; ++i) {
                if (node.equals(p.getChild(i))) {
                    return p.getChild(i + 1);
                }
            }
        }
        return null;
    }

    /**
     * RuleObserver interface: Called when a new rule is added.
     * <p>
     * Because this service doesn't need the fine granularity the RuleObserver has, we can mostly
     * just use the default implementation. Only onRuleAdded() and onRuleRemoved() manage an
     * ArrayList of the rules and their additional information.
     *
     * @param rule new rule
     */
    @Override
    public void onRuleAdded(Rule rule) {
        // If rule is added that is not enabled, ignore it.
        if (!rule.isEnabled()) {
            Log.d(TAG, "Rule " + rule.name + " is not enabled, ignoring rule.");
            return;
        }
        // Rule has been added to the database, add it to the array list.
        rules.add(new RuleWithExtras(rule, this));
        // This rule may be the first for a specific app. Update listened apps.
        listenToPackagesWithRules();
    }

    /**
     * RuleObserver interface: Called when a rule is removed.
     * <p>
     * Because this service doesn't need the fine granularity the RuleObserver has, we can mostly
     * just use the default implementation. Only onRuleAdded() and onRuleRemoved() manage an
     * ArrayList of the rules and their additional information.
     *
     * @param rule removed rule
     */
    @Override
    public void onRuleRemoved(Rule rule) {
        // Rule has been removed from the database, remove it from the array list.
        rules.removeIf(r -> r.r.id == rule.id);
        // This may have removed the last rule for a specific app. Update listened apps.
        listenToPackagesWithRules();
    }
}
