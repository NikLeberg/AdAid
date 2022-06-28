package ch.bfh.adaid.gui.rule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.List;

import ch.bfh.adaid.R;
import ch.bfh.adaid.db.Rule;

/**
 * Extended rule activity specifically for adding new rules.
 *
 * @author Niklaus Leuenberger
 */
public class NewRuleActivity extends RuleActivity {

    /**
     * Get an intent to start this activity to create a rule.
     *
     * @param callingActivity The activity that called this activity.
     * @return created intent, use with startActivity(intent).
     */
    public static Intent getStartActivityIntent(Activity callingActivity) {
        return new Intent(callingActivity, NewRuleActivity.class);
    }


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

        // Button to delete rule is not needed for a new rule.
        findViewById(R.id.buttonDelete).setVisibility(View.GONE);

        // Instantiate an empty rule.
        rule = new Rule();
    }


    /*
     * RuleObserver interface.
     */

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to have default implementation not call onRuleAdded() on load. Do nothing.
     *
     * @param rules unused
     */
    @Override
    public void onRuleLoad(List<Rule> rules) {
        // This will be called with every rule on load. We don't need to do anything here.
    }

    /**
     * Called when a new rule is added.
     *
     * @param rule the new rule
     */
    @Override
    public void onRuleAdded(Rule rule) {
        // This will be called when a rule is added. Presumably it is the rule we are creating and
        // the user pressed the save button. We can exit the activity.
        runOnUiThread(this::finish);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We are only creating a new rule, we don't expect a rule to be changed.
     *
     * @param rule unused
     * @throws UnsupportedOperationException always
     */
    @Override
    public void onRuleChanged(Rule rule) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Unexpected onRuleChanged call.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * We are only creating a new rule, we don't expect this rule to be removed as it haven't even
     * been added to the database yet.
     *
     * @param rule unused
     * @throws UnsupportedOperationException always
     */
    @Override
    public void onRuleRemoved(Rule rule) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Unexpected onRuleRemoved call.");
    }


    /*
     * Button methods implementation.
     */

    /**
     * Called when the save button is clicked.
     * <p>
     * This will validate the rule and if valid, add it to the database. After it has been
     * successfully added to the db the onRuleAdded() callback will be called. It finishes this
     * activity.
     */
    @Override
    protected void saveRule() {
        setRuleFromView();
        if (formValid) {
            data.add(this, rule);
        }
    }

    /**
     * Called when the delete button is clicked.
     * <p>
     * The delete button was made inaccessible in the onCreate() method. This method should never be
     * called.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    protected void deleteRule() throws UnsupportedOperationException {
        // Button to delete rule is not needed for a new rule.
        throw new UnsupportedOperationException("Unexpected deleteRule call.");
    }

}