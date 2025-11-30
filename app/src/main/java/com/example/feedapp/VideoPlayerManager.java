package com.example.feedapp;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;
import java.util.HashMap;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.RawResourceDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

@OptIn(markerClass = UnstableApi.class)
public class VideoPlayerManager {

    private static volatile VideoPlayerManager instance;

    private final ExoPlayer player;
    private final PlayerView playerView;

    // 当前绑定的 ViewHolder 和视频 url
    private FeedAdapter.VideoVH currentViewHolder;
    private String currentVideoUrl;
    // 用来区分每一条 FeedItem，例如用 item.id 转成字符串
    private String currentItemKey;
    // 记录每个 item 的上次播放进度
    private final Map<String, Long> positionStore = new HashMap<>();

    private VideoPlayerManager(Context context) {

        Context appCtx = context.getApplicationContext();
        player = new ExoPlayer.Builder(appCtx).build();

        playerView = new PlayerView(appCtx);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setUseController(true);
        // 让控制条不要自动消失，由用户控制显示/隐藏
        playerView.setControllerShowTimeoutMs(0);
        playerView.setPlayer(player);

        // 根据是否在播放，控制进度条显隐
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY && player.getPlayWhenReady() && currentViewHolder != null) {
                    currentViewHolder.ivCover.setVisibility(View.INVISIBLE);
                    currentViewHolder.ivPlayButton.setVisibility(View.INVISIBLE);
                    playerView.hideController();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    playerView.hideController();
                } else {
                    playerView.showController();
                }
            }
        });
    }

    public FeedAdapter.VideoVH getCurrentViewHolder() {
        return currentViewHolder;
    }

    public static VideoPlayerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (VideoPlayerManager.class) {
                if (instance == null) {
                    instance = new VideoPlayerManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 播放指定 ViewHolder 上的视频。
     * 同一个 videoUrl 再次调用时，不会重置进度，只会接续播放。
     */
    public void play(FeedAdapter.VideoVH vh, String itemKey, String videoUrl) {
        if (vh == null || videoUrl == null || itemKey == null) return;

        // 保存上一个 item 的进度
        if (currentItemKey != null) {
            long pos = player.getCurrentPosition();
            positionStore.put(currentItemKey, pos);
        }

        boolean sameUrl = videoUrl.equals(currentVideoUrl);
        boolean sameItem = itemKey.equals(currentItemKey);

        // 移动 PlayerView
        attachPlayerViewTo(vh);

        currentItemKey = itemKey;

        if (!sameUrl) {
            // 完全不同视频
            currentVideoUrl = videoUrl;

            MediaItem mediaItem;
            if (videoUrl.startsWith("rawresource://")) {
                try {
                    int resId = Integer.parseInt(
                            videoUrl.substring("rawresource://".length())
                    );
                    Uri uri = RawResourceDataSource.buildRawResourceUri(resId);
                    mediaItem = new MediaItem.Builder().setUri(uri).build();
                } catch (Exception e) {
                    mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
                }
            } else {
                mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
            }

            player.setMediaItem(mediaItem);
            player.prepare();
        }

        // 根据 itemKey 决定续播位置
        Long savedPos = positionStore.get(itemKey);
        if (savedPos != null) {
            player.seekTo(savedPos);
        } else {
            // new item
            player.seekTo(0);
        }

        // 播放
        player.setPlayWhenReady(true);

        vh.ivCover.setVisibility(View.INVISIBLE);
        vh.ivPlayButton.setVisibility(View.INVISIBLE);
    }

    /**
     * 把全局唯一的 PlayerView 移动到当前 ViewHolder 的容器。
     */
    private void attachPlayerViewTo(FeedAdapter.VideoVH vh) {
        // 从旧容器卸载
        if (playerView.getParent() != null) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }

        // 挂到新容器
        vh.playerContainer.removeAllViews();
        vh.playerContainer.addView(playerView,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));

        currentViewHolder = vh;
    }

    /**
     * 滚动时调用：只暂停，不清空进度。
     * 这样同一个 url 再次 play 时能接续播放。
     */
    public void stop() {
        // 先记一下当前位置
        if (currentItemKey != null) {
            long pos = player.getCurrentPosition();
            positionStore.put(currentItemKey, pos);
        }

        player.setPlayWhenReady(false);

        if (currentViewHolder != null) {
            currentViewHolder.ivCover.setVisibility(View.VISIBLE);
            currentViewHolder.ivPlayButton.setVisibility(View.VISIBLE);
        }
    }


    public void release() {
        currentVideoUrl = null;
        currentViewHolder = null;

        playerView.setPlayer(null);
        player.release();
        instance = null;
    }
}
