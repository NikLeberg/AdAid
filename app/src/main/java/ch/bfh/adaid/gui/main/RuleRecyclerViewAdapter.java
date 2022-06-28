
package ch.bfh.adaid.gui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

import ch.bfh.adaid.R;
import ch.bfh.adaid.db.Rule;

/**
 * RuleRecyclerViewAdapter is the adapter for the RecyclerView in the MainActivity. It has a list of
 * rules and is responsible for the layout of the RecyclerView.
 *
 * Source: https://stackoverflow.com/a/40584425/16034014
 *
 * @author Adrian Reusser
 */
public class RuleRecyclerViewAdapter extends RecyclerView.Adapter<RuleRecyclerViewAdapter.ViewHolder> {

    private final ArrayList<Rule> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    RuleRecyclerViewAdapter(Context context, ArrayList<Rule> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String name = mData.get(position).name;
        holder.myTextView.setText(name);
        holder.mySwitch.setChecked(mData.get(position).enabled);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView myTextView;
        SwitchMaterial mySwitch;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.ruleName);
            itemView.setOnClickListener(this::onItemClick);
            mySwitch = itemView.findViewById(R.id.ruleSwitch);
            mySwitch.setOnClickListener(this::onItemSwitchClick);
        }

        private void onItemClick(View view) {
            if (mClickListener == null) return;
            mClickListener.onItemClick(getAdapterPosition());
        }

        private void onItemSwitchClick(View view) {
            if (mClickListener == null) return;
            // The listener that is triggered here will probably change the state of the switch. But
            // if a switch is programmatically changed its animation is not triggered and just jumps
            // to the new state. One way to fix would be to delay the callback until after the
            // animation has finished. But this is ugly when the switch is called in rapid
            // succession. Source: https://stackoverflow.com/q/69142820/16034014
            // TODO: Fix it so that animation is correctly played.
            // long delay = 2 * mySwitch.animate().getDuration();
            // final Handler handler = new Handler(Looper.getMainLooper());
            // handler.postDelayed(() -> mClickListener.onItemSwitchClick(getAdapterPosition()), delay);
            mClickListener.onItemSwitchClick(getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    Rule getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);

        void onItemSwitchClick(int position);
    }
}
