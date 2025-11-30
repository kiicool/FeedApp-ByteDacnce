package com.example.feedapp;

/**
 * 内容模型，可以同时支持网络资源和本地资源。
 */
public class ContentEntry {
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_TEXT = 2;
    public static final int TYPE_VIDEO = 3;

    public final int type;
    public final String title;
    public final String description;

    // 分别存储网络URL和本地资源ID
    public final String imageUrl;
    public final int imageRes; // 用于存储 R.drawable.xxx

    public final String videoUrl;

    // --- 构造函数 ---

    public ContentEntry(String title, String description, String imageUrl) {
        this.type = TYPE_IMAGE;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageRes = 0; // 0 表示没有本地图片资源
        this.videoUrl = null;
    }

    /**
     * 【新增】图文内容构造函数 (使用本地 drawable 图片)
     */
    public ContentEntry(String title, String description, int imageRes) {
        this.type = TYPE_IMAGE;
        this.title = title;
        this.description = description;
        this.imageUrl = null; // 没有网络图片URL
        this.imageRes = imageRes; // 使用本地资源ID
        this.videoUrl = null;
    }

    /**
     * 纯文本内容的构造函数
     */
    public ContentEntry(String title, String description) {
        this.type = TYPE_TEXT;
        this.title = title;
        this.description = description;
        this.imageUrl = null;
        this.imageRes = 0;
        this.videoUrl = null;
    }

    /**
     * 视频内容的构造函数 (使用网络封面图)
     */
    public ContentEntry(String title, String description, String imageUrl, String videoUrl) {
        this.type = TYPE_VIDEO;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.imageRes = 0;
        this.videoUrl = videoUrl;
    }

    /**
     * 【新增】视频内容的构造函数 (使用本地 drawable 封面图)
     */
    public ContentEntry(String title, String description, int imageRes, String videoUrl) {
        this.type = TYPE_VIDEO;
        this.title = title;
        this.description = description;
        this.imageUrl = null;
        this.imageRes = imageRes;
        this.videoUrl = videoUrl;
    }
}
