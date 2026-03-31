package com.example.feedapp.feed.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.feedapp.R;
import com.example.feedapp.feed.data.LocalFeedCache;
import com.example.feedapp.feed.data.MockDataGenerator;
import com.example.feedapp.feed.exposure.ExposureLogger;
import com.example.feedapp.feed.exposure.ExposureManager;
import com.example.feedapp.feed.model.FeedItem;
import com.example.feedapp.feed.player.VideoPlayerManager;
import com.example.feedapp.feed.ui.adapter.FeedAdapter;
import com.example.feedapp.feed.ui.factory.BannerCardFactory;
import com.example.feedapp.debug.DebugExposureActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ExposureManager exposureManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private boolean isLoadingMore = false;
    private boolean hasMore = true;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;

    private MockDataGenerator mockDataGenerator;
    // 视频播放管理器
    private VideoPlayerManager videoPlayerManager;
    //测试缓存功能开关。使用方法：先设置为False，启用网络加载;设置为true再次运行，使用的是上一次加载的缓存。
    private boolean debugForceError = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoPlayerManager = VideoPlayerManager.getInstance(this);
        mockDataGenerator = new MockDataGenerator();

        setupToolbar();
        setupRecyclerView();
        setupListeners();

        // 首次进入自动刷新
        swipeRefreshLayout.setRefreshing(true);
        refreshData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(toolbar);
    }

    private void setupRecyclerView() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        adapter = new FeedAdapter(this);
        adapter.registerCardFactory(new BannerCardFactory());

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                int total = adapter.getItemCount();
                if (position < 0 || position >= total) {
                    return 2;
                }

                int viewType = adapter.getItemViewType(position);
                if (viewType == FeedAdapter.VIEW_TYPE_FOOTER) return 2;

                FeedItem item = adapter.getItem(position);
                if (item == null) return 2;

                return item.layoutType == FeedItem.LAYOUT_SINGLE_COLUMN ? 2 : 1;
            }
        });


        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
        layoutManager = gridLayoutManager;

        // 一些基础性能优化（可选，但推荐）
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setItemAnimator(null);
    }



    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // 视频点击播放
        adapter.setOnVideoClickListener((vh, item) -> {
            if (item.videoUrl != null) {
                videoPlayerManager.play(
                        vh,
                        String.valueOf(item.id), // itemKey
                        item.videoUrl
                );
            }
        });

        // ✅ 只保留这一份滚动监听（里面有自动播放 + 预加载）
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            private int lastPreloadPosition = -1;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                super.onScrollStateChanged(rv, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 列表静止，自动播放当前屏幕中最合适的视频
                    autoPlayCenterVideo();
                } else {
                    // 手指拖动/惯性滑动时一律停播，保证滚动流畅
                    if (videoPlayerManager != null) {
                        videoPlayerManager.stop();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                if (!(layoutManager instanceof GridLayoutManager)) return;

                GridLayoutManager glm = (GridLayoutManager) layoutManager;
                int lastVisible = glm.findLastVisibleItemPosition();
                int total = adapter.getItemCount();

                // ① 距离底部还有 6 个 item 时预加载「即将出现」的图片
                if (lastVisible >= 0 && total - lastVisible <= 6) {
                    int startPreloadPos = Math.max(lastVisible + 1, lastPreloadPosition + 1);
                    if (startPreloadPos < total) {
                        int end = Math.min(total, startPreloadPos + 6);
                        List<FeedItem> sub = adapter.getSubItems(startPreloadPos, end);
                        preloadImages(sub, sub.size());
                        lastPreloadPosition = end - 1;
                    }
                }

                if (lastVisible >= 0
                        && total > 0
                        && total - lastVisible <= 3    // 距离底部 3 个以内就触发
                        && !isLoadingMore
                        && hasMore) {

                    loadMore();
                }
            }

        });

        adapter.setFooterRetryListener(this::loadMore);

        // 曝光统计保持不变
        exposureManager = new ExposureManager(
                recyclerView,
                layoutManager,
                adapter,
                new ExposureManager.ExposureListener() {
                    @Override
                    public void onItemExposed(FeedItem item, int position, float visibleRatio) {
                    }

                    @Override
                    public void onItemFullyVisible(FeedItem item, int position) {
                        String msg = "FULL    id=" + item.id + " pos=" + position;
                        ExposureLogger.log(msg);

                        // 不在这里直接播放视频，只在列表静止时统一处理自动播放
                        if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                            autoPlayCenterVideo();
                        }
                    }

                    @Override
                    public void onItemHidden(FeedItem item,
                                             int position,
                                             float lastVisibleRatio,
                                             long totalVisibleMillis) {
                    }
                }
        );
    }


    /**
     * 选择当前屏幕中“最合适”的视频卡片并自动播放：
     * - 只在当前可见范围内查找视频；
     * - 选择离 RecyclerView 垂直中线最近的一个视频卡。
     */
    private void autoPlayCenterVideo() {
        if (layoutManager == null || adapter == null) return;

        int firstVisible = RecyclerView.NO_POSITION;
        int lastVisible = RecyclerView.NO_POSITION;

        if (layoutManager instanceof GridLayoutManager) {
            GridLayoutManager glm = (GridLayoutManager) layoutManager;
            firstVisible = glm.findFirstVisibleItemPosition();
            lastVisible = glm.findLastVisibleItemPosition();
        } else {
            return;
        }

        if (firstVisible == RecyclerView.NO_POSITION
                || lastVisible == RecyclerView.NO_POSITION) {
            return;
        }

        int rvCenterY = recyclerView.getHeight() / 2;

        FeedAdapter.VideoVH bestVH = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int pos = firstVisible; pos <= lastVisible; pos++) {
            RecyclerView.ViewHolder holder =
                    recyclerView.findViewHolderForAdapterPosition(pos);
            if (!(holder instanceof FeedAdapter.VideoVH)) {
                continue;
            }

            View itemView = holder.itemView;
            int[] loc = new int[2];
            itemView.getLocationOnScreen(loc);
            int itemCenterY = loc[1] + itemView.getHeight() / 2;
            int distance = Math.abs(itemCenterY - rvCenterY);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestVH = (FeedAdapter.VideoVH) holder;
            }
        }
        if (bestVH == null) {
            videoPlayerManager.stop();
            return;
        }

        FeedAdapter.VideoVH playingVH = videoPlayerManager.getCurrentViewHolder();
        if (playingVH != null && playingVH == bestVH) {
            return;
        }

        int position = bestVH.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        FeedItem item = adapter.getItem(position);
        if (item == null || item.videoUrl == null) {
            return;
        }

        // 正常播放：触发按 itemKey 记忆进度的逻辑
        videoPlayerManager.play(
                bestVH,
                String.valueOf(item.id),
                item.videoUrl
        );
    }

    private void refreshData() {
        currentPage = 0;
        hasMore = true;
        isLoadingMore = false;
        adapter.setFooterState(FeedAdapter.FOOTER_STATE_HIDDEN);
        mockDataGenerator.reset();
        requestPage(true);
    }

    private void loadMore() {
        if (isLoadingMore || !hasMore) {
            return;
        }
        isLoadingMore = true;
        adapter.setFooterState(FeedAdapter.FOOTER_STATE_LOADING);
        requestPage(false);
    }

    private void requestPage(boolean isRefresh) {
        // 这里可以看到每次请求到底是不是在模拟错误
        Log.d("MainActivity", "requestPage() called, isRefresh=" + isRefresh
                + ", debugForceError=" + debugForceError);

        handler.postDelayed(() -> {

            boolean simulateError = debugForceError;  // 统一用这个开关
            Log.d("MainActivity", "requestPage() inner, simulateError="
                    + simulateError + ", isRefresh=" + isRefresh);

            if (simulateError) {
                // 网络失败分支：走本地缓存
                isLoadingMore = false;

                if (isRefresh) {
                    // 下拉刷新失败，则尝试从本地缓存恢复
                    List<FeedItem> cached = LocalFeedCache.load(this);
                    if (cached != null && !cached.isEmpty()) {
                        Log.d("MainActivity", "use local cache, size=" + cached.size());
                        adapter.setItems(cached);
                        hasMore = false;
                        adapter.setFooterState(FeedAdapter.FOOTER_STATE_NO_MORE);
                        Toast.makeText(this,
                                "网络失败，已使用本地缓存",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("MainActivity", "no local cache available");
                        Toast.makeText(this,
                                "刷新失败，且暂无本地缓存",
                                Toast.LENGTH_SHORT).show();
                    }
                    swipeRefreshLayout.setRefreshing(false);

                } else {
                    // 加载更多失败，只提示错误，不读缓存
                    adapter.setFooterState(FeedAdapter.FOOTER_STATE_ERROR);
                    Toast.makeText(this,
                            "加载更多失败，可以下拉刷新或稍后重试",
                            Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // 网络请求成功分支
            int start = currentPage * PAGE_SIZE;
            List<FeedItem> page = mockDataGenerator.generatePageData(start, PAGE_SIZE);
            isLoadingMore = false;
            //预加载图片
            preloadImages(page, 8);
            if (isRefresh) {
                adapter.setItems(page);
                swipeRefreshLayout.setRefreshing(false);
            } else {
                adapter.setFooterState(FeedAdapter.FOOTER_STATE_HIDDEN);
                adapter.addItems(page);
            }

            // 成功时更新本地缓存
            LocalFeedCache.save(this, adapter.getItemsSnapshot());

            if (page.size() < PAGE_SIZE) {
                hasMore = false;
                adapter.setFooterState(FeedAdapter.FOOTER_STATE_NO_MORE);
            } else {
                hasMore = true;
                currentPage++;
            }
        }, 800);
    }
    private void preloadImages(List<FeedItem> items, int maxCount) {
        for (int i = 0; i < items.size() && i < maxCount; i++) {
            FeedItem fi = items.get(i);
            Object src = fi.imageUrl != null ? fi.imageUrl : fi.imageRes;
            // 没图就跳过
            if (src == null || (src instanceof Integer && ((Integer) src) == 0)) {
                continue;
            }

            Glide.with(this)
                    .load(src)
                    .override(400, 300)   // 随便给个大小，别太大
                    .preload();
        }
    }




    @Override
    protected void onPause() {
        super.onPause();
        if (videoPlayerManager != null) {
            videoPlayerManager.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPlayerManager != null) {
            videoPlayerManager.release();
        }
    }

    // Menu 相关
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_debug_exposure) {
            startActivity(new Intent(this, DebugExposureActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
