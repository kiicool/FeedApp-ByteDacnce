package com.example.feedapp;

public class FeedItem {

    public static final int CARD_TYPE_TEXT = 1;
    public static final int CARD_TYPE_IMAGE = 2;

    public static final int LAYOUT_SINGLE_COLUMN = 1; // 占两列
    public static final int LAYOUT_DOUBLE_COLUMN = 2; // 占一列

    public String id;
    public int cardType;
    public int layoutType;
    public String title;
    public String description;
    public String imageUrl;

    public FeedItem(String id, int cardType, int layoutType,
                    String title, String description, String imageUrl) {
        this.id = id;
        this.cardType = cardType;
        this.layoutType = layoutType;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
    }
}
