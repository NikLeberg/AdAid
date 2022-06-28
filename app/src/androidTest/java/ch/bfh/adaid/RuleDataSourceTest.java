package ch.bfh.adaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ch.bfh.adaid.db.Rule;
import ch.bfh.adaid.db.RuleDataSource;
import ch.bfh.adaid.db.RuleDatabase;
import ch.bfh.adaid.db.RuleObserver;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * This checks if the data source to the database works as expected e.g. notifies observers on
 * changes.
 *
 * As real database access is made in a background executor within {@link RuleDataSource} exceptions
 * / asserts that are thrown in the observable are not caught by the test runner and as such not
 * reported. They are listed as mysteriously failing with no clear reason. To see the reason open
 * Logcat, there is the exception shown.
 */
@RunWith(AndroidJUnit4.class)
public class RuleDataSourceTest {
    private static RuleDatabase db;

    @BeforeClass
    public static void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, RuleDatabase.class).build();
    }

    @AfterClass
    public static void closeDb() {
        db.close();
    }

    private RuleDataSource ruleDataSource;

    @Before
    public void createDataSource() {
        db.ruleDao().deleteAll();
        ruleDataSource = new RuleDataSource(db.ruleDao());
    }

    /**
     * Basic observer interface implementation that implements the abstract methods to throw
     * failures if they are called. Used as parent of anonymous observer classes in test cases to
     * check if the right callback methods are called.
     */
    private static class BasicTestObserver implements RuleObserver {
        @Override
        public void onRuleLoad(List<Rule> rules) {
            fail("call of onRuleLoad not expected");
        }
        @Override
        public void onRuleAdded(Rule rule) {
            fail("call of onRuleAdded not expected");
        }
        @Override
        public void onRuleChanged(Rule rule) {
            fail("call of onRuleChanged not expected");
        }
        @Override
        public void onRuleRemoved(Rule rule) {
            fail("call of onRuleRemoved not expected");
        }
        @Override
        public void onRuleError(RuleDataSource.Error error, Rule rule) {
            fail("call of onRuleError not expected");
        }
    }

    @Test
    public void can_register_and_get_load_event() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        RuleObserver observer = new BasicTestObserver(){
            @Override
            public void onRuleLoad(List<Rule> rules) {
                // On load, no rules should be available.
                assertEquals(0, rules.size());
                // Signal the finished execution with the latch.
                latch.countDown();
            }
        };
        ruleDataSource.addObserver(observer);
        // Wait at max 1 s for background executor to finish.
        if (!latch.await(1, TimeUnit.SECONDS)) {
            fail("Timed out waiting for initial rule load.");
        }
        ruleDataSource.removeObserver(observer);
    }

    @Test
    public void can_add_rule_and_get_added_event() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        RuleObserver observer = new BasicTestObserver(){
            @Override
            public void onRuleLoad(List<Rule> rules) {
                // After load, add a rule. This should trigger the added event.
                ruleDataSource.add(this, new Rule("TestAdd"));
            }
            @Override
            public void onRuleAdded(Rule rule) {
                // On add, the observer should be notified with the same rule that was added.
                assertEquals("TestAdd", rule.name);
                // Signal the finished execution with the latch.
                latch.countDown();
            }
        };
        ruleDataSource.addObserver(observer);
        // Wait at max 1 s for background executor to finish.
        if (!latch.await(1, TimeUnit.SECONDS)) {
            fail("Timed out waiting for rule addition.");
        }
        ruleDataSource.removeObserver(observer);
    }

    @Test
    public void can_change_rule_and_get_changed_event() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        RuleObserver observer = new BasicTestObserver(){
            @Override
            public void onRuleLoad(List<Rule> rules) {
                // After load, change a rule. This should trigger the added event.
                ruleDataSource.add(this, new Rule("Test"));
            }
            @Override
            public void onRuleAdded(Rule rule) {
                // After add, change the rule. This should trigger the changed event.
                rule.name = "TestChange";
                ruleDataSource.change(this, rule);
            }
            @Override
            public void onRuleChanged(Rule rule) {
                // On change, the observer should be notified with the same rule that was changed.
                assertEquals("TestChange", rule.name);
                // Signal the finished execution with the latch.
                latch.countDown();
            }
        };
        ruleDataSource.addObserver(observer);
        // Wait at max 1 s for background executor to finish.
        if (!latch.await(1, TimeUnit.SECONDS)) {
            fail("Timed out waiting for rule change.");
        }
        ruleDataSource.removeObserver(observer);
    }

    @Test
    public void can_remove_rule_and_get_removed_event() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        RuleObserver observer = new BasicTestObserver(){
            @Override
            public void onRuleLoad(List<Rule> rules) {
                // After load, add a rule. This should trigger the added event.
                ruleDataSource.add(this, new Rule("TestRemove"));
            }
            @Override
            public void onRuleAdded(Rule rule) {
                // After add, remove the rule. This should trigger the removed event.
                ruleDataSource.remove(this, rule);
            }
            @Override
            public void onRuleRemoved(Rule rule) {
                // On remove, the observer should be notified with the same rule that was removed.
                assertEquals("TestRemove", rule.name);
                // Signal the finished execution with the latch.
                latch.countDown();
            }
        };
        ruleDataSource.addObserver(observer);
        // Wait at max 1 s for background executor to finish.
        if (!latch.await(1, TimeUnit.SECONDS)) {
            fail("Timed out waiting for rule removal.");
        }
        ruleDataSource.removeObserver(observer);
    }

    @Test
    public void can_add_rule_and_multiple_observers_get_added_event() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        RuleObserver observer1 = new BasicTestObserver(){
            @Override
            public void onRuleLoad(List<Rule> rules) {
                // The first observer adds a new rule.
                // After load on the first observer, add a rule. This should trigger the added event
                // for both observers.
                ruleDataSource.add(this, new Rule("TestMultipleObservers"));
            }
            @Override
            public void onRuleAdded(Rule rule) {
                // On add, the observer should be notified with the same rule that was added.
                assertEquals("TestMultipleObservers", rule.name);
                // Signal the finished execution of this observer with the latch.
                latch.countDown();
            }
        };
        RuleObserver observer2 = new BasicTestObserver(){
            @Override
            public void onRuleLoad(List<Rule> rules) {}
            @Override
            public void onRuleAdded(Rule rule) {
                // On add, the observer should be notified with the same rule that was added.
                assertEquals("TestMultipleObservers", rule.name);
                // Signal the finished execution of this observer with the latch.
                latch.countDown();
            }
        };
        ruleDataSource.addObserver(observer1);
        ruleDataSource.addObserver(observer2);
        // Wait at max 1 s for background executors to finish.
        if (!latch.await(1, TimeUnit.SECONDS)) {
            fail("Timed out waiting for both observers.");
        }
        ruleDataSource.removeObserver(observer1);
        ruleDataSource.removeObserver(observer2);
    }
}