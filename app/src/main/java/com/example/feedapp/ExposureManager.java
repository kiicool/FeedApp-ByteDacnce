package com.example.feedapp;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

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
    // 【修改】使用通用的 LayoutManager 类型
    private final RecyclerView.LayoutManager layoutManager;
    private final FeedAdapter adapter;
    private final ExposureListener listener;

    private final Map<String, ItemState> stateMap = new HashMap<>();

    // 【核心修改】构造函数接受通用的 LayoutManager
    public ExposureManager(RecyclerView recyclerView,
                           RecyclerView.LayoutManager layoutManager,
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
        if (adapter.getItemCount() == 0 || layoutManager == null) return;

        // 【核心修改】兼容不同 LayoutManager 来获取可见范围
        int firstVisibleItemPosition = RecyclerView.NO_POSITION;
        int lastVisibleItemPosition = RecyclerView.NO_POSITION;

        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager glm = (GridLayoutManager) layoutManager;
            firstVisibleItemPosition = glm.findFirstVisibleItemPosition();
            lastVisibleItemPosition = glm.findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager sglm = (StaggeredGridLayoutManager) layoutManager;
            int[] firstVisibleItems = sglm.findFirstVisibleItemPositions(null);
            int[] lastVisibleItems = sglm.findLastVisibleItemPositions(null);

            if (firstVisibleItems != null && firstVisibleItems.length > 0) {
                firstVisibleItemPosition = firstVisibleItems[0];
                for (int pos : firstVisibleItems) {
                    if (pos < firstVisibleItemPosition) {
                        firstVisibleItemPosition = pos;
                    }
                }
            }
            if (lastVisibleItems != null && lastVisibleItems.length > 0) {
                lastVisibleItemPosition = lastVisibleItems[0];
                for (int pos : lastVisibleItems) {
                    if (pos > lastVisibleItemPosition) {
                        lastVisibleItemPosition = pos;
                    }
                }
            }
        }

        if (firstVisibleItemPosition == RecyclerView.NO_POSITION || lastVisibleItemPosition == RecyclerView.NO_POSITION) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        Set<String> currentlyVisible = new HashSet<>();

        Rect rect = new Rect();

        // 后续的曝光计算逻辑，因为使用了通用的 first/last 变量，所以无需修改
        for (int pos = firstVisibleItemPosition; pos <= lastVisibleItemPosition; pos++) {
            if (pos < 0 || pos >= adapter.getItemCount()) continue;

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

            if (state.lastRatio == 0f && ratio > 0f) {
                state.visibleStartTime = now;
                if (!state.hasExposed) {
                    state.hasExposed = true;
                    if (listener != null) {
                        listener.onItemExposed(item, pos, ratio);
                    }
                }
            }

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

        for (Map.Entry<String, ItemState> entry : stateMap.entrySet()) {
            String id = entry.getKey();
            ItemState state = entry.getValue();

            if (state.lastRatio > 0f && !currentlyVisible.contains(id)) {
                long endTime = now;
                long visibleTime = 0L;
                if (state.visibleStartTime > 0L) {
                    visibleTime = endTime - state.visibleStartTime;
                    state.totalVisibleTime += visibleTime;
                    state.visibleStartTime = 0L;
                }

                if (listener != null) {
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
