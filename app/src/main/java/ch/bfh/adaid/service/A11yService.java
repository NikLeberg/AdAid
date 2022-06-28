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

/**
 * Accessibility service that executes the rules and its actions.
 *
 * @author Niklaus Leuenberger
 */
public class A11yService extends AccessibilityService implements RuleObserver {

    private static final String TAG = "A11yService";

    /**
     * List of rules as they are stored in the database. Gets updated with the implemented observer
     * callbacks.
     */
    private final ArrayList<RuleWithExtras> rules = new ArrayList<>();

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
     * Service lifecycle: The service is created by the system.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize possible actions.
        SwipeAction.initialize(getApplicationContext());
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
//        // DEBUG
//        walkNodeTree(root, 0);
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
    public void updateListenedPackages() {
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
     * Walks the node tree and its children and prints it to the log.
     *
     * @param node  Node to start with.
     * @param level Current level in the tree. Start with 0.
     */
    private void walkNodeTree(AccessibilityNodeInfo node, int level) {
        if (level == 0) {
            System.out.println("----------------------------------------------------");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                printNodeInfo(child, level);
                walkNodeTree(child, level + 1);
            }
        }
    }

    /**
     * Prints the node info to the log.
     *
     * @param node  Node to print.
     * @param level Current level in the tree.
     */
    private void printNodeInfo(AccessibilityNodeInfo node, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
        String text = node.getText() != null ? node.getText().toString() : "";
        System.out.format("Node: %s, Id: %s, Text: %s\n", node.getClassName(),
                node.getViewIdResourceName(), text);
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
            Log.d(TAG, "Rule " + rule.r.name + " with expected viewText " + rule.r.viewText +
                    " does not match actual viewText: " + viewText);
            return;
        }
        // All conditions are met, execute seen action and mark it as triggered.
        Log.d(TAG, "Triggering (seen) rule " + rule.r.name);
        rule.setTriggeredByCurrentEvent(true);
        rule.action.triggerSeen(node);
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
        updateListenedPackages();
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
        updateListenedPackages();
    }
}
