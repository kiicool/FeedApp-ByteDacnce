package com.example.feedapp.feed.exposure;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.feedapp.feed.model.FeedItem;
import com.example.feedapp.feed.ui.adapter.FeedAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 负责 RecyclerView 中卡片的曝光统计：
 * - 进入可见区域
 * - 完全可见
 * - 离开可见区域
 * 同时记录停留时长。
 */
public class ExposureManager {

    /**
     * 曝光事件回调。
     */
    public interface ExposureListener {
        /** 第一次进入可视区域（可见比例 > 0） */
        void onItemExposed(@NonNull FeedItem item, int position, float visibleRatio);

        /** 第一次达到 100% 可见 */
        void onItemFullyVisible(@NonNull FeedItem item, int position);

        /** 从可视区域彻底消失 */
        void onItemHidden(@NonNull FeedItem item, int position,
                          float lastVisibleRatio, long totalVisibleMillis);
    }

    /**
     * 单个 item 的曝光状态。
     */
    private static class ItemState {
        float lastRatio = 0f;
        int lastPosition = RecyclerView.NO_POSITION;
        long visibleStartTime = 0L;
        long totalVisibleTime = 0L;
        boolean hasExposed = false;
        boolean hasFullVisible = false;
    }

    private final RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private final FeedAdapter adapter;
    private final ExposureListener listener;

    /** 以 item.id 作为 key 记录状态 */
    private final Map<String, ItemState> stateMap = new HashMap<>();

    /** 上一次执行曝光计算的时间，用于节流 */
    private long lastCheckTime = 0L;

    public ExposureManager(@NonNull RecyclerView recyclerView,
                           @NonNull RecyclerView.LayoutManager layoutManager,
                           @NonNull FeedAdapter adapter,
                           @NonNull ExposureListener listener) {
        this.recyclerView = recyclerView;
        this.layoutManager = layoutManager;
        this.adapter = adapter;
        this.listener = listener;
        attach();
    }

    /**
     * 监听滚动与状态变化，触发曝光计算。
     */
    private void attach() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                // 滚动过程中使用节流版曝光计算（只处理当前可见项）
                checkExposure(false);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                super.onScrollStateChanged(rv, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 停止滚动时强制全量结算一次（包括离屏项）
                    checkExposure(true);
                }
            }
        });

        // 初次布局完成后也结算一次
        recyclerView.post(() -> checkExposure(true));
    }

    private void checkExposure() {
        checkExposure(false);
    }

    /**
     * 核心曝光计算：
     */
    private void checkExposure(boolean force) {
        if (adapter.getItemCount() == 0) return;

        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (lm == null) return;
        layoutManager = lm;

        long now = SystemClock.uptimeMillis();

        if (!force) {
            if (now - lastCheckTime < 120) {
                return;
            }
        }
        lastCheckTime = now;

        int firstVisibleItemPosition = RecyclerView.NO_POSITION;
        int lastVisibleItemPosition = RecyclerView.NO_POSITION;

        if (layoutManager instanceof StaggeredGridLayoutManager) {
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
        } else if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager glm = (GridLayoutManager) layoutManager;
            firstVisibleItemPosition = glm.findFirstVisibleItemPosition();
            lastVisibleItemPosition = glm.findLastVisibleItemPosition();
        } else if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager llm = (LinearLayoutManager) layoutManager;
            firstVisibleItemPosition = llm.findFirstVisibleItemPosition();
            lastVisibleItemPosition = llm.findLastVisibleItemPosition();
        }

        if (firstVisibleItemPosition == RecyclerView.NO_POSITION
                || lastVisibleItemPosition == RecyclerView.NO_POSITION) {
            return;
        }

        Set<String> currentlyVisible = new HashSet<>();
        Rect rect = new Rect();

        // 第一圈：只处理当前屏幕上可见的 item，更新比例和事件
        for (int pos = firstVisibleItemPosition; pos <= lastVisibleItemPosition; pos++) {
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

            state.lastPosition = pos;

            if (ratio > 0f) {
                currentlyVisible.add(item.id);

                // 开始可见，记录起始时间
                if (state.visibleStartTime == 0L) {
                    state.visibleStartTime = now;
                }

                // 第一次曝光
                if (!state.hasExposed) {
                    state.hasExposed = true;
                    if (listener != null) {
                        listener.onItemExposed(item, pos, ratio);
                    }
                }

                // 第一次 100% 可见
                if (!state.hasFullVisible && ratio >= 1f) {
                    state.hasFullVisible = true;
                    if (listener != null) {
                        listener.onItemFullyVisible(item, pos);
                    }
                }
            }

            state.lastRatio = ratio;
        }

        // 如果不是强制结算，滚动时到此结束
        if (!force) {
            return;
        }

        // 第二圈：只在 force=true 时处理已经离开屏幕的 item，结算停留时长并触发隐藏事件
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
                            listener.onItemHidden(item, pos,
                                    state.lastRatio, state.totalVisibleTime);
                        }
                    }
                }

                state.lastRatio = 0f;
            }
        }
    }
}
