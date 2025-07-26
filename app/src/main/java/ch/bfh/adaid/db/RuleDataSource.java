package ch.bfh.adaid.db;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Data source for the rules. Observers can register to be notified when rules are added, removed or
 * updated and they also can edit the rules themselves.
 *
 * @author Niklaus Leuenberger
 */
public class RuleDataSource {

    private final RuleDao ruleDao; // rule database access object
    private static final ArrayList<RuleObserver> observers = new ArrayList<>();

    /**
     * Error states on method calls. The observer that issued the call will be notified of the error
     * in the onRuleError() method.
     */
    public enum Error {
        ADD_RULE_FAILED,
        UPDATE_RULE_FAILED,
        DELETE_RULE_FAILED
    }

    /**
     * Executor to run database operations outside of the main thread.
     */
    private static final int NUMBER_OF_THREADS = 2; // number of background executor threads
    public static final ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    /**
     * Data source for the rule database. Manages asynchronous access to the database and notifies
     * observers when the database is changed.
     *
     * @param context Application context for creating the database.
     */
    public RuleDataSource(Context context) {
        RuleDatabase db = RuleDatabase.getDatabase(context);
        ruleDao = db.ruleDao();
    }

    /**
     * Data source for the rule database. Special constructor that allows to inject a rule database.
     *
     * @param ruleDao The rule database access object.
     */
    public RuleDataSource(RuleDao ruleDao) {
        this.ruleDao = ruleDao;
    }

    /**
     * Add an observer that gets notified when the database is changed.
     *
     * @param observer The observer to add.
     */
    public void addObserver(RuleObserver observer) {
        observers.add(observer);
        // On initial addition as observer, send all available rules to the observer.
        executor.execute(() -> {
            List<Rule> rules = ruleDao.getAll();
            observer.onRuleLoad(rules);
        });
    }

    /**
     * Remove an observer.
     *
     * @param observer The observer to remove.
     */
    public void removeObserver(RuleObserver observer) {
        observers.remove(observer);
    }

    /**
     * Add rule to the database. On success all observers (also the one that added the rule) are
     * notified.
     *
     * @param observer The observer that adds the rule.
     * @param rule     The rule to add.
     */
    public void add(RuleObserver observer, Rule rule) {
        executor.execute(() -> {
            // If an error occurs during the addition (insert returns row id -1) then notify the
            // triggering observer. On success notify all observers.
            long id = ruleDao.insert(rule);
            if (id == -1) {
                observer.onRuleError(Error.ADD_RULE_FAILED, rule);
            } else {
                rule.id = id;
                for (RuleObserver o : observers) {
                    o.onRuleAdded(rule);
                }
            }
        });
    }

    /**
     * Change rule in the database. On success all observers (also the one that changed the rule)
     * are notified.
     *
     * @param observer The observer that changes the rule.
     * @param rule     The rule to change.
     */
    public void change(RuleObserver observer, Rule rule) {
        executor.execute(() -> {
            // If an error occurs during the change (update returns count of affected rows) then
            // notify the triggering observer. On success notify all observers.
            int rows = ruleDao.update(rule);
            if (rows == 0) {
                observer.onRuleError(Error.UPDATE_RULE_FAILED, rule);
            } else {
                for (RuleObserver o : observers) {
                    o.onRuleChanged(rule);
                }
            }
        });
    }

    /**
     * Remove rule from the database. All observers (also the one that removed the rule) are
     * notified.
     *
     * @param observer The observer that deletes the rule.
     * @param rule     The rule to update.
     */
    public void remove(RuleObserver observer, Rule rule) {
        executor.execute(() -> {
            // If an error occurs during the removal (delete returns count of affected rows) then
            // notify the triggering observer. On success notify all observers.
            int rows = ruleDao.delete(rule);
            if (rows == 0) {
                observer.onRuleError(Error.DELETE_RULE_FAILED, rule);
            } else {
                for (RuleObserver o : observers) {
                    o.onRuleRemoved(rule);
                }
            }
        });
    }
}
