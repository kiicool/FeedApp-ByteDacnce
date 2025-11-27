package com.example.feedapp;

import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ExposureManager exposureManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FeedAdapter adapter;

    private boolean isLoadingMore = false;
    private boolean hasMore = true;
    private int currentPage = 0;
    private boolean isSingleColumnMode = true;
    private int doubleColumnCount = 0;
    private static final int PAGE_SIZE = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(toolbar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);

        adapter = new FeedAdapter(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                if (position < 0 || position >= adapter.getItemCount()) {
                    return 2; // 对于无效位置，返回默认占位宽度
                }

                boolean isFooter = (position == adapter.getItemCount() - 1) &&
                        (adapter.getItemViewType(position) == FeedAdapter.VIEW_TYPE_FOOTER);

                if (isFooter) {
                    return 2; // Footer 占两列
                }

                // 3. 在确保位置有效后再获取item
                FeedItem item = adapter.getItem(position);
                if (item.layoutType == FeedItem.LAYOUT_SINGLE_COLUMN) {
                    return 2; // 单列，占两列
                } else {
                    return 1; // 双列，占一列
                }
            }
        });

        // 适当提高预取数量，有利于滑动时提前创建/绑定下一个屏幕的 View，减少抖动
        layoutManager.setInitialPrefetchItemCount(6);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // 滑到底加载更多
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

                // 预加载阈值，例如还剩3个item滚动到底部时
                final int THRESHOLD = 3;

                if (!isLoadingMore && hasMore && lastVisibleItemPosition >= totalItemCount - 1 - THRESHOLD) {
                    rv.post(MainActivity.this::loadMore);
                }
            }
        });

        // footer 点击重试
        adapter.setFooterRetryListener(() -> {
            if (!isLoadingMore && hasMore) {
                loadMore();
            }
        });

        // 首次进入自动刷新
        swipeRefreshLayout.setRefreshing(true);
        refreshData();

        exposureManager = new ExposureManager(recyclerView, layoutManager, adapter,
                new ExposureManager.ExposureListener() {
                    @Override
                    public void onItemExposed(FeedItem item, int position, float visibleRatio) {
                        String msg = "EXPOSE  id=" + item.id +
                                " pos=" + position +
                                " ratio=" + String.format("%.2f", visibleRatio);
                        ExposureLogger.log(msg);
                    }

                    @Override
                    public void onItemFullyVisible(FeedItem item, int position) {
                        String msg = "FULL    id=" + item.id +
                                " pos=" + position;
                        ExposureLogger.log(msg);
                    }

                    @Override
                    public void onItemHidden(FeedItem item,
                                             int position,
                                             float lastVisibleRatio,
                                             long totalVisibleMillis) {
                        String msg = "HIDE    id=" + item.id +
                                " pos=" + position +
                                " lastRatio=" + String.format("%.2f", lastVisibleRatio) +
                                " time=" + totalVisibleMillis + "ms";
                        ExposureLogger.log(msg);
                    }
                });

    }

    private void refreshData() {
        currentPage = 0;
        hasMore = true;
        isLoadingMore = false;
        adapter.setFooterState(FeedAdapter.FOOTER_STATE_HIDDEN);

        // 重置布局状态
        this.isSingleColumnMode = true;
        this.doubleColumnCount = 0;

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
            List<FeedItem> page = mockData(start);

            // 先将 isLoadingMore 状态恢复
            isLoadingMore = false;

            // 将新数据添加到 Adapter
            if (isRefresh) {
                adapter.setItems(page);
                swipeRefreshLayout.setRefreshing(false);
            } else {
                // 在添加新数据之前，先把之前的 Footer 隐藏掉
                // 这样可以避免新 item 插在 Footer 前面导致闪烁
                adapter.setFooterState(FeedAdapter.FOOTER_STATE_HIDDEN);
                adapter.addItems(page);
            }

            // 判断是否还有更多数据
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
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_debug_exposure) {
            startActivity(new android.content.Intent(this, DebugExposureActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<FeedItem> mockData(int start) {
        List<FeedItem> list = new ArrayList<>();
        int[] localImages = new int[]{R.drawable.test1, R.drawable.test2, R.drawable.test3};
        String[] onlineImages = new String[]{
                "https://picsum.photos/400/300",
                "https://picsum.photos/id/237/400/300",
                "https://picsum.photos/id/870/600/400",
                "https://picsum.photos/id/1084/400/300",
                "https://picsum.photos/seed/picsum/400/300"
        };

        int[] rowWeights = {5, 4, 3, 2};
        int totalWeight = 0;
        for (int w : rowWeights) {
            totalWeight += w;
        }


        for (int i = start; i < start + PAGE_SIZE; i++) {
            int layout;

            // 1. 决定布局类型（现在使用 this.isSingleColumnMode）
            if (this.isSingleColumnMode) {
                layout = FeedItem.LAYOUT_SINGLE_COLUMN;
                this.isSingleColumnMode = false; // 进入双列模式

                // 决定接下来双列的行数
                int rand = (int) (Math.random() * totalWeight);
                int cumulativeWeight = 0;
                int doubleRows = 2;
                for (int j = 0; j < rowWeights.length; j++) {
                    cumulativeWeight += rowWeights[j];
                    if (rand < cumulativeWeight) {
                        doubleRows = j + 2;
                        break;
                    }
                }
                // 更新成员变量
                this.doubleColumnCount = doubleRows * 2;

            } else {
                layout = FeedItem.LAYOUT_DOUBLE_COLUMN;
                this.doubleColumnCount--;

                if (this.doubleColumnCount == 0) {
                    this.isSingleColumnMode = true; // 双列结束，准备下一个单列
                }
            }

            int cardType = (i % 2 == 0)
                    ? FeedItem.CARD_TYPE_IMAGE
                    : FeedItem.CARD_TYPE_TEXT;
            String id = "id_" + i;

            if (cardType == FeedItem.CARD_TYPE_TEXT) {
                list.add(new FeedItem(id, cardType, layout, "文本标题 " + i, "文本内容 " + i, (String) null));
            } else {
                if (i % 3 == 0) {
                    list.add(new FeedItem(id, cardType, layout, "网络图片 " + i, "网络图 " + i, onlineImages[i % onlineImages.length]));
                } else {
                    list.add(new FeedItem(id, cardType, layout, "本地图片 " + i, "本地图 " + i, localImages[i % localImages.length]));
                }
            }
        }
        return list;
    }
}
