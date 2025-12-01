package com.example.feedapp;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责为从 ContentRepository 获取的内容应用布局和类型控制。
 */
public class MockDataGenerator {

    // 布局状态：单列 / 双列
    private boolean isSingleColumnMode = true;
    private int doubleColumnCount = 0;

    // 控制视频出现频率：8~10 个卡片才出一次视频
    private static final int MIN_CARDS_BEFORE_VIDEO = 8;
    private static final int MAX_CARDS_BEFORE_VIDEO = 10;

    // 距离上一次成功生成视频卡片已经过去了多少个卡片
    private int cardsSinceLastVideo = 0;
    private int nextVideoInterval = getRandomVideoInterval();

    /**
     * 在下拉刷新时调用，以便从头开始生成布局和视频节奏。
     */
    public void reset() {
        this.isSingleColumnMode = true;
        this.doubleColumnCount = 0;
        this.cardsSinceLastVideo = 0;
        this.nextVideoInterval = getRandomVideoInterval();
    }

    /**
     * 生成一页的模拟数据。
     */
    public List<FeedItem> generatePageData(int start, int pageSize) {
        List<FeedItem> list = new ArrayList<>();

        // 行布局权重：2/3/4/5 行
        int[] rowWeights = {5, 4, 3, 2};
        int totalWeight = 0;
        for (int w : rowWeights) {
            totalWeight += w;
        }

        int totalCount = ContentRepository.getTotalCount();

        for (int i = start; i < start + pageSize; i++) {

            // 决定当前卡片使用单列还是双列布局
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

            String id = "id_" + i;

            // 从 ContentRepository 随机取一条内容
            int randomIndex = (int) (Math.random() * totalCount);
            ContentEntry content = ContentRepository.getEntry(randomIndex);

            boolean inFirstFive = (i < 5);

            // 前 5 个卡片：如果抽到视频，就重抽，直到抽到非视频
            if (inFirstFive) {
                int safety = totalCount; // 最多尝试 totalCount 次，防止极端死循环
                while (content.type == ContentEntry.TYPE_VIDEO && safety > 0) {
                    randomIndex = (int) (Math.random() * totalCount);
                    content = ContentRepository.getEntry(randomIndex);
                    safety--;
                }
            }

            // 控制视频节奏：至少隔 N 张卡才允许再次出视频
            boolean canShowVideoByInterval = cardsSinceLastVideo >= nextVideoInterval;
            boolean canShowVideoNow = !inFirstFive && canShowVideoByInterval;

            switch (content.type) {
                case ContentEntry.TYPE_VIDEO:
                    if (canShowVideoNow) {
                        list.add(new FeedItem(
                                id,
                                layout,
                                content.title,
                                content.description,
                                content.imageUrl,
                                content.imageRes,
                                content.videoUrl
                        ));
                        cardsSinceLastVideo = 0;
                        nextVideoInterval = getRandomVideoInterval();
                    } else {
                        // 节奏不允许视频：这里降级为图文卡处理
                        if (content.imageUrl != null) {
                            list.add(new FeedItem(
                                    id,
                                    layout,
                                    content.title,
                                    content.description,
                                    content.imageUrl
                            ));
                        } else {
                            list.add(new FeedItem(
                                    id,
                                    layout,
                                    content.title,
                                    content.description,
                                    content.imageRes
                            ));
                        }
                        cardsSinceLastVideo++;
                    }
                    break;

                case ContentEntry.TYPE_TEXT:
                    list.add(new FeedItem(
                            id,
                            layout,
                            content.title,
                            content.description
                    ));
                    cardsSinceLastVideo++;
                    break;

                case ContentEntry.TYPE_IMAGE:
                default:
                    if (content.imageUrl != null) {
                        list.add(new FeedItem(
                                id,
                                layout,
                                content.title,
                                content.description,
                                content.imageUrl
                        ));
                    } else {
                        list.add(new FeedItem(
                                id,
                                layout,
                                content.title,
                                content.description,
                                content.imageRes
                        ));
                    }
                    cardsSinceLastVideo++;
                    break;
            }
        }

        return list;
    }

    /**
     * 返回一个 [8, 10] 之间的随机整数，用于控制多少张卡片后允许出现下一个视频。
     */
    private int getRandomVideoInterval() {
        int range = MAX_CARDS_BEFORE_VIDEO - MIN_CARDS_BEFORE_VIDEO + 1; // 3
        return MIN_CARDS_BEFORE_VIDEO + (int) (Math.random() * range);
    }
}
