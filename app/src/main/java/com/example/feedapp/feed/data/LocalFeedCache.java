package com.example.feedapp.feed.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.feedapp.feed.model.FeedItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地缓存：把当前 Feed 列表序列化到 SharedPreferences，
 * 当“网络请求”失败时，从这里读出来展示。
 */
public class LocalFeedCache {

    private static final String PREF_NAME = "feed_cache";
    private static final String KEY_FEED_ITEMS = "key_feed_items_v1";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", "\\n");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\|", "|");
    }

    private static int safeParseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    // 保存当前列表到本地
    public static void save(Context context, List<FeedItem> items) {
        if (items == null || items.isEmpty()) {
            Log.d("LocalFeedCache", "save: empty list, skip");
            return;
        }
        Log.d("LocalFeedCache", "save: size=" + items.size());
        StringBuilder sb = new StringBuilder();
        for (FeedItem item : items) {
            sb.append(escape(item.id)).append('|')
                    .append(item.cardType).append('|')
                    .append(item.layoutType).append('|')
                    .append(escape(item.title)).append('|')
                    .append(escape(item.description)).append('|')
                    .append(escape(item.imageUrl)).append('|')
                    .append(item.imageRes).append('|')
                    .append(escape(item.videoUrl == null ? "" : item.videoUrl))
                    .append('\n');
        }
        prefs(context).edit().putString(KEY_FEED_ITEMS, sb.toString()).apply();
    }

    // 从本地读取缓存，如果没有就返回 null
    @Nullable
    public static List<FeedItem> load(Context context) {
        String data = prefs(context).getString(KEY_FEED_ITEMS, null);
        if (data == null || data.isEmpty()) return null;

        List<FeedItem> list = new ArrayList<>();
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\\|", -1);
            if (parts.length < 8) continue;

            String id = unescape(parts[0]);
            int cardType = safeParseInt(parts[1], FeedItem.CARD_TYPE_TEXT);
            int layoutType = safeParseInt(parts[2], FeedItem.LAYOUT_SINGLE_COLUMN);
            String title = unescape(parts[3]);
            String desc = unescape(parts[4]);
            String imageUrl = unescape(parts[5]);
            int imageRes = safeParseInt(parts[6], 0);
            String videoUrl = unescape(parts[7]);
            if (videoUrl.isEmpty()) videoUrl = null;

            FeedItem item;
            switch (cardType) {
                case FeedItem.CARD_TYPE_IMAGE:
                    if (imageRes != 0) {
                        // 本地图
                        item = new FeedItem(id, layoutType, title, desc, imageRes);
                    } else {
                        // 网络图
                        item = new FeedItem(id, layoutType, title, desc, imageUrl);
                    }
                    break;
                case FeedItem.CARD_TYPE_VIDEO:
                    item = new FeedItem(id, layoutType, title, desc, imageUrl, imageRes, videoUrl);
                    break;
                case FeedItem.CARD_TYPE_TEXT:
                default:
                    item = new FeedItem(id, layoutType, title, desc);
                    break;
            }
            list.add(item);
        }
        Log.d("LocalFeedCache", "load: parsed size=" + list.size());
        return list;
    }

}
