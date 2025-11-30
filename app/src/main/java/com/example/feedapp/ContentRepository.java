package com.example.feedapp;

import java.util.ArrayList;import java.util.List;

/**
 * 内容仓库，负责提供预设的、包含文本、图片、视频三类资源的列表。
 */
public class ContentRepository {

    private static final List<ContentEntry> entries = new ArrayList<>();

    // 使用静态初始化块来填充我们的内容数据
    static {
        // --- 预设内容列表 ---

        // 视频
        entries.add(new ContentEntry(
                "大自然的鬼斧神工",
                "感受流水的力量和山川的壮丽。",
                "https://picsum.photos/id/1015/800/600", // 封面图
                "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4"  // 视频
        ));

        // 纯文本
        entries.add(new ContentEntry(
                "一条思考",
                "“重要的不是你所站的位置，而是你所朝的方向。”"
        ));

        // 图文
        entries.add(new ContentEntry(
                "可爱的小狗",
                "一只好奇的小狗歪着头，仿佛在倾听着什么。",
                "https://picsum.photos/id/237/800/600"
        ));

        // 视频
        entries.add(new ContentEntry(
                "城市的日与夜",
                "从白昼到黑夜，感受都市的呼吸。",
                "https://picsum.photos/id/1040/800/600", // 封面图
                "http://vfx.mtime.cn/Video/2019/03/18/mp4/190318231014076505.mp4"  // 视频
        ));

        // 图文
        entries.add(new ContentEntry(
                "咖啡时光",
                "一杯香醇的拿铁，一本好书，一个悠闲的下午。",
                "https://picsum.photos/id/30/800/600"
        ));

        // 纯文本
        entries.add(new ContentEntry(
                "生活小贴士",
                "尝试每天早上喝一杯温水，有助于开启新的一天。"
        ));

        // 视频
        entries.add(new ContentEntry(
                "大象之梦",
                "另一部充满想象力的开源动画电影。",
                "https://picsum.photos/id/1041/800/600", // 封面图
                "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"  // 视频
        ));

        // 您可以在这里继续添加任意多的、任意类型的内容
    }

    /**
     * 根据索引获取一条内容。
     * 使用取模运算(%)来实现无限循环获取，防止索引越界。
     * @param index 索引
     * @return 一条内容条目
     */
    public static ContentEntry getEntry(int index) {
        if (entries.isEmpty()) {
            // 提供一个默认值以防止列表为空时崩溃
            return new ContentEntry("列表为空", "请在ContentRepository中添加内容。");
        }
        return entries.get(index % entries.size());
    }

    /**
     * 获取预设内容的总数。
     * @return 内容列表的大小
     */
    public static int getTotalCount() {
        return entries.size();
    }
}
