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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ExposureManager exposureManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    // 用一个通用的 LayoutManager 引用来保存实际的 GridLayoutManager
    private RecyclerView.LayoutManager layoutManager;

    private boolean isLoadingMore = false;
    private boolean hasMore = true;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;

    // 持有 MockDataGenerator 的实例
    private MockDataGenerator mockDataGenerator;
    // 视频播放管理器
    private VideoPlayerManager videoPlayerManager;

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

        // ==== 关键修改：使用 GridLayoutManager + SpanSizeLookup ====
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
                if (item.layoutType == FeedItem.LAYOUT_SINGLE_COLUMN) {
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
                videoPlayerManager.play(vh, item.videoUrl);
            }
        });

        // 滚动监听，处理 loadMore
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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

        // 曝光统计，继续用通用的 layoutManager 引用
        exposureManager = new ExposureManager(
                recyclerView,
                layoutManager,
                adapter,
                new ExposureManager.ExposureListener() {
                    @Override
                    public void onItemExposed(FeedItem item, int position, float visibleRatio) {
                        // 可选：不做事
                    }

                    @Override
                    public void onItemFullyVisible(FeedItem item, int position) {
                        String msg = "FULL    id=" + item.id + " pos=" + position;
                        ExposureLogger.log(msg);

                        // 自动播放视频
                        RecyclerView.ViewHolder holder =
                                recyclerView.findViewHolderForAdapterPosition(position);
                        if (holder instanceof FeedAdapter.VideoVH) {
                            videoPlayerManager.play(
                                    (FeedAdapter.VideoVH) holder,
                                    item.videoUrl
                            );
                        } else {
                            videoPlayerManager.stop();
                        }
                    }

                    @Override
                    public void onItemHidden(FeedItem item,
                                             int position,
                                             float lastVisibleRatio,
                                             long totalVisibleMillis) {
                        // 可选：不做事
                    }
                }
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
            List<FeedItem> page =
                    mockDataGenerator.generatePageData(start, PAGE_SIZE);
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
