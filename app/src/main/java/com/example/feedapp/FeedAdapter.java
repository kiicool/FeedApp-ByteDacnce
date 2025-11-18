package com.example.feedapp; // 注意改成你自己的包名
import com.bumptech.glide.Glide;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<FeedItem> items = new ArrayList<>();

    public FeedAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<FeedItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addItems(List<FeedItem> more) {
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public FeedItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).cardType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == FeedItem.CARD_TYPE_TEXT) {
            View view = inflater.inflate(R.layout.item_text_card, parent, false);
            return new TextVH(view);
        } else {
            View view = inflater.inflate(R.layout.item_image_card, parent, false);
            return new ImageVH(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {

        FeedItem item = items.get(position);

        if (holder instanceof TextVH) {
            TextVH h = (TextVH) holder;
            h.tvTitle.setText(item.title);
            h.tvDesc.setText(item.description);
        } else if (holder instanceof ImageVH) {
            ImageVH h = (ImageVH) holder;
            h.tvTitle.setText(item.title);

            if (item.imageRes != 0) {
                // 本地图片
                h.ivImage.setImageResource(item.imageRes);
            } else {
                // 网络图片
                Glide.with(context)
                        .load(item.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(h.ivImage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TextVH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDesc;

        TextVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
        }
    }

    static class ImageVH extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;

        ImageVH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}
