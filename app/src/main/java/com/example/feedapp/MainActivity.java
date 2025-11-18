package com.example.feedapp;  // 改成你的包名

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FeedAdapter adapter;
    private boolean isLoadingMore = false;
    private boolean hasMore = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 就是你刚改的那个 XML

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);

        adapter = new FeedAdapter(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                FeedItem item = adapter.getItem(position);
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

        // 滑动到底自动加载更多（简单版本）
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

        // 首次进入自动刷新一次
        swipeRefreshLayout.setRefreshing(true);
        refreshData();
    }

    private void refreshData() {
        List<FeedItem> list = mockData(0);
        adapter.setItems(list);
        swipeRefreshLayout.setRefreshing(false);
        hasMore = true;
    }

    private void loadMore() {
        isLoadingMore = true;
        List<FeedItem> more = mockData(adapter.getItemCount());
        if (more.isEmpty()) {
            hasMore = false;
        } else {
            adapter.addItems(more);
        }
        isLoadingMore = false;
    }

    // 简单模拟一些假数据
    private List<FeedItem> mockData(int start) {
        List<FeedItem> list = new ArrayList<>();

        // 本地图片
        int[] localImages = new int[]{
                R.drawable.test1,
                R.drawable.test2,
                R.drawable.test3
        };

        // 网络图片
        String[] onlineImages = new String[]{
                "https://picsum.photos/400/300",
                "https://picsum.photos/id/237/400/300",
                "https://picsum.photos/id/870/600/400",
                "https://picsum.photos/id/1084/400/300",
                "https://picsum.photos/seed/picsum/400/300"
        };

        for (int i = start; i < start + 10; i++) {

            int cardType = (i % 2 == 0)
                    ? FeedItem.CARD_TYPE_IMAGE   // 测试网络图
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
                    // 网络图片
                    list.add(new FeedItem(
                            id, cardType, layout,
                            "网络图片 " + i,
                            "这是一个网络图片",
                            onlineImages[i % onlineImages.length]
                    ));
                } else {
                    // 本地图片
                    list.add(new FeedItem(
                            id, cardType, layout,
                            "本地图片 " + i,
                            "这是一个本地图片",
                            localImages[i % localImages.length]
                    ));
                }
            }
        }

        return list;
    }

}
