package ch.bfh.adaid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import ch.bfh.adaid.db.Rule;
import ch.bfh.adaid.db.RuleDao;
import ch.bfh.adaid.db.RuleDatabase;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * This checks if the access to the room database over the DAO (Data Access Object) works.
 */
@RunWith(AndroidJUnit4.class)
public class RuleDaoTest {
    private RuleDao ruleDao;
    private RuleDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, RuleDatabase.class).build();
        ruleDao = db.ruleDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    public Rule helper_insertRule(String name) {
        Rule rule = new Rule(name);
        rule.id = ruleDao.insert(rule);
        return rule;
    }

    @Test
    public void can_insert_rule() {
        Rule rule = helper_insertRule("Test");
        assertEquals(1, rule.id);
    }

    @Test
    public void can_get_rule_by_id() {
        Rule rule = helper_insertRule("Test");
        Rule foundRule = ruleDao.getById(rule.id);
        assertNotNull(foundRule);
        assertEquals("Test", foundRule.name);
    }

    @Test
    public void can_find_rule_by_name() {
        helper_insertRule("Test");
        Rule foundRule = ruleDao.findByName("Test");
        assertNotNull(foundRule);
        assertEquals("Test", foundRule.name);
    }

    @Test
    public void can_update_rule() {
        Rule rule = helper_insertRule("Test");
        rule.name = "Updated";
        ruleDao.update(rule);
        Rule oldRule = ruleDao.findByName("Test");
        assertNull(oldRule);
        Rule updatedRule = ruleDao.findByName("Updated");
        assertNotNull(updatedRule);
        assertEquals("Updated", updatedRule.name);
    }

    @Test
    public void can_delete_rule() {
        Rule rule1 = helper_insertRule("Test1");
        helper_insertRule("Test2");
        ruleDao.delete(rule1);
        Rule deletedRule = ruleDao.findByName("Test1");
        assertNull(deletedRule);
        Rule unaffectedRule = ruleDao.findByName("Test2");
        assertNotNull(unaffectedRule);
    }

    @Test
    public void can_get_all_rules() {
        helper_insertRule("Test1");
        helper_insertRule("Test2");
        List<Rule> rules = ruleDao.getAll();
        assertEquals(2, rules.size());
        assertEquals("Test1", rules.get(0).name);
        assertEquals("Test2", rules.get(1).name);
    }

    @Test
    public void can_delete_all_rules() {
        helper_insertRule("Test1");
        helper_insertRule("Test2");
        ruleDao.deleteAll();
        List<Rule> rules = ruleDao.getAll();
        assertEquals(0, rules.size());
    }
}