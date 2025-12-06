package com.example.feedapp;

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

        // 使用 GridLayoutManager + SpanSizeLookup
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int viewType = adapter.getItemViewType(position);
                // footer 独占一整行
                if (viewType == FeedAdapter.VIEW_TYPE_FOOTER) {
                    return 2;
                }

                FeedItem item = adapter.getItem(position);
                if (item != null && item.layoutType == FeedItem.LAYOUT_SINGLE_COLUMN) {
                    // 单列：占满两列（整行）
                    return 2;
                } else {
                    // 双列：占一列
                    return 1;
                }
            }
        });

        this.layoutManager = gridLayoutManager;
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemViewCacheSize(20);
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


        // 滚动监听
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                super.onScrollStateChanged(rv, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 列表静止，自动播放当前屏幕中最合适的视频
                    autoPlayCenterVideo();
                } else {
                    // 手指拖动 / 惯性滑动时一律停播，保证滚动流畅
                    if (videoPlayerManager != null) {
                        videoPlayerManager.stop();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0 || layoutManager == null) return;

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItemPosition = -1;

                if (layoutManager instanceof GridLayoutManager) {
                    lastVisibleItemPosition =
                            ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
                }

                final int THRESHOLD = 3;
                if (lastVisibleItemPosition != -1
                        && !isLoadingMore
                        && hasMore
                        && lastVisibleItemPosition >= totalItemCount - 1 - THRESHOLD) {
                    rv.post(MainActivity.this::loadMore);
                }
            }
        });

        adapter.setFooterRetryListener(this::loadMore);

        // 曝光统计
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
            // 当前已经在这个卡片上播放，无需重新切 Surface / 重绑 PlayerView
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
                // ======= 网络失败分支：这里要走本地缓存 =======
                isLoadingMore = false;

                if (isRefresh) {
                    // 下拉刷新失败 → 尝试从本地缓存恢复
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

            // ======= 正常成功分支（模拟“网络成功”） =======
            int start = currentPage * PAGE_SIZE;
            List<FeedItem> page = mockDataGenerator.generatePageData(start, PAGE_SIZE);
            isLoadingMore = false;

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
