package com.example.feedapp.feed.data;


import com.example.feedapp.feed.model.ContentEntry;
import com.example.feedapp.feed.model.FeedItem;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

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
    private int cardsSinceLastAd = 0;
    private int nextAdInterval = 10;

    private int getRandomAdInterval() {
        // 每 8~15 张内容插一条广告
        return 8 + (int) (Math.random() * 8); // [8, 15]
    }
    /**
     * 在下拉刷新时调用，以便从头开始生成布局和视频节奏。
     */
    public void reset() {
        this.isSingleColumnMode = true;
        this.doubleColumnCount = 0;
        this.cardsSinceLastVideo = 0;
        this.nextVideoInterval = getRandomVideoInterval();
    }

    public List<FeedItem> generatePageData(int start, int pageSize) {
        List<FeedItem> list = new ArrayList<>();

        // 当前这一页里已经用过哪些内容，避免同一页出现相同图片/文案
        Set<Integer> usedIndicesInPage = new HashSet<>();



        // 行布局权重：2/3/4/5 行
        int[] rowWeights = {5, 4, 3, 2};
        int totalWeight = 0;
        for (int w : rowWeights) {
            totalWeight += w;
        }

        int totalCount = ContentRepository.getTotalCount();

        for (int i = start; i < start + pageSize; i++) {

            //先决定当前卡片是单列还是双列
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

            // 只在“应该是单列”的位置考虑插广告
            boolean shouldInsertAd =
                    (layout == FeedItem.LAYOUT_SINGLE_COLUMN)   // 当前行是单列
                            && (cardsSinceLastAd >= nextAdInterval); // 达到广告间隔

            if (shouldInsertAd) {
                String adId = "banner_" + start + "_" + i;
                list.add(new FeedItem(
                        adId,
                        FeedItem.CARD_TYPE_BANNER,
                        FeedItem.LAYOUT_SINGLE_COLUMN,
                        "猜你喜欢 · 精选推荐",
                        "基于你的浏览习惯推荐一些内容",
                        "https://picsum.photos/seed/" + adId + "/800/400",
                        0,
                        null
                ));

                // 重置广告节奏
                cardsSinceLastAd = 0;
                nextAdInterval = getRandomAdInterval();

                // 这一轮已经填了一个完整单列行，就不再生成普通内容了
                continue;
            }

            //正常内容推送逻辑

            // 是否属于“第一页的前 5 张卡片”
            boolean inFirstFive = (start == 0 && i < 5);

            boolean canShowVideoByInterval = (cardsSinceLastVideo >= nextVideoInterval);
            boolean allowVideoNow = !inFirstFive && canShowVideoByInterval;

            int randomIndex = -1;
            ContentEntry content = null;

            int safety = totalCount * 2;
            while (safety-- > 0) {
                int candidateIndex = (int) (Math.random() * totalCount);

                if (usedIndicesInPage.contains(candidateIndex)) {
                    continue;
                }

                ContentEntry candidate = ContentRepository.getEntry(candidateIndex);

                // 不允许视频时，跳过 TYPE_VIDEO
                if (!allowVideoNow && candidate.type == ContentEntry.TYPE_VIDEO) {
                    continue;
                }

                randomIndex = candidateIndex;
                content = candidate;
                usedIndicesInPage.add(candidateIndex);
                break;
            }

            if (content == null) {
                randomIndex = (int) (Math.random() * totalCount);
                content = ContentRepository.getEntry(randomIndex);
            }

            switch (content.type) {
                case ContentEntry.TYPE_VIDEO:
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

            cardsSinceLastAd++;
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
