package com.example.feedapp;

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
        for (int i = start; i < start + 10; i++) {
            int type = (i % 2 == 0)
                    ? FeedItem.CARD_TYPE_TEXT
                    : FeedItem.CARD_TYPE_IMAGE;
            int layout = (i % 3 == 0)
                    ? FeedItem.LAYOUT_SINGLE_COLUMN
                    : FeedItem.LAYOUT_DOUBLE_COLUMN;

            list.add(new FeedItem(
                    "id_" + i,
                    type,
                    layout,
                    "标题 " + i,
                    "这是第 " + i + " 条测试数据",
                    "https://example.com/" + i   // 先不用真地址
            ));
        }
        return list;
    }
}
