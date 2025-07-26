package ch.bfh.adaid.gui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import ch.bfh.adaid.R;
import ch.bfh.adaid.db.Rule;
import ch.bfh.adaid.db.RuleDataSource;
import ch.bfh.adaid.db.RuleObserver;
import ch.bfh.adaid.gui.rule.EditRuleActivity;
import ch.bfh.adaid.gui.rule.NewRuleActivity;
import ch.bfh.adaid.service.A11yService;

/**
 * The main activity that is showing a list of rules.
 *
 * @author Niklaus Leuenberger
 */
public class MainActivity extends AppCompatActivity implements RuleObserver, RuleRecyclerViewAdapter.ItemClickListener {

    RuleRecyclerViewAdapter adapter;
    private RuleDataSource data;
    final ArrayList<Rule> rules = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the RecyclerView with an Adapter that has a list of rules. The list of rules is
        // managed by the RuleObserver callbacks implemented further down.
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RuleRecyclerViewAdapter(this, rules);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        // Start rules database and add this activity as observer.
        data = new RuleDataSource(getApplicationContext());
        data.addObserver(this);

        // On a click to the FAB open the rule activity to create a new rule.
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = NewRuleActivity.getStartActivityIntent(this);
            startActivity(intent);
        });
    }

    @Override
    public void onItemClick(int position) {
        // When an item in the recycler view is clicked, open the rule activity to edit the rule.
        Intent intent = EditRuleActivity.getStartActivityIntent(this, adapter.getItem(position).id);
        startActivity(intent);
    }

    @Override
    public void onItemSwitchClick(int position) {
        // When a switch of an item in the recycler view is clicked, toggle the rule enabled state.
        Rule rule = adapter.getItem(position);
        Log.d("MainActivity", "Toggling rule " + rule.id + " to " + !rule.enabled);
        rule.enabled = !rule.enabled;
        data.change(this, rule);
    }

    @Override
    public void onStart() {
        super.onStart();

        // If a11y service is disabled show a snackbar with a button to take the user to
        // the setting that enables it.
        if (!A11yService.isServiceEnabled(getApplicationContext())) {
            Intent intent = A11yService.getOpenSettingIntent(getApplicationContext());
            View container = findViewById(R.id.layoutContainer);
            Snackbar.make(container, R.string.a11y_service_not_enabled_message, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.a11y_service_not_enabled_action,
                            v -> startActivity(intent))
                    .show();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyRulesChanged() {
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            // Show / hide the special "empty view".
            View emptyView = findViewById(R.id.empty_view);
            emptyView.setVisibility(rules.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onRuleLoad(List<Rule> rules) {
        this.rules.addAll(rules);
        notifyRulesChanged();
    }

    @Override
    public void onRuleAdded(Rule rule) {
        rules.add(rule);
        notifyRulesChanged();
    }

    @Override
    public void onRuleChanged(Rule rule) {
        rules.replaceAll(r -> r.id == rule.id ? rule : r);
        notifyRulesChanged();
    }

    @Override
    public void onRuleRemoved(Rule rule) {
        rules.removeIf(r -> r.id == rule.id);
        notifyRulesChanged();
    }

    @Override
    public void onRuleError(RuleDataSource.Error error, Rule rule) {
        System.out.println("onRuleError: " + error);
        System.out.format("Name: %s, Id: %d\n", rule.name, rule.id);
    }
}
