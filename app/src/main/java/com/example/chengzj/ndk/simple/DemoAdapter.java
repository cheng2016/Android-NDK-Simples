package com.example.chengzj.ndk.simple;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chengzj.ndk.simple.model.DemoItem;

import java.util.List;

public class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.Holder> {

    public interface OnDemoClickListener {
        void onDemoClick(DemoItem item);
    }

    private final List<DemoItem> items;
    private final OnDemoClickListener listener;

    public DemoAdapter(List<DemoItem> items, OnDemoClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_demo, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DemoItem item = items.get(position);
        holder.category.setText(item.category);
        holder.title.setText(item.title);
        holder.summary.setText(item.summary);
        holder.topic.setText(item.topic);
        holder.itemView.setOnClickListener(v -> listener.onDemoClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView category;
        final TextView title;
        final TextView summary;
        final TextView topic;

        Holder(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.demo_category);
            title = itemView.findViewById(R.id.demo_title);
            summary = itemView.findViewById(R.id.demo_summary);
            topic = itemView.findViewById(R.id.demo_topic);
        }
    }
}
