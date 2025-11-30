package com.example.feedapp;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责生成包含复杂随机布局的模拟数据。
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
     * @param start     起始索引
     * @param pageSize  每页数量
     * @return 一页 FeedItem 列表
     */
    public List<FeedItem> generatePageData(int start, int pageSize) {
        List<FeedItem> list = new ArrayList<>();
        int[] localImages = new int[]{R.drawable.test1, R.drawable.test2, R.drawable.test3};
        String[] onlineImages = new String[]{
                "https://picsum.photos/id/1015/800/600",
                "https://picsum.photos/id/1040/800/600",
                "https://picsum.photos/id/237/800/600",
        };
        // 一些可用的公共视频URL
        String[] onlineVideos = new String[]{
                "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4",
                "http://vfx.mtime.cn/Video/2019/03/18/mp4/190318231014076505.mp4",
                "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4"
        };

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

            // --- 【修改】内容生成逻辑，加入视频 ---
            String id = "id_" + i;
            int typeSelector = i % 3; // 0=图片, 1=文本, 2=视频

            if (typeSelector == 1) { // 生成文本
                list.add(new FeedItem(id, FeedItem.CARD_TYPE_TEXT, layout, "纯文本标题 " + i, "这是一段纯文本描述内容，用于测试列表中的不同卡片类型。", (String) null));
            } else if (typeSelector == 2) { // 生成视频
                list.add(new FeedItem(id, layout, "这是一个视频 " + i, "视频描述，点击可以播放和暂停。", onlineImages[i % onlineImages.length], onlineVideos[i % onlineVideos.length]));
            } else { // 生成图片
                list.add(new FeedItem(id, FeedItem.CARD_TYPE_IMAGE, layout, "图片标题 " + i, "这是一张网络图片。", onlineImages[i % onlineImages.length]));
            }
        }
        return list;
    }

}
