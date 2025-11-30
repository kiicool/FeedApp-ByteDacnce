package com.example.feedapp;
public class FeedItem {


    public static final int CARD_TYPE_TEXT = 1;
    public static final int CARD_TYPE_IMAGE = 2;
    public static final int CARD_TYPE_VIDEO = 3; // 视频卡片类型


    public static final int LAYOUT_SINGLE_COLUMN = 1; // 占两列
    public static final int LAYOUT_DOUBLE_COLUMN = 2; // 占一列

    public String id;
    public int cardType;
    public int layoutType;
    public String title;
    public String description;
    public String imageUrl;   // 对于视频，这是封面图URL
    public int imageRes;       // 本地图片资源 id（0 表示没有）
    public final String videoUrl; // 2. 【新增】视频URL字段


    // 网络图片/文本构造函数 (保持不变)
    public FeedItem(String id,
                    int cardType,
                    int layoutType,
                    String title,
                    String description,
                    String imageUrl) {
        this.id = id;
        this.cardType = cardType;
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageRes = 0;
        this.videoUrl = null; // 非视频卡片，videoUrl为null
    }

    // 本地图片构造函数 (保持不变)
    public FeedItem(String id,
                    int cardType,
                    int layoutType,
                    String title,
                    String description,
                    int imageRes) {
        this.id = id;
        this.cardType = cardType;
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageRes = imageRes;
        this.imageUrl = null;
        this.videoUrl = null; // 非视频卡片，videoUrl为null
    }

    // 3. 【新增】视频卡片的构造函数
    public FeedItem(String id,
                    int layoutType,
                    String title,
                    String description,
                    String imageUrl,  // 视频的封面图
                    String videoUrl) {
        this.id = id;
        this.cardType = CARD_TYPE_VIDEO; // 类型固定为视频
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;    // 封面图URL
        this.videoUrl = videoUrl;    // 视频URL
        this.imageRes = 0;
    }

}
