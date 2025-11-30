package com.example.feedapp;

/**
 * 内容模型，用于在资源库中存放预设的图文、纯文本或视频内容。
 */
public class ContentEntry {
    // 定义内容类型常量，方便区分
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_TEXT = 2;
    public static final int TYPE_VIDEO = 3;

    public final int type;
    public final String title;
    public final String description;
    public final String imageUrl;    // 对于图文和视频，这是图片/封面图的URL
    public final String videoUrl;    // 只有视频类型才有值

    // --- 构造函数 ---

    /**
     * 图文内容的构造函数
     * @param title 标题
     * @param description 描述
     * @param imageUrl 图片URL
     */
    public ContentEntry(String title, String description, String imageUrl) {
        this.type = TYPE_IMAGE;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.videoUrl = null; // 图文内容没有视频URL
    }

    /**
     * 纯文本内容的构造函数
     * @param title 标题
     * @param description 描述
     */
    public ContentEntry(String title, String description) {
        this.type = TYPE_TEXT;
        this.title = title;
        this.description = description;
        this.imageUrl = null;  // 纯文本没有图片URL
        this.videoUrl = null;  // 纯文本没有视频URL
    }

    /**
     * 视频内容的构造函数
     * @param title 标题
     * @param description 描述
     * @param imageUrl 封面图URL
     * @param videoUrl 视频播放URL
     */
    public ContentEntry(String title, String description, String imageUrl, String videoUrl) {
        this.type = TYPE_VIDEO;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
    }
}
