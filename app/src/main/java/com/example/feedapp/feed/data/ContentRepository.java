package com.example.feedapp.feed.data;

import com.example.feedapp.R;
import com.example.feedapp.feed.model.ContentEntry;
import java.util.ArrayList;
import java.util.List;
/**
 * 内容仓库：负责提供预设的文本 / 图片 / 视频资源。
 */
public class ContentRepository {

    private static final List<ContentEntry> entries = new ArrayList<>();

    static {
        // ---- 图文类 ----
        entries.add(new ContentEntry(
                "秋日山景",
                "晴朗天空之下，连片的群山向远方延伸，空气里有一点冷冽。",
                "https://picsum.photos/id/1015/800/600"
        ));

        entries.add(new ContentEntry(
                "果色果香",
                "饱满的树莓，红得发亮，像一盘端上桌的宝石。",
                "https://picsum.photos/id/102/800/600"
        ));

        entries.add(new ContentEntry(
                "静谧湖泊",
                "湖面像一面镜子，只要轻轻一丢石子，所有宁静都会泛起涟漪。",
                "https://picsum.photos/id/103/800/600"
        ));

        entries.add(new ContentEntry(
                "可爱的小狗",
                "一只好奇的小狗歪着头，仿佛在认真听你讲话。",
                "https://picsum.photos/id/237/800/600"
        ));

        entries.add(new ContentEntry(
                "建筑之美",
                "利落的几何线条，把城市的边界切得干干净净。",
                "https://picsum.photos/id/1040/800/600"
        ));

        entries.add(new ContentEntry(
                "咖啡时光",
                "一杯咖啡、一块桌面，就足够装下一整天的计划。",
                "https://picsum.photos/id/30/800/600"
        ));

        entries.add(new ContentEntry(
                "雪山之巅",
                "风从雪线上吹下来时，连呼吸都变得格外清醒。",
                "https://picsum.photos/id/10/800/600"
        ));

        entries.add(new ContentEntry(
                "穿越森林",
                "一条小径把森林划开了一道缝，也给了人走进去的借口。",
                "https://picsum.photos/id/106/800/600"
        ));

        entries.add(new ContentEntry(
                "古老的灯塔",
                "夜里的灯塔不说话，只负责把光打到远处去。",
                "https://picsum.photos/id/108/800/600"
        ));

        entries.add(new ContentEntry(
                "工作空间",
                "桌面干净的时候，大脑也会跟着利落一点。",
                "https://picsum.photos/id/2/800/600"
        ));
        entries.add(new ContentEntry(
                "晨曦小鹿",
                "清晨的薄雾轻轻笼罩在山谷中，小鹿悄悄撕开雾气。",
                "https://picsum.photos/id/1003/800/600"
        ));

        entries.add(new ContentEntry(
                "湖心秘境",
                "靛蓝色湖水下，反射着昏黄的光，那是森林之心。",
                "https://picsum.photos/id/1011/800/600"
        ));

        entries.add(new ContentEntry(
                "黄岩",
                "云朵像被风推着，在蓝天下缓慢移动，给黄岩打上阴霾。",
                "https://picsum.photos/id/1016/800/600"
        ));

        entries.add(new ContentEntry(
                "城市边缘",
                "小狗在青草间跳跃，城市从不真正入睡，但偶尔也很安静。",
                "https://picsum.photos/id/1012/800/600"
        ));

        entries.add(new ContentEntry(
                "午后阳光",
                "阳光穿过窗帘缝隙，打在木地板上，简单却无比治愈。",
                "https://picsum.photos/id/104/800/600"
        ));

        entries.add(new ContentEntry(
                "旅行背包",
                "一个装满故事的背包，一张未折痕的地图，一个刚刚好的好天气。",
                "https://picsum.photos/id/250/800/600"
        ));

        entries.add(new ContentEntry(
                "温暖餐桌",
                "热气腾腾的饭菜、叮当作响的碗筷，家的味道永远是最真实的安慰。",
                "https://picsum.photos/id/292/800/600"
        ));

        entries.add(new ContentEntry(
                "森林清风",
                "风穿过铁道时，会把树叶卷成一场小小的舞蹈表演。",
                "https://picsum.photos/id/233/800/600"
        ));

        entries.add(new ContentEntry(
                "黄昏小镇",
                "太阳落在地平线那刻，小镇的影子都被拉得细长而温柔。",
                "https://picsum.photos/id/863/800/600"
        ));

        entries.add(new ContentEntry(
                "热烈绽放",
                "花开的时候不会问值得不值得，只管用力盛开一次。",
                "https://picsum.photos/id/1080/800/600"
        ));

        // ---- 视频类（本地视频 + 本地封面）----
        entries.add(new ContentEntry(
                "丝之鸽",
                "大战钟道兽，今天的手柄已经准备好了吗？",
                R.drawable.silk,
                "android.resource://" + "com.example.feedapp" + "/" + R.raw.sample
        ));

        entries.add(new ContentEntry(
                "以撒的结合",
                "一场看似胡乱奔跑的逃亡，背后都是精算过的选择。",
                R.drawable.issac,
                "android.resource://" + "com.example.feedapp" + "/" + R.raw.sample2
        ));

        // ---- 纯文本 ----
        entries.add(new ContentEntry(
                "一条思考",
                "重要的不是你所在的位置，而是你面对的方向。"
        ));

        entries.add(new ContentEntry(
                "生活小贴士",
                "如果今天过得很糟，可以先把明天要做的一件小事写下来。"
        ));

        entries.add(new ContentEntry(
                "关于节奏",
                "刷信息流的时候，也可以偶尔停一下，看看窗外的天气。"
        ));

        entries.add(new ContentEntry(
                "关于学习",
                "真正让人焦虑的不是学不会，而是明明知道要学却迟迟没有动手。"
        ));

        entries.add(new ContentEntry(
                "关于休息",
                "休息不是浪费时间，而是为了不在关键节点掉链子。"
        ));

        entries.add(new ContentEntry(
                "一句废话",
                "今天也可以只完成 60 分，但至少比昨天的 0 分要好。"
        ));
    }

    public static ContentEntry getEntry(int index) {
        if (entries.isEmpty()) {
            return new ContentEntry("列表为空", "请在 ContentRepository 中添加内容。");
        }
        return entries.get(index % entries.size());
    }

    public static int getTotalCount() {
        return entries.size();
    }
}
