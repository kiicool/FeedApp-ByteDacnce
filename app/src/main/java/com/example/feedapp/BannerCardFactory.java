package com.example.feedapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

/**
 * 示例：通过“新增一个类 + 注册”，扩展一类新卡片样式（Banner 卡）
 */
public class BannerCardFactory implements FeedAdapter.CardFactory {

    @Override
    public int getCardType() {
        return FeedItem.CARD_TYPE_BANNER; // 新增的卡片类型
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater,
                                                      @NonNull ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_banner_card, parent, false);
        return new BannerVH(view);
    }

    /**
     * ViewHolder：只要实现 FeedAdapter.IBindableVH，就能被 FeedAdapter 复用
     */
    public static class BannerVH extends RecyclerView.ViewHolder
            implements FeedAdapter.IBindableVH {

        private final ImageView ivBanner;

        public BannerVH(@NonNull View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.ivBanner);
        }

        @Override
        public void bind(FeedItem item) {
            Object src = item.imageUrl != null ? item.imageUrl : item.imageRes;
            Glide.with(itemView.getContext()).load(src).into(ivBanner);
        }
    }
}
