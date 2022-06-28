package ch.bfh.adaid.db;

import java.util.List;

import ch.bfh.adaid.db.RuleDataSource.Error;

/**
 * Observer interface for the rule database.
 *
 * Please note that the callbacks of this interface will be called from the context of the database
 * thread. To edit the UI, use the runOnUiThread() method inside the callbacks.
 *
 * @author Niklaus Leuenberger
 */
public interface RuleObserver {

    /**
     * Called on initial load of the database or observer registration.
     *
     * Default implementation adds every rule one by one i.e. calls onRuleAdded for each rule.
     *
     * @param rules list of rules
     */
    default void onRuleLoad(List<Rule> rules) {
        for (Rule rule : rules) {
            onRuleAdded(rule);
        }
    }

    /**
     * Called on new rule addition.
     *
     * @param rule new rule
     */
    void onRuleAdded(Rule rule);

    /**
     * Called on changes to existing rules.
     *
     * Default implementation first removes the rule and adds it back i.e. calls onRuleRemoved
     * and onRuleAdded. This works as consumers of this interface should remove rules by id and
     * not by comparing object references.
     *
     * @param rule changed rule
     */
    default void onRuleChanged(Rule rule) {
        onRuleRemoved(rule);
        onRuleAdded(rule);
    }

    /**
     * Called on removal of rules.
     *
     * To check what rule to remove, use the rule's id and NOT the object reference.
     *
     * @param rule removed rule
     */
    void onRuleRemoved(Rule rule);

    /**
     * Called on errors with inserting or updating rules in the database.
     *
     * Default implementation just throws a RuntimeException.
     *
     * @param error the error that occurred
     * @param rule rule that caused the error
     */
    default void onRuleError(Error error, Rule rule) {
        throw new RuntimeException("onRuleError called with error: " + error.name());
    }
}
