package com.example.wipear;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.VH> {

    public interface OnRemove {
        void onRemove(PhotoItem item);
    }

    private final List<PhotoItem> items;
    private final OnRemove onRemove;

    private final ExecutorService exec = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> cache;

    public TrashAdapter(List<PhotoItem> items, OnRemove onRemove) {
        this.items = items;
        this.onRemove = onRemove;
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
        cache = new LruCache<String, Bitmap>(maxKb / 8) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
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
        holder.boundKey = item.key();
        holder.name.setText(item.isVideo ? "🎬 " + item.name : item.name);

        if (item.isPdf) {
            holder.thumb.setBackgroundColor(0xFFFFFFFF);
            Bitmap cached = cache.get(item.key());
            if (cached != null) {
                holder.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.thumb.setImageBitmap(cached);
            } else {
                holder.thumb.setScaleType(ImageView.ScaleType.CENTER);
                holder.thumb.setImageResource(R.drawable.ic_pdf);
                renderPdfThumb(holder, item.key(), item.uri);
            }
        } else {
            holder.thumb.setBackground(null);
            holder.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(holder.thumb.getContext())
                    .load(item.uri)
                    .centerCrop()
                    .into(holder.thumb);
        }
        holder.remove.setOnClickListener(v -> onRemove.onRemove(item));
    }

    private void renderPdfThumb(VH holder, String key, Uri uri) {
        final Context ctx = holder.thumb.getContext().getApplicationContext();
        exec.execute(() -> {
            final Bitmap bmp = PdfThumb.renderFirstPage(ctx, uri, 360);
            if (bmp == null) return;
            main.post(() -> {
                cache.put(key, bmp);
                if (key.equals(holder.boundKey)) {
                    holder.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.thumb.setImageBitmap(bmp);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Stop background rendering when the screen goes away. */
    public void shutdown() {
        exec.shutdownNow();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView name;
        final ImageButton remove;
        String boundKey;

        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            name = itemView.findViewById(R.id.name);
            remove = itemView.findViewById(R.id.removeButton);
        }
    }
}
