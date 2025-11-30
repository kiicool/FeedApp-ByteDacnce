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
        entries.add(new ContentEntry("秋日山景", "晴朗天空之下，连片的群山向远方延伸，壮丽无比。", "https://picsum.photos/id/1015/800/600"));
        entries.add(new ContentEntry("果色果香", "饱满的树莓，红得发亮，仿佛一颗颗诱人的红宝石。", "https://picsum.photos/id/102/800/600"));
        entries.add(new ContentEntry("静谧湖泊", "秋日的午后，在柔软的草地上放松自己，享受阳光。", "https://picsum.photos/id/103/800/600"));
        entries.add(new ContentEntry("可爱的小狗", "一只好奇的小狗歪着头，仿佛在倾听着什么。", "https://picsum.photos/id/237/800/600"));
        entries.add(new ContentEntry("建筑之美", "现代建筑的几何线条在蓝天下勾勒出独特的轮廓。", "https://picsum.photos/id/1040/800/600"));
        entries.add(new ContentEntry("咖啡时光", "一杯香醇的拿铁，一本好书，一个悠闲的下午。", "https://picsum.photos/id/30/800/600"));
        entries.add(new ContentEntry("雪山之巅", "从高空俯瞰，茂密的树林环抱着宝石蓝的湖水。", "https://picsum.photos/id/10/800/600"));
        entries.add(new ContentEntry("穿越森林", "一条小径蜿蜒穿过茂密的森林，引人探索。", "https://picsum.photos/id/106/800/600"));
        entries.add(new ContentEntry("古老的灯塔", "海边的灯塔静静矗立，指引着远方的航船。", "https://picsum.photos/id/108/800/600"));
        entries.add(new ContentEntry("工作空间", "整洁的桌面，一台笔记本，开启新一天的工作。", "https://picsum.photos/id/2/800/600"));
        // 新增条目
        entries.add(new ContentEntry("热烈绽放", "鲜艳的红色花朵在阳光下尽情绽放，充满生命力。", "https://picsum.photos/id/1080/800/600"));
        // 视频 (使用本地 drawable 图片作为封面)
        entries.add(new ContentEntry(
                "丝之鸽",
                "大战钟道兽。",
                R.drawable.silk ,  // 【核心修改】使用本地图片的资源ID
                "android.resource://" + "com.example.feedapp" + "/" + R.raw.sample // 本地视频
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
