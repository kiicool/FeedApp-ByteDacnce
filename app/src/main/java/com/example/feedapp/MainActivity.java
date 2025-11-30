package com.example.feedapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ExposureManager exposureManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private RecyclerView.LayoutManager layoutManager; // 使用通用的 LayoutManager

    private boolean isLoadingMore = false;
    private boolean hasMore = true;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;

    // 持有 MockDataGenerator 的实例
    private MockDataGenerator mockDataGenerator;
    // 【新增】视频播放管理器
    private VideoPlayerManager videoPlayerManager;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 【新增】初始化 VideoPlayerManager
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

        // 【修改】切换为 StaggeredGridLayoutManager 以更好地支持视频和不同高度的 Item
        this.layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        ((StaggeredGridLayoutManager) this.layoutManager)
                .setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);

        recyclerView.setLayoutManager(this.layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemViewCacheSize(20);
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // 【新增】为 Adapter 设置视频点击监听器，实现手动播放/暂停
        adapter.setOnVideoClickListener((vh, item) -> {
            if (item.videoUrl != null) {
                videoPlayerManager.play(vh, item.videoUrl);
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0 || layoutManager == null) return;

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItemPosition = -1;

                if (layoutManager instanceof StaggeredGridLayoutManager) {
                    int[] lastPositions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
                    if (lastPositions != null && lastPositions.length > 0) {
                        for (int pos : lastPositions) {
                            if (pos > lastVisibleItemPosition) {
                                lastVisibleItemPosition = pos;
                            }
                        }
                    }
                }

                final int THRESHOLD = 3;
                if (lastVisibleItemPosition != -1 && !isLoadingMore && hasMore &&
                        lastVisibleItemPosition >= totalItemCount - 1 - THRESHOLD) {
                    rv.post(MainActivity.this::loadMore);
                }
            }
        });

        adapter.setFooterRetryListener(this::loadMore);

        // 【修改】设置 ExposureManager 的回调以实现自动播放
        exposureManager = new ExposureManager(recyclerView, layoutManager, adapter,
                new ExposureManager.ExposureListener() {
                    @Override
                    public void onItemExposed(FeedItem item, int position, float visibleRatio) {
                        // 可以留空
                    }

                    @Override
                    public void onItemFullyVisible(FeedItem item, int position) {
                        String msg = "FULL    id=" + item.id + " pos=" + position;
                        ExposureLogger.log(msg);

                        // 自动播放的核心逻辑
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
                        if (holder instanceof FeedAdapter.VideoVH) {
                            videoPlayerManager.play((FeedAdapter.VideoVH) holder, item.videoUrl);
                        } else {
                            // 如果滑到的是非视频区域，则停止所有播放
                            videoPlayerManager.stop();
                        }
                    }

                    @Override
                    public void onItemHidden(FeedItem item, int position, float lastVisibleRatio, long totalVisibleMillis) {
                        // 可以留空
                    }
                });
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
        handler.postDelayed(() -> {
            boolean simulateError = false;
            if (simulateError) {
                isLoadingMore = false;
                if (isRefresh) {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "刷新失败", Toast.LENGTH_SHORT).show();
                } else {
                    adapter.setFooterState(FeedAdapter.FOOTER_STATE_ERROR);
                }
                return;
            }

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

            if (page.size() < PAGE_SIZE) {
                hasMore = false;
                adapter.setFooterState(FeedAdapter.FOOTER_STATE_NO_MORE);
            } else {
                hasMore = true;
                currentPage++;
            }
        }, 800);
    }

    // 【新增】生命周期管理，确保播放器被正确释放
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

    // Menu 相关代码
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
