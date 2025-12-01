package com.example.feedapp;

/**
 * App中所有卡片的数据模型。
 * 它可以统一表示文本、图片（网络或本地）和视频（网络或本地封面）等多种类型。 */
public class FeedItem {

    // --- 卡片类型常量 ---
    public static final int CARD_TYPE_TEXT = 1;
    public static final int CARD_TYPE_IMAGE = 2;
    public static final int CARD_TYPE_VIDEO = 3;

    // --- 布局类型常量 ---
    public static final int LAYOUT_SINGLE_COLUMN = 1; // 占满一行
    public static final int LAYOUT_DOUBLE_COLUMN = 2; // 占一半

    // --- 成员变量 ---
    public final String id;
    public final int cardType;
    public final int layoutType;
    public final String title;
    public final String description;

    // 图片资源：网络URL和本地ID，通常只有一个有值
    public final String imageUrl;
    public final int imageRes;

    // 视频资源
    public final String videoUrl;

    // --- 构造函数 ---

    /**
     * 构造函数 1: 用于纯文本卡片
     * @param id         唯一ID
     * @param layoutType 布局类型
     * @param title      标题
     * @param description 描述
     */
    public FeedItem(String id, int layoutType, String title, String description) {
        this.id = id;
        this.cardType = CARD_TYPE_TEXT;
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageUrl = null;
        this.imageRes = 0;
        this.videoUrl = null;
    }

    /**
     * 构造函数 2: 用于图文卡片 (加载网络图片)
     * @param id          唯一ID
     * @param layoutType  布局类型
     * @param title       标题
     * @param description  描述
     * @param imageUrl    网络图片URL
     */
    public FeedItem(String id, int layoutType, String title, String description, String imageUrl) {
        this.id = id;
        this.cardType = CARD_TYPE_IMAGE;
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageRes = 0;
        this.videoUrl = null;
    }

    /**
     * 构造函数 3: 用于图文卡片 (加载本地Drawable资源)
     * @param id          唯一ID
     * @param layoutType  布局类型
     * @param title       标题
     * @param description  描述
     * @param imageRes    本地图片资源ID (例如, R.drawable.my_image)
     */
    public FeedItem(String id, int layoutType, String title, String description, int imageRes) {
        this.id = id;
        this.cardType = CARD_TYPE_IMAGE;
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageUrl = null;
        this.imageRes = imageRes;
        this.videoUrl = null;
    }

    /**
     * 【核心新增】构造函数 4: 用于视频卡片 (可同时处理网络封面和本地封面)
     * @param id          唯一ID
     * @param layoutType  布局类型
     * @param title       标题
     * @param description  描述
     * @param imageUrl    网络封面图URL (如果使用本地封面，则传 null)
     * @param imageRes    本地封面图资源ID (如果使用网络封面，则传 0)
     * @param videoUrl    视频URL
     */
    public FeedItem(String id, int layoutType, String title, String description, String imageUrl, int imageRes, String videoUrl) {
        this.id = id;
        this.cardType = CARD_TYPE_VIDEO;
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageRes = imageRes;
        this.videoUrl = videoUrl;
    }
}
