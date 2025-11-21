package com.example.feedapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 普通卡片 viewType 就用 FeedItem.cardType（1 文本 / 2 图片）
    private static final int VIEW_TYPE_FOOTER = 100;

    // footer 状态
    public static final int FOOTER_STATE_HIDDEN  = -1; // 不显示 footer
    public static final int FOOTER_STATE_LOADING = 0;  // 加载中
    public static final int FOOTER_STATE_ERROR   = 1;  // 加载失败，可重试
    public static final int FOOTER_STATE_NO_MORE = 2;  // 没有更多

    private final Context context;
    private final List<FeedItem> items = new ArrayList<>();

    private int footerState = FOOTER_STATE_HIDDEN;

    public interface OnFooterRetryListener {
        void onRetry();
    }

    private OnFooterRetryListener footerRetryListener;

    public void setFooterRetryListener(OnFooterRetryListener listener) {
        this.footerRetryListener = listener;
    }

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
        // 最后一项且 footer 需要显示 → footer
        if (footerState != FOOTER_STATE_HIDDEN && position == items.size()) {
            return VIEW_TYPE_FOOTER;
        }
        // 其他都是普通卡片
        return items.get(position).cardType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == FeedItem.CARD_TYPE_TEXT) {
            View view = inflater.inflate(R.layout.item_text_card, parent, false);
            return new TextVH(view);
        } else if (viewType == FeedItem.CARD_TYPE_IMAGE) {
            View view = inflater.inflate(R.layout.item_image_card, parent, false);
            return new ImageVH(view);
        } else { // footer
            View view = inflater.inflate(R.layout.item_load_more_footer, parent, false);
            return new FooterVH(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        if (holder instanceof FooterVH) {
            bindFooter((FooterVH) holder);
            return;
        }

        FeedItem item = items.get(position);

        if (holder instanceof TextVH) {
            TextVH h = (TextVH) holder;
            h.tvTitle.setText(item.title);
            h.tvDesc.setText(item.description);

        } else if (holder instanceof ImageVH) {
            ImageVH h = (ImageVH) holder;
            h.tvTitle.setText(item.title);

            if (item.imageRes != 0) { // 本地图
                h.ivImage.setImageResource(item.imageRes);
            } else if (item.imageUrl != null) { // 网络图
                Glide.with(context)
                        .load(item.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(h.ivImage);
            } else { // 都没有，给个占位
                h.ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

        // 所有普通卡片都支持长按删除
        bindLongClickDelete(holder);
    }

    private void bindFooter(FooterVH h) {
        h.itemView.setOnClickListener(null);
        h.progressBar.setVisibility(View.GONE);
        h.tvStatus.setText("");

        switch (footerState) {
            case FOOTER_STATE_LOADING:
                h.progressBar.setVisibility(View.VISIBLE);
                h.tvStatus.setText("正在加载...");
                break;

            case FOOTER_STATE_ERROR:
                h.progressBar.setVisibility(View.GONE);
                h.tvStatus.setText("加载失败，点击重试");
                h.itemView.setOnClickListener(v -> {
                    if (footerRetryListener != null) {
                        footerRetryListener.onRetry();
                    }
                });
                break;

            case FOOTER_STATE_NO_MORE:
                h.progressBar.setVisibility(View.GONE);
                h.tvStatus.setText("已经到底了");
                break;
        }
    }

    private void bindLongClickDelete(@NonNull RecyclerView.ViewHolder holder) {
        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || pos >= items.size()) {
                return true;
            }

            new AlertDialog.Builder(context)
                    .setTitle("删除卡片")
                    .setMessage("确定要删除这张卡片吗？")
                    .setPositiveButton("删除", (dialog, which) -> removeItem(pos))
                    .setNegativeButton("取消", null)
                    .show();

            return true;
        });
    }

    public void removeItem(int position) {
        if (position < 0 || position >= items.size()) return;
        items.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        int count = items.size();
        if (footerState != FOOTER_STATE_HIDDEN) {
            count += 1;
        }
        return count;
    }

    // 对外提供设置 footer 状态的方法
    public void setFooterState(int newState) {
        if (footerState == newState) return;
        footerState = newState;
        notifyDataSetChanged(); // 简单粗暴一点，先全刷新
    }

    // 文本卡片 ViewHolder
    static class TextVH extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDesc;

        TextVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
        }
    }

    // 图片卡片 ViewHolder
    static class ImageVH extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;

        ImageVH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }

    // footer ViewHolder
    static class FooterVH extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        TextView tvStatus;

        FooterVH(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
