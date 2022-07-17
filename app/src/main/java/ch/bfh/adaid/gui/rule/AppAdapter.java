package ch.bfh.adaid.gui.rule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized Adapter based on ArrayAdapter for string arrays. Used for an AutoCompleteTextView
 * based spinner dropdown. Main extension is that besides the list entry text also the
 * corresponding icon of the app / package is displayed.
 * <p>
 * Sources on how to implement and customize an ArrayAdapter:
 * - https://stackoverflow.com/questions/5867312/spinner-with-text-and-icons
 * - https://developer.android.com/reference/android/widget/ArrayAdapter
 * <p>
 * Sources on how to get the package names of installed apps.
 * - https://developer.android.com/training/package-visibility/declaring
 * - https://stackoverflow.com/questions/60679685/what-does-query-all-packages-permission-do
 *
 * @author Niklaus Leuenberger, TLGINO
 */
public class AppAdapter extends ArrayAdapter<String> {

    private final PackageManager packageManager;

    /**
     * Construct a new Adapter for an AutoCompleteTextView based spinner dropdown.
     *
     * @param context Activity or application context.
     */
    @SuppressLint("QueryPermissionsNeeded")
    public AppAdapter(@NonNull Context context) {
        // Construct a default ArrayAdapter with the material layout of simple_list_item_1.
        super(context, android.R.layout.simple_list_item_1);
        // Get list of installed packages. Does not return full list if permission is missing.
        packageManager = context.getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        // Reduce ApplicationInfo to just packageName attribute and sort it into an array.
        String[] packageNames = sortPackages(packages);
        super.addAll(packageNames);
        // Loading all package icons upfront would require too much processing time on the main
        // ui tread and create animation stutters. So this is handled in getView().
    }

    /**
     * Returns a list of names corresponding to the non-system file apps
     * @param packages list of found packages
     * @return String[] of package names
     */
    public String[] sortPackages(List<ApplicationInfo> packages) {
        ArrayList<String> names = new ArrayList<>();
        for (ApplicationInfo applicationInfo : packages){
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0){
                names.add(applicationInfo.packageName);
            }
        }
        return names.stream().toArray(String[]::new);
    }


    /**
     * Get the icon of the app as drawable.
     *
     * @param position Position of the item in the list.
     * @return Icon of the app / package at the given position. Null if not found.
     */
    @Nullable
    private Drawable getIcon(int position) {
        // Try to get corresponding package icon.
        Drawable icon;
        try {
            icon = packageManager.getApplicationIcon(getItem(position));
            int size = 80; // TODO: calculate from dpi for compatibility
            icon.setBounds(0, 0, size, size);
        } catch (PackageManager.NameNotFoundException e) {
            icon = null;
        }
        return icon;
    }

    /**
     * Get an individual view of an list entry i.e. dropdown row. Uses the default android layout
     * simple_list_item_1 to display the app / package name and also a small icon of the app.
     *
     * @param position    Position of the list entry in the list.
     * @param convertView View to reuse.
     * @param parent      Parent view group.
     * @return View for the list entry.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView row = (TextView) super.getView(position, convertView, parent);
        // Also display icon of the package. This necessarily reloads the same drawables over
        // and over on every dropdown change. But AutoCompleteTextView is smart enough to only
        // load the views of visible list entries. This is around 15 icons instead of the 300 or
        // so default installed apps.
        row.setCompoundDrawables(getIcon(position), null, null, null);
        row.setCompoundDrawablePadding(32); // TODO: calculate from dpi for compatibility
        return row;
    }
}
