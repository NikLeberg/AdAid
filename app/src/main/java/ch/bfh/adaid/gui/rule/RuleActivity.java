package ch.bfh.adaid.gui.rule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import ch.bfh.adaid.R;
import ch.bfh.adaid.action.ActionType;
import ch.bfh.adaid.db.Rule;
import ch.bfh.adaid.db.RuleDataSource;
import ch.bfh.adaid.db.RuleObserver;
import ch.bfh.adaid.gui.helper.RuleHelperActivity;

/**
 * Base activity for editing existing rules or adding new rules. Implements common form manipulation
 * and validation.
 *
 * @author Niklaus Leuenberger
 */
public abstract class RuleActivity extends AppCompatActivity implements RuleObserver {

    /**
     * Intent extra keys for sending data (viewId, viewText and packageName) to this activity.
     */
    public static final String EXTRA_VIEW_ID_KEY = "ch.bfh.adaid.gui.rule.RuleActivity.viewId";
    public static final String EXTRA_VIEW_TEXT_KEY = "ch.bfh.adaid.gui.rule.RuleActivity.viewText";
    public static final String EXTRA_PACKAGE_KEY = "ch.bfh.adaid.gui.rule.RuleActivity.packageName";

    /**
     * Get an intent to return data from the rule helper to this activity.
     *
     * @param viewId      The id of the view that was captured.
     * @param viewText    (optional) Text that was in the view.
     * @param packageName The name of the package / app for which the view was captured.
     * @return created intent, use with setResult(intent).
     */
    public static Intent getResultIntent(String viewId, String viewText, String packageName) {
        return new Intent()
                .putExtra(EXTRA_VIEW_ID_KEY, viewId)
                .putExtra(EXTRA_VIEW_TEXT_KEY, viewText)
                .putExtra(EXTRA_PACKAGE_KEY, packageName);
    }


    /*
     * Attributes
     */

    /**
     * Data source to the rule database.
     */
    protected RuleDataSource data;

    /**
     * The currently created or edited rule.
     */
    protected Rule rule;

    /**
     * Flag to indicate if entered data in form is valid. Gets set in the valid*() methods.
     */
    protected boolean formValid;

    /**
     * Launcher for the contract to get data from the rule helper activity.
     */
    private ActivityResultLauncher<Intent> getDataFromRuleHelperLauncher;


    /*
     * Lifecycle methods of AppCompatActivity
     */

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule);

        // Set correct animation for this activity.
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

        // Populate dropdowns.
        populateAppDropdown();
        populateActionTypeDropdown();

        // Set button click listeners.
        findViewById(R.id.buttonSave).setOnClickListener(v -> saveRule());
        findViewById(R.id.buttonDelete).setOnClickListener(v -> deleteRule());

        // Load rules from database.
        data = new RuleDataSource(getApplicationContext());
        data.addObserver(this);

        // Initialize contract to start RuleHelperActivity and get a result back. Replacement for
        // deprecated startActivityForResult().
        getDataFromRuleHelperLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::setFormFromRuleHelperResult);
    }

    /**
     * Called when this activity is about to exit the screen due to a hardware back button press.
     */
    @Override
    public void finish() {
        super.finish();

        // Remove as observer.
        data.removeObserver(this);

        // Slide out this activity to the right and fade in the previous/new activity.
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    /**
     * Called when the options menu is created.
     *
     * @param menu The menu to be created.
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu and add items to the action bar.
        getMenuInflater().inflate(R.menu.menu_rule, menu);
        return true;
    }

    /**
     * Called when the title bar is pressed.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // If the back button is pressed, finish this activity.
            finish(); // plays an animation
            return true;
        } else if (item.getItemId() == R.id.action_help) {
            // If the help button is pressed, start the rule helper activity and get data from it.
            Intent intent = RuleHelperActivity.getStartActivityIntent(this);
            getDataFromRuleHelperLauncher.launch(intent);
            return true;
        }
        // Unknown item, let super handle it.
        return super.onOptionsItemSelected(item);
    }


    /*
     * Private helper methods for managing the form.
     */

    /**
     * Populates the app dropdown with all installed apps.
     */
    private void populateAppDropdown() {
        AutoCompleteTextView dropdown = findViewById(R.id.dropdownApp);
        dropdown.setAdapter(new AppAdapter(this));
    }

    /**
     * Populates the action type dropdown with localized strings.
     */
    private void populateActionTypeDropdown() {
        // Source1: https://material.io/components/menus/android#dropdown-menus
        // Source2: https://rmirabelle.medium.com/there-is-no-material-design-spinner-for-android-3261b7c77da8
        AutoCompleteTextView actionType = findViewById(R.id.dropdownActionType);
        actionType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.rule_action_type_list)));
    }

    /**
     * Set the text of a TextInputEditText.
     *
     * @param id   The id of the TextInputEditText.
     * @param text The text to set.
     */
    private void setTextInput(int id, String text) {
        ((TextInputEditText) findViewById(id)).setText(text);
    }

    /**
     * Set the text of the app dropdown (AutoCompleteTextView).
     *
     * @param id    The id of the AutoCompleteTextView.
     * @param appId The text to set.
     */
    private void setAppIdDropdown(int id, String appId) {
        ((AutoCompleteTextView) findViewById(id)).setText(appId);
    }

    /**
     * Set the selected type of the action type dropdown (AutoCompleteTextView).
     *
     * @param id         The id of the AutoCompleteTextView.
     * @param actionType The action type to set.
     */
    private void setActionTypeDropdown(int id, ActionType actionType) {
        // Convert action type to localized string. Assumes that the action type enumeration is in
        // the same order as the string array resource.
        String[] actionStrings = getResources().getStringArray(R.array.rule_action_type_list);
        String actionString = actionStrings[actionType.ordinal()];
        // "Filter: false" is needed as otherwise dropdown gets reduced to one item.
        ((AutoCompleteTextView) findViewById(id)).setText(actionString, false);
    }

    /**
     * Set the numerical value of a TextInputEditText.
     *
     * @param id     The id of the TextInputEditText.
     * @param number The number to set.
     */
    private void setNumberInput(int id, int number) {
        setTextInput(id, String.valueOf(number));
    }

    /**
     * Set the error message of a TextInputLayout. If valid == true the error is reset.
     *
     * @param valid       Whether the input is valid.
     * @param idContainer The id of the TextInputLayout (container of TextInput).
     * @param idError     The id of the error message.
     */
    private void setOrResetError(boolean valid, int idContainer, int idError) {
        TextInputLayout container = findViewById(idContainer);
        // This resets the error when the text is valid by setting the error to null.
        String errorString = valid ? null : getString(idError);
        container.setError(errorString);
    }

    /**
     * Validate TextInput from form and get its text value.
     *
     * @param id          The id of the TextInput.
     * @param idContainer The id of the TextInputLayout (container of TextInput).
     * @param idError     The id of the error message, if available.
     * @param optional    Whether the input is optional, updates formValid flag accordingly.
     * @return validated text from the TextInput.
     */
    private String validateTextInput(int id, int idContainer, int idError, boolean optional) {
        TextInputEditText input = findViewById(id);
        String value = Objects.requireNonNull(input.getText()).toString();
        boolean valid = !value.isEmpty();
        if (idError != 0) {
            setOrResetError(valid, idContainer, idError);
        }
        // Only update validity if input is not optional
        formValid &= valid || optional;
        return value;
    }

    /**
     * Validate app dropdown (AutoCompleteTextView) from form and get its text value.
     *
     * @param id          The id of the AutoCompleteTextView.
     * @param idContainer The id of the TextInputLayout (container of AutoCompleteTextView).
     * @param idError     The id of the error message, if available.
     * @param optional    Whether the input is optional, updates formValid flag accordingly.
     * @return validated text from the TextInput.
     */
    private String validateAppDropdown(int id, int idContainer, int idError, boolean optional) {
        AutoCompleteTextView dropdown = findViewById(id);
        String value = Objects.requireNonNull(dropdown.getText()).toString();
        boolean valid = !value.isEmpty();
        if (idError != 0) {
            setOrResetError(valid, idContainer, idError);
        }
        // Only update validity if input is not optional
        formValid &= valid || optional;
        return value;
    }

    /**
     * Validate action type dropdown (AutoCompleteTextView) from form and get its enum value.
     *
     * @param id          The id of the AutoCompleteTextView.
     * @param idContainer The id of the TextInputLayout (container of AutoCompleteTextView).
     * @param idError     The id of the error message, if available.
     * @param optional    Whether the input is optional, updates formValid flag accordingly.
     * @return validated action type from the dropdown or null.
     */
    private ActionType validateActionTypeDropdown(int id, int idContainer, int idError, boolean optional) {
        AutoCompleteTextView dropdown = findViewById(id);
        String value = Objects.requireNonNull(dropdown.getText()).toString();
        boolean valid = !value.isEmpty();
        if (idError != 0) {
            setOrResetError(valid, idContainer, idError);
        }
        // Only update validity if input is not optional
        formValid &= valid || optional;
        return valid ? getActionTypeFromDropdownString(value) : null;
    }

    /**
     * Validate TextInput from form and get its numerical value.
     *
     * @param id          The id of the TextInput.
     * @param idContainer The id of the TextInputLayout (container of TextInput).
     * @param idError     The id of the error message, if available.
     * @param optional    Whether the input is optional, updates formValid flag accordingly.
     * @return validated numerical value from the TextInput.
     */
    private int validateNumberInput(int id, int idContainer, int idError, boolean optional) {
        TextInputEditText input = findViewById(id);
        String value = Objects.requireNonNull(input.getText()).toString();
        boolean valid = true;
        int number = 0;
        if (!value.isEmpty()) {
            try {
                number = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                valid = false;
            }
        }
        if (idError != 0) {
            setOrResetError(valid, idContainer, idError);
        }
        // Only update validity if input is not optional
        formValid &= valid || optional;
        return number;
    }

    /**
     * Convert localized string (shown in dropdown) to action type.
     *
     * @param actionString The localized string from the dropdown.
     * @return The action type enum.
     * @throws IllegalArgumentException if the string is not representing a valid action type.
     */
    private ActionType getActionTypeFromDropdownString(String actionString) throws IllegalArgumentException {
        String[] actionStrings = getResources().getStringArray(R.array.rule_action_type_list);
        for (int i = 0; i < actionStrings.length; i++) {
            if (actionStrings[i].equals(actionString)) {
                return ActionType.values()[i];
            }
        }
        throw new IllegalArgumentException("Invalid action type: " + actionString);
    }


    /*
     * Shared helper methods for managing the form.
     */

    /**
     * Initialize shown values in input texts and dropdown from rule values.
     */
    protected void initFormFromRule() {
        setTextInput(R.id.textInputName, rule.name);
        ((SwitchMaterial) findViewById(R.id.switchInputEnabled)).setChecked(rule.enabled);
        setAppIdDropdown(R.id.dropdownApp, rule.appId);
        setTextInput(R.id.textInputViewId, rule.viewId);
        setTextInput(R.id.textInputViewText, rule.viewText);
        setActionTypeDropdown(R.id.dropdownActionType, rule.actionType);
    }

    /**
     * Set rule values from shown values in input texts and dropdown. This also updates the validity
     * of the form and shows error messages of the inputs.
     */
    protected void setRuleFromView() {
        formValid = true; // set to true, validate methods will set to false if invalid.
        rule.name = validateTextInput(R.id.textInputName, R.id.textInputNameContainer,
                R.string.rule_name_error, false);
        rule.enabled = ((SwitchMaterial) findViewById(R.id.switchInputEnabled)).isChecked();
        rule.appId = validateAppDropdown(R.id.dropdownApp, R.id.dropdownAppContainer,
                R.string.rule_app_error, false);
        rule.viewId = validateTextInput(R.id.textInputViewId, R.id.textInputViewIdContainer,
                R.string.rule_view_id_error, false);
        rule.viewText = validateTextInput(R.id.textInputViewText, 0, 0, true);
        rule.actionType = validateActionTypeDropdown(R.id.dropdownActionType, R.id.dropdownActionTypeContainer,
                R.string.rule_action_type_error, false);
    }

    /**
     * Set the form values for app, id and text to the ones returned from the rule helper activity.
     *
     * @param result The result from the rule helper activity.
     */
    protected void setFormFromRuleHelperResult(ActivityResult result) {
        Intent intent = result.getData();
        if (result.getResultCode() != Activity.RESULT_OK || intent == null) {
            return;
        }
        // Get the required data from the intent.
        if (!intent.hasExtra(EXTRA_VIEW_ID_KEY) || !intent.hasExtra(EXTRA_PACKAGE_KEY)) {
            throw new IllegalArgumentException("No valid data in result intent given.");
        }
        String viewId = intent.getStringExtra(EXTRA_VIEW_ID_KEY);
        setTextInput(R.id.textInputViewId, viewId);
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_KEY);
        setAppIdDropdown(R.id.dropdownApp, packageName);
        // Get the optional data from the intent.
        if (intent.hasExtra(EXTRA_VIEW_TEXT_KEY)) {
            String viewText = intent.getStringExtra(EXTRA_VIEW_TEXT_KEY);
            setTextInput(R.id.textInputViewText, viewText);
        }
    }


    /*
     * RuleObserver interface.
     */

    /**
     * {@inheritDoc}
     * <p>
     * Best we can do on database errors is to show a toast and let the user try again.
     *
     * @param error the error that occurred
     * @param rule  rule that caused the error
     */
    @Override
    public void onRuleError(RuleDataSource.Error error, Rule rule) {
        // Build a toast message from string resource.
        String message = getString(R.string.rule_db_error, error.name());
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    /*
     * Abstract button methods.
     */

    /**
     * Abstract method for handling the "save" button press.
     */
    abstract protected void saveRule();

    /**
     * Abstract method for handling the "delete" button press.
     */
    abstract protected void deleteRule();
}