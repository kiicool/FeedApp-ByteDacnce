package com.example.feedapp;

/**
 * App 中所有卡片的数据模型。
 * 支持：文本 / 图片（网络或本地）/ 视频（网络或本地封面）/ 未来任意扩展卡片。
 */
public class FeedItem {

    // --- 卡片类型常量 ---
    public static final int CARD_TYPE_TEXT = 1;
    public static final int CARD_TYPE_IMAGE = 2;
    public static final int CARD_TYPE_VIDEO = 3;
    // 如需扩展 Banner 等类型，外部可自行定义：
    public static final int CARD_TYPE_BANNER = 4;


    // --- 布局类型常量 ---
    public static final int LAYOUT_SINGLE_COLUMN = 1; // 占满一行
    public static final int LAYOUT_DOUBLE_COLUMN = 2; // 占一半

    // --- 成员变量 ---
    public final String id;
    public final int cardType;
    public final int layoutType;
    public final String title;
    public final String description;

    // 图片资源：网络 URL 或本地资源 ID（只会用一个）
    public final String imageUrl;
    public final int imageRes;

    // 视频资源
    public final String videoUrl;

    // 通用构造函数 — 插件体系核心依赖
    public FeedItem(String id,
                    int cardType,
                    int layoutType,
                    String title,
                    String description,
                    String imageUrl,
                    int imageRes,
                    String videoUrl) {

        this.id = id;
        this.cardType = cardType;
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageRes = imageRes;
        this.videoUrl = videoUrl;
    }

    // 文本卡片

    public FeedItem(String id, int layoutType, String title, String description) {
        this(id,
                CARD_TYPE_TEXT,
                layoutType,
                title,
                description,
                null,   // imageUrl
                0,      // imageRes
                null    // videoUrl
        );
    }

    // 图片卡片（网络图片）

    public FeedItem(String id, int layoutType, String title, String description, String imageUrl) {
        this(id,
                CARD_TYPE_IMAGE,
                layoutType,
                title,
                description,
                imageUrl,   // 网络图
                0,
                null);
    }

    // 【旧构造函数 3】图片卡片（本地图片）
    public FeedItem(String id, int layoutType, String title, String description, int imageRes) {
        this(id,
                CARD_TYPE_IMAGE,
                layoutType,
                title,
                description,
                null,       // 无网络图
                imageRes,   // 本地图
                null);
    }

    // 【旧构造函数 4】视频卡片 —— 兼容旧代码
    public FeedItem(String id,
                    int layoutType,
                    String title,
                    String description,
                    String imageUrl,
                    int imageRes,
                    String videoUrl) {
        this(id,
                CARD_TYPE_VIDEO,
                layoutType,
                title,
                description,
                imageUrl,
                imageRes,
                videoUrl);
    }

}
