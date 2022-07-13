
package ch.bfh.adaid.gui.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ch.bfh.adaid.R;

/**
 * NodeRecyclerViewAdapter is the adapter for the RecyclerView in the RuleHelperActivity. It gets
 * the tree structure of all ViewNodes i.e. the snapshot and is responsible for the tree like layout
 * of the individual nodes.
 *
 * @author Niklaus Leuenberger
 */
public class ViewTreeRecyclerViewAdapter extends RecyclerView.Adapter<ViewTreeRecyclerViewAdapter.ViewTreeRecyclerViewHolder> {

    private final FlattenedViewTree mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    public ViewTreeRecyclerViewAdapter(Context context, FlattenedViewTree viewTree) {
        mInflater = LayoutInflater.from(context);
        mData = viewTree;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewTreeRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.tree_view_row, parent, false);
        return new ViewTreeRecyclerViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull ViewTreeRecyclerViewHolder holder, int position) {
        FlattenedViewTree.SimpleView view = getItem(position);
        if (view.id.isEmpty()) {
            holder.textViewId.setText(R.string.rule_helper_value_not_set);
            holder.textViewId.setAlpha(0.5f);
        } else {
            holder.textViewId.setText(view.id);
            holder.textViewId.setAlpha(1.0f);
        }
        holder.textViewText.setText(view.text);
        ViewGroup.MarginLayoutParams params;
        params = (ViewGroup.MarginLayoutParams) holder.textViewId.getLayoutParams();
        params.leftMargin = 20 * view.level;
        params = (ViewGroup.MarginLayoutParams) holder.textViewText.getLayoutParams();
        params.leftMargin = 20 * view.level;
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.views.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewTreeRecyclerViewHolder extends RecyclerView.ViewHolder {
        TextView textViewId;
        TextView textViewText;

        ViewTreeRecyclerViewHolder(View itemView) {
            super(itemView);
            textViewId = itemView.findViewById(R.id.viewId);
            itemView.setOnClickListener(this::onItemClick);
            textViewText = itemView.findViewById(R.id.viewText);
        }

        private void onItemClick(View view) {
            if (mClickListener == null) return;
            mClickListener.onItemClick(getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    FlattenedViewTree.SimpleView getItem(int position) {
        return mData.views.get(position);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);
    }
}
