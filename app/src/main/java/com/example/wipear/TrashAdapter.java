package com.example.wipear;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.VH> {

    public interface OnRemove {
        void onRemove(PhotoItem item);
    }

    private final List<PhotoItem> items;
    private final OnRemove onRemove;

    public TrashAdapter(List<PhotoItem> items, OnRemove onRemove) {
        this.items = items;
        this.onRemove = onRemove;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trash, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PhotoItem item = items.get(position);
        holder.name.setText(item.name);
        Glide.with(holder.thumb.getContext())
                .load(item.uri)
                .centerCrop()
                .into(holder.thumb);
        holder.remove.setOnClickListener(v -> onRemove.onRemove(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView name;
        final ImageButton remove;

        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            name = itemView.findViewById(R.id.name);
            remove = itemView.findViewById(R.id.removeButton);
        }
    }
}
