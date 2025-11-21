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
                // footer 占两列，普通卡片按自己的 layoutType
                if (position == adapter.getItemCount() - 1 &&
                        adapter.getItemCount() > 0 &&
                        adapter.getItemViewType(position) == 100) { // VIEW_TYPE_FOOTER
                    return 2;
                }

                FeedItem item;
                try {
                    item = adapter.getItem(position);
                } catch (IndexOutOfBoundsException e) {
                    return 2;
                }

                if (item.layoutType == FeedItem.LAYOUT_SINGLE_COLUMN) {
                    return 2; // 单列，占两列
                } else {
                    return 1; // 双列，占一列
                }
            }
        });

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // 滑到底加载更多
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@Nullable RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (rv == null || dy <= 0) return;

                int total = layoutManager.getItemCount();
                int last = layoutManager.findLastVisibleItemPosition();

                if (!isLoadingMore && hasMore && last >= total - 3) {
                    loadMore();
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

        requestPage(true);
    }

    private void loadMore() {
        if (isLoadingMore || !hasMore) return;

        isLoadingMore = true;
        adapter.setFooterState(FeedAdapter.FOOTER_STATE_LOADING);

        requestPage(false);
    }

    // 模拟网络请求：用 Handler 延迟 800ms
    private void requestPage(boolean isRefresh) {
        handler.postDelayed(() -> {

            // 设置加载失败/成果
            boolean simulateError = false;
            // 第 3 页模拟失败：
            //simulateError = (!isRefresh && currentPage == 2);

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

            if (isRefresh) {
                adapter.setItems(page);
                swipeRefreshLayout.setRefreshing(false);
            } else {
                adapter.addItems(page);
            }

            // 判断是否还有更多数据（这里简单按页数判断）
            if (page.size() < PAGE_SIZE) {
                hasMore = false;
                adapter.setFooterState(FeedAdapter.FOOTER_STATE_NO_MORE);
            } else {
                hasMore = true;
                currentPage++;
                isLoadingMore = false;
                adapter.setFooterState(FeedAdapter.FOOTER_STATE_HIDDEN);
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

        int[] localImages = new int[]{
                R.drawable.test1,
                R.drawable.test2,
                R.drawable.test3
        };

        String[] onlineImages = new String[]{
                "https://picsum.photos/400/300",
                "https://picsum.photos/id/237/400/300",
                "https://picsum.photos/id/870/600/400",
                "https://picsum.photos/id/1084/400/300",
                "https://picsum.photos/seed/picsum/400/300"
        };

        for (int i = start; i < start + PAGE_SIZE; i++) {
            int cardType = (i % 2 == 0)
                    ? FeedItem.CARD_TYPE_IMAGE
                    : FeedItem.CARD_TYPE_TEXT;

            int layout = (i % 3 == 0)
                    ? FeedItem.LAYOUT_SINGLE_COLUMN
                    : FeedItem.LAYOUT_DOUBLE_COLUMN;

            String id = "id_" + i;

            if (cardType == FeedItem.CARD_TYPE_TEXT) {
                list.add(new FeedItem(
                        id, cardType, layout,
                        "文本标题 " + i,
                        "文本内容 " + i,
                        (String) null
                ));
            } else {
                if (i % 2 == 0) {
                    list.add(new FeedItem(
                            id, cardType, layout,
                            "网络图片 " + i,
                            "网络图 " + i,
                            onlineImages[i % onlineImages.length]
                    ));
                } else {
                    list.add(new FeedItem(
                            id, cardType, layout,
                            "本地图片 " + i,
                            "本地图 " + i,
                            localImages[i % localImages.length]
                    ));
                }
            }
        }

        return list;
    }
}
