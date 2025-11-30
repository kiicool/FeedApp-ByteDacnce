package com.example.feedapp;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责为从 ContentRepository 获取的内容应用随机布局。
 */
public class MockDataGenerator {

    // 将布局状态作为生成器的内部状态
    private boolean isSingleColumnMode = true;
    private int doubleColumnCount = 0;

    /**
     * 在下拉刷新时调用，以便从头开始生成布局。
     */
    public void reset() {
        this.isSingleColumnMode = true;
        this.doubleColumnCount = 0;
    }

    /**
     * 生成一页的模拟数据。
     * 它从 ContentRepository 获取内容，然后为其分配随机布局。
     * @param start     起始索引
     * @param pageSize  每页数量
     * @return 一页 FeedItem 列表
     */
    public List<FeedItem> generatePageData(int start, int pageSize) {
        List<FeedItem> list = new ArrayList<>();

        // --- 随机布局生成逻辑 (保持不变) ---
        int[] rowWeights = {5, 4, 3, 2}; // 对应 2, 3, 4, 5 行
        int totalWeight = 0;
        for (int w : rowWeights) {
            totalWeight += w;
        }

        for (int i = start; i < start + pageSize; i++) {
            int layout;
            if (this.isSingleColumnMode) {
                layout = FeedItem.LAYOUT_SINGLE_COLUMN;
                this.isSingleColumnMode = false;
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
                this.doubleColumnCount = doubleRows * 2;
            } else {
                layout = FeedItem.LAYOUT_DOUBLE_COLUMN;
                this.doubleColumnCount--;
                if (this.doubleColumnCount == 0) {
                    this.isSingleColumnMode = true;
                }
            }

            // --- 从 ContentRepository 获取内容 ---
            String id = "id_" + i;
            // 随机从内容库中挑选，避免固定的重复模式
            int randomIndex = (int) (Math.random() * ContentRepository.getTotalCount());
            ContentEntry content = ContentRepository.getEntry(randomIndex);

            // --- 【最终修正】根据获取到的内容类型，创建对应的 FeedItem ---
            switch (content.type) {
                case ContentEntry.TYPE_VIDEO:
                    // 调用包含 imageUrl 和 imageRes 的视频构造函数
                    list.add(new FeedItem(id, layout, content.title, content.description, content.imageUrl, content.imageRes, content.videoUrl));
                    break;

                case ContentEntry.TYPE_TEXT:
                    // 调用纯文本的构造函数 (它内部会自动设置 cardType)
                    list.add(new FeedItem(id, layout, content.title, content.description));
                    break;

                case ContentEntry.TYPE_IMAGE:
                default:
                    // 判断 ContentEntry 是用网络图还是本地图创建的
                    if (content.imageUrl != null) {
                        // 调用接收网络 URL 的图文构造函数
                        list.add(new FeedItem(id, layout, content.title, content.description, content.imageUrl));
                    } else {
                        // 调用接收本地资源 ID 的图文构造函数
                        list.add(new FeedItem(id, layout, content.title, content.description, content.imageRes));
                    }
                    break;
            }
        }
        return list;
    }
}
