package com.example.feedapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import android.util.SparseArray;
public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_FOOTER = 100;

    // footer 状态
    public static final int FOOTER_STATE_HIDDEN = -1;
    public static final int FOOTER_STATE_LOADING = 0;
    public static final int FOOTER_STATE_ERROR = 1;
    public static final int FOOTER_STATE_NO_MORE = 2;

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

    // 为 VideoVH 添加点击事件回调接口
    public interface OnVideoClickListener {
        void onVideoClick(VideoVH vh, FeedItem item);
    }
    private OnVideoClickListener videoClickListener;

    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.videoClickListener = listener;
    }
    public interface CardFactory {
        int getCardType();

        @NonNull
        RecyclerView.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater,
                                                   @NonNull ViewGroup parent);
    }

    private final SparseArray<CardFactory> cardFactories = new SparseArray<>();

    public void registerCardFactory(@NonNull CardFactory factory) {
        cardFactories.put(factory.getCardType(), factory);
    }

    public FeedAdapter(Context context) {
        this.context = context;
        registerCardFactory(new CardFactory() {
            @Override
            public int getCardType() {
                return FeedItem.CARD_TYPE_TEXT;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater,
                                                              @NonNull ViewGroup parent) {
                // 复用原来的创建方法
                return createTextVH(parent);
            }
        });

        registerCardFactory(new CardFactory() {
            @Override
            public int getCardType() {
                return FeedItem.CARD_TYPE_IMAGE;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater,
                                                              @NonNull ViewGroup parent) {
                return createImageVH(parent);
            }
        });

        registerCardFactory(new CardFactory() {
            @Override
            public int getCardType() {
                return FeedItem.CARD_TYPE_VIDEO;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater,
                                                              @NonNull ViewGroup parent) {
                return createVideoVH(parent);
            }
        });
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
        if (footerState != FOOTER_STATE_HIDDEN && position == items.size()) {
            return VIEW_TYPE_FOOTER;
        }
        if (position < 0 || position >= items.size()) {
            return super.getItemViewType(position);
        }
        return items.get(position).cardType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            return createFooterVH(parent);
        }

        CardFactory factory = cardFactories.get(viewType);
        if (factory != null) {
            return factory.onCreateViewHolder(LayoutInflater.from(context), parent);
        }

        throw new IllegalArgumentException("Unknown viewType = " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof IBindableVH) {
            if (position >= 0 && position < items.size()) {
                ((IBindableVH) holder).bind(items.get(position));
            }
        } else if (holder instanceof FooterVH) {
            ((FooterVH) holder).bind(footerState, footerRetryListener);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            boolean isFullSpan = false;
            int viewType = getItemViewType(position);
            if (viewType == VIEW_TYPE_FOOTER) {
                isFullSpan = true;
            } else {
                if (!items.isEmpty() && position < items.size()) {
                    FeedItem item = items.get(position);
                    if (item.layoutType == FeedItem.LAYOUT_SINGLE_COLUMN) {
                        isFullSpan = true;
                    }
                }
            }
            p.setFullSpan(isFullSpan);
        }
    }

    private TextVH createTextVH(ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_text_card, parent, false);
        TextVH holder = new TextVH(view);
        bindLongClickDelete(holder);
        return holder;
    }

    private ImageVH createImageVH(ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_card, parent, false);
        ImageVH holder = new ImageVH(view);
        bindLongClickDelete(holder);
        return holder;
    }

    private VideoVH createVideoVH(ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_card, parent, false);
        VideoVH holder = new VideoVH(view);
        bindLongClickDelete(holder);

        holder.playerContainer.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && videoClickListener != null) {
                videoClickListener.onVideoClick(holder, items.get(pos));
            }
        });

        return holder;
    }

    private FooterVH createFooterVH(ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_load_more_footer, parent, false);
        return new FooterVH(view);
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

    public void setFooterState(int newState) {
        if (footerState == newState) return;
        int oldState = this.footerState;
        this.footerState = newState;
        boolean wasVisible = (oldState != FOOTER_STATE_HIDDEN);
        boolean isVisible = (newState != FOOTER_STATE_HIDDEN);
        if (wasVisible && !isVisible) {
            notifyItemRemoved(items.size());
        } else if (!wasVisible && isVisible) {
            notifyItemInserted(items.size());
        } else if (wasVisible && isVisible) {
            notifyItemChanged(items.size());
        }
    }

    interface IBindableVH {
        void bind(FeedItem item);
    }

    public static class TextVH extends RecyclerView.ViewHolder implements IBindableVH {
        TextView tvTitle;
        TextView tvDesc;

        public TextVH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
        }

        @Override
        public void bind(FeedItem item) {
            tvTitle.setText(item.title);
            tvDesc.setText(item.description);
        }
    }

    public static class ImageVH extends RecyclerView.ViewHolder implements IBindableVH {
        ImageView ivImage;
        TextView tvTitle;
        TextView tvDesc;
        ImageView ivPlayButton;

        public ImageVH(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            ivPlayButton = itemView.findViewById(R.id.ivPlayButton);
        }

        @Override
        public void bind(FeedItem item) {
            tvTitle.setText(item.title);
            tvDesc.setText(item.description);

            if (ivPlayButton != null) {
                ivPlayButton.setVisibility(View.GONE);
            }

            Glide.with(itemView.getContext()).clear(ivImage);
            if (item.imageRes != 0) {
                Glide.with(itemView.getContext()).load(item.imageRes).into(ivImage);
            } else if (item.imageUrl != null) {
                Glide.with(itemView.getContext()).load(item.imageUrl).into(ivImage);
            } else {
                ivImage.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }
    }


    public static class VideoVH extends RecyclerView.ViewHolder implements IBindableVH {
        FrameLayout playerContainer;
        ImageView ivCover;
        ImageView ivPlayButton;
        TextView tvTitle;
        TextView tvDesc;

        public VideoVH(@NonNull View itemView) {
            super(itemView);
            playerContainer = itemView.findViewById(R.id.playerContainer);
            ivCover = itemView.findViewById(R.id.ivCover);
            ivPlayButton = itemView.findViewById(R.id.ivPlayButton);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDesc = itemView.findViewById(R.id.tvDesc);
        }

        @Override
        public void bind(FeedItem item) {
            tvTitle.setText(item.title);
            tvDesc.setText(item.description);

            Object coverSource = item.imageUrl != null ? item.imageUrl : item.imageRes;
            Glide.with(itemView.getContext()).load(coverSource).into(ivCover);

            // 每次绑定时，都恢复初始状态，显示封面和播放按钮
            ivCover.setVisibility(View.VISIBLE);
            ivPlayButton.setVisibility(View.VISIBLE);
        }
    }

    public static class FooterVH extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        TextView tvStatus;

        public FooterVH(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        public void bind(int footerState, OnFooterRetryListener retryListener) {
            itemView.setOnClickListener(null);
            switch (footerState) {
                case FOOTER_STATE_LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    tvStatus.setText("正在加载...");
                    break;
                case FOOTER_STATE_ERROR:
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("加载失败，点击重试");
                    itemView.setOnClickListener(v -> {
                        if (retryListener != null) {
                            retryListener.onRetry();
                        }
                    });
                    break;
                case FOOTER_STATE_NO_MORE:
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("已经到底了");
                    break;
                default:
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("");
                    break;
            }
        }
    }
    /** 给本地缓存用，返回当前列表的一份浅拷贝 */
    public List<FeedItem> getItemsSnapshot() {
        return new ArrayList<>(items);
    }

}
