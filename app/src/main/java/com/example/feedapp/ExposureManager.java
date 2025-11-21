package com.example.feedapp;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExposureManager {

    public interface ExposureListener {
        // 第一次进入可视区域（可见比例 > 0）
        void onItemExposed(@NonNull FeedItem item, int position, float visibleRatio);

        // 第一次达到 100% 可见
        void onItemFullyVisible(@NonNull FeedItem item, int position);

        // 从可视区域彻底消失
        void onItemHidden(@NonNull FeedItem item, int position, float lastVisibleRatio, long totalVisibleMillis);
    }

    private static class ItemState {
        float lastRatio = 0f;
        int lastPosition = RecyclerView.NO_POSITION;
        long visibleStartTime = 0L;
        long totalVisibleTime = 0L;
        boolean hasExposed = false;
        boolean hasFullVisible = false;
    }

    private final RecyclerView recyclerView;
    private final GridLayoutManager layoutManager;
    private final FeedAdapter adapter;
    private final ExposureListener listener;

    private final Map<String, ItemState> stateMap = new HashMap<>();

    public ExposureManager(RecyclerView recyclerView,
                           GridLayoutManager layoutManager,
                           FeedAdapter adapter,
                           ExposureListener listener) {
        this.recyclerView = recyclerView;
        this.layoutManager = layoutManager;
        this.adapter = adapter;
        this.listener = listener;

        attach();
    }

    private void attach() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                checkExposure();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                super.onScrollStateChanged(rv, newState);
                // 停止滚动时再算一遍，保证停留状态被记录
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    checkExposure();
                }
            }
        });

        // 初次布局完成也算一遍
        recyclerView.post(this::checkExposure);
    }

    private void checkExposure() {
        if (adapter.getItemCount() == 0) return;

        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        Set<String> currentlyVisible = new HashSet<>();

        Rect rect = new Rect();

        for (int pos = first; pos <= last; pos++) {
            if (pos < 0 || pos >= adapter.getItemCount()) continue;

            // 跳过 footer
            if (adapter.getItemViewType(pos) == FeedAdapter.VIEW_TYPE_FOOTER) {
                continue;
            }

            View child = layoutManager.findViewByPosition(pos);
            if (child == null) continue;

            FeedItem item = adapter.getItem(pos);
            if (item == null || item.id == null) continue;

            boolean visible = child.getGlobalVisibleRect(rect);
            if (!visible) continue;

            int height = child.getHeight();
            if (height <= 0) continue;

            int visibleHeight = rect.bottom - rect.top;
            float ratio = Math.max(0f, Math.min(1f, visibleHeight * 1f / height));

            ItemState state = stateMap.get(item.id);
            if (state == null) {
                state = new ItemState();
                stateMap.put(item.id, state);
            }

            // 从不可见 -> 可见
            if (state.lastRatio == 0f && ratio > 0f) {
                state.visibleStartTime = now;
                if (!state.hasExposed) {
                    state.hasExposed = true;
                    if (listener != null) {
                        listener.onItemExposed(item, pos, ratio);
                    }
                }
            }

            // 可见期间，每次更新
            if (ratio > 0f && state.visibleStartTime > 0L) {
                long delta = now - Math.max(state.visibleStartTime, 0L);
                // 为了简单，这里不叠加 delta，每次退出时算一次总时长
            }

            // 第一次达到完全可见
            if (!state.hasFullVisible && ratio >= 1.0f) {
                state.hasFullVisible = true;
                if (listener != null) {
                    listener.onItemFullyVisible(item, pos);
                }
            }

            state.lastRatio = ratio;
            state.lastPosition = pos;
            currentlyVisible.add(item.id);
        }

        // 处理那些上次可见，这次不可见的：视为“曝光结束”
        for (Map.Entry<String, ItemState> entry : stateMap.entrySet()) {
            String id = entry.getKey();
            ItemState state = entry.getValue();

            if (state.lastRatio > 0f && !currentlyVisible.contains(id)) {
                // 刚刚从可见 -> 不可见
                long endTime = now;
                long visibleTime = 0L;
                if (state.visibleStartTime > 0L) {
                    visibleTime = endTime - state.visibleStartTime;
                    state.totalVisibleTime += visibleTime;
                    state.visibleStartTime = 0L;
                }

                if (listener != null) {
                    // 这里没有 item 对象，只能通过 position 再取一遍
                    int pos = state.lastPosition;
                    if (pos >= 0 && pos < adapter.getItemCount()) {
                        FeedItem item = adapter.getItem(pos);
                        if (item != null) {
                            listener.onItemHidden(item, pos, state.lastRatio, state.totalVisibleTime);
                        }
                    }
                }

                state.lastRatio = 0f;
            }
        }
    }
}
