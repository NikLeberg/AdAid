package ch.bfh.adaid.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import ch.bfh.adaid.action.ActionType;

/**
 * Rules on which all functionality od AdAid is based on. Defines what nodes will be searched in the
 * current view hierarchy and what action will be performed.
 * Has additional annotation for androidx.Room to turn this into a database table.
 *
 * @author Niklaus Leuenberger
 */
@Entity(tableName = "rule")
public class Rule {

    /**
     * Unique id of the rule.
     */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Displayed name of the rule.
     */
    @ColumnInfo(name = "name")
    public String name;

    /**
     * If rule is enabled or not.
     */
    @ColumnInfo(name = "enabled")
    public boolean enabled;

    /**
     * The app package name on which the rule applies.
     */
    @ColumnInfo(name = "app_id")
    public String appId;

    /**
     * View id for which should be searched for.
     *
     * Note: Common prefixes needn't be added to the view id.
     * --> Not "com.instagram.android:id/reel_viewer_subtitle" but just "reel_viewer_subtitle".
     */
    @ColumnInfo(name = "view_id")
    public String viewId;

    /**
     * Optional text that is searched inside view. Supports regex matching.
     */
    @ColumnInfo(name = "view_text")
    public String viewText;

    /**
     * The action to be performed when the rule is triggered.
     */
    @ColumnInfo(name = "action_type")
    public ActionType actionType;

    /**
     * Optional relative path to the view upon which should be acted.
     *
     * Encoding is as follows:
     * - "p", move one parent up
     * - "c[n]", move to nth child (0 indexed)
     * - "su", move up a sibling
     * - "sd", move down a sibling
     * These can be chained if written separated by a dot like: "p.p.su.c[2]".
     */
    @ColumnInfo(name = "relative_path")
    public String relativePath;

    /**
     * Default constructor.
     */
    @Ignore
    public Rule() {}

    /**
     * Simple constructor, mainly used for testing.
     *
     * @param name The name of the rule.
     */
    @Ignore
    public Rule(String name) {
        this.name = name;
    }

    /**
     * Complete constructor.
     *
     * @param name         The name of the rule.
     * @param enabled      True if rule is enabled.
     * @param appId        The app package name on which the rule applies.
     * @param viewId       The view id for which should be searched for.
     * @param viewText     Optional text that is inside searched view.
     * @param actionType   The action to be performed when the rule is triggered.
     * @param relativePath Optional relative path to the view upon which should be acted.
     */
    public Rule(String name, boolean enabled, String appId, String viewId, String viewText, ActionType actionType, String relativePath) {
        this.name = name;
        this.enabled = enabled;
        this.appId = appId;
        this.viewId = viewId;
        this.viewText = viewText;
        this.actionType = actionType;
        this.relativePath = relativePath;
    }

    /**
     * Check if rule is enabled.
     *
     * @return true if rule is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if the appId of the rule matched the given appId.
     *
     * @param appId The appId to check.
     * @return True if the appId matches, false otherwise.
     */
    @Ignore
    public boolean isMatchingAppId(String appId) {
        return this.appId.equals(appId);
    }

    /**
     * Get the complete view id how android expects it: com.app.app:id/xyz
     *
     * @return appId + ":id/" + viewId
     */
    @Ignore
    public String getCompleteViewId() {
        return appId + ":id/" + viewId;
    }

    /**
     * Checks if the viewText of the rule matches the given text.
     *
     * Comparison is case sensitive and is done using regex, so the text can contain wildcards and
     * the full regex syntax.
     * If a rule has no viewText, the text is by definition always matched.
     *
     * @param text The text to be matched against.
     * @return True if the text matches the viewText, false otherwise.
     */
    @Ignore
    public boolean isMatchingViewText(String text) {
        if (viewText != null && !viewText.isEmpty()) {
            return text.matches(viewText);
        } else {
            return true;
        }
    }
}
