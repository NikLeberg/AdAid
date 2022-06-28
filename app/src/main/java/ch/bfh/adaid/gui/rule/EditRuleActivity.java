package ch.bfh.adaid.gui.rule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

import ch.bfh.adaid.db.Rule;

/**
 * Extended rule activity specifically for editing existing rules.
 *
 * @author Niklaus Leuenberger
 */
public class EditRuleActivity extends RuleActivity {

    /**
     * Intent extra key for starting this activity. Put as value the rule to edit as long.
     */
    public static final String EXTRA_RULE_ID = "ch.bfh.adaid.gui.rule.EditRuleActivity.ruleId";

    /**
     * Get an intent to start this activity to edit a rule.
     *
     * @param callingActivity The activity that called this activity.
     * @param ruleId          The id of the rule that will be edited with the activity.
     * @return created intent, use with startActivity(intent).
     */
    public static Intent getStartActivityIntent(Activity callingActivity, long ruleId) {
        Intent intent = new Intent(callingActivity, EditRuleActivity.class);
        intent.putExtra(EXTRA_RULE_ID, ruleId);
        return intent;
    }

    /**
     * Id of the rule that will be edited from this activity. Valid from onCreate() to onRuleLoad().
     */
    private long ruleId;


    /*
     * Lifecycle methods of RuleActivity
     */

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get rule id from intent.
        ruleId = getIntent().getLongExtra(EXTRA_RULE_ID, -1);
        if (ruleId == -1) {
            throw new IllegalArgumentException("No rule id given.");
        }
    }


    /*
     * RuleObserver interface.
     */

    /**
     * {@inheritDoc}
     * <p>
     * Called on initial load of the database or observer registration. Used to get the rule by id.
     *
     * @param rules list of rules
     */
    @Override
    public void onRuleLoad(List<Rule> rules) {
        // This will be called with every rule on load. We pick out the rule we want to edit.
        rule = rules.stream().filter(r -> r.id == ruleId).findFirst().orElse(null);
        if (rule == null) {
            throw new IllegalArgumentException("Rule with id " + ruleId + " not found.");
        }
        // Initialize form. But as this callback is called from the RuleDataSource.Executor
        // thread, we need to change to the UI thread.
        runOnUiThread(this::initFormFromRule);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We are only editing an existing rule, we don't expect a rule to be added.
     *
     * @param rule unused
     * @throws UnsupportedOperationException always
     */
    @Override
    public void onRuleAdded(Rule rule) throws UnsupportedOperationException {
        // We are only editing a rule, we don't expect a new rule to be added.
        throw new UnsupportedOperationException("Unexpected onRuleAdded call.");
    }

    /**
     * Called when a rule is changed.
     *
     * @param rule changed rule
     */
    @Override
    public void onRuleChanged(Rule rule) {
        // This will be called when a rule is changed. If it is the rule we are editing then we know
        // that our changes were saved to the database and we can exit the activity.
        if (rule.id == this.rule.id) {
            // Again, run on UI thread.
            runOnUiThread(this::finish);
        }
    }

    /**
     * Called when a rule is removed.
     *
     * @param rule removed rule
     */
    @Override
    public void onRuleRemoved(Rule rule) {
        // This will be called when a rule is removed. If it is the rule we are editing then we know
        // that it was deleted from the database and we can exit the activity.
        if (rule.id == this.rule.id) {
            // Again, run on UI thread.
            runOnUiThread(this::finish);
        }
    }


    /*
     * Button methods implementation.
     */

    /**
     * Called when the save button is clicked.
     * <p>
     * This will validate the rule and if valid, change it in the database. After it has been
     * successfully changed the onRuleChanged() callback will be called. It finishes this activity.
     */
    @Override
    protected void saveRule() {
        setRuleFromView();
        if (formValid) {
            data.change(this, rule);
        }
    }

    /**
     * Called when the delete button is clicked.
     * <p>
     * This will delete the rule from the database. After it has been successfully deleted the
     * onRuleRemoved() callback will be called. It finishes this activity.
     */
    @Override
    protected void deleteRule() {
        data.remove(this, rule);
    }

}