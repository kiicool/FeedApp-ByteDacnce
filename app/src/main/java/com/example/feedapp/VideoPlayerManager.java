package com.example.feedapp;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;

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

    private static VideoPlayerManager instance;
    private final ExoPlayer player;
    private final PlayerView playerView;

    private FeedAdapter.VideoVH currentViewHolder;

    private VideoPlayerManager(Context context) {
        Context appCtx = context.getApplicationContext();

        // 1. 创建播放器
        player = new ExoPlayer.Builder(appCtx).build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);

        // 2. 创建全局唯一的 PlayerView
        playerView = new PlayerView(appCtx);
        playerView.setUseController(false);
        // 用 FIT，既不裁切也不变形
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setPlayer(player);

        // 3. 媒体【只设置 & prepare 一次】
        Uri uri = RawResourceDataSource.buildRawResourceUri(R.raw.sample);
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(false); // 初始不自动播

        // 4. 监听一次播放状态，READY 再隐藏封面
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY && currentViewHolder != null) {
                    currentViewHolder.ivCover.setVisibility(android.view.View.INVISIBLE);
                    currentViewHolder.ivPlayButton.setVisibility(android.view.View.INVISIBLE);
                }
            }
        });
    }

    public static synchronized VideoPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoPlayerManager(context);
        }
        return instance;
    }

    /**
     * 播放本地 raw/sample.mp4，忽略 videoUrl。
     */
    public void play(FeedAdapter.VideoVH vh, String videoUrl) {
        if (vh == null) return;

        // 如果同一个 ViewHolder 已经在播，直接忽略
        if (vh == currentViewHolder && player.isPlaying()) {
            return;
        }

        // 停掉上一个 ViewHolder 的 UI 状态
        if (currentViewHolder != null) {
            currentViewHolder.ivCover.setVisibility(android.view.View.VISIBLE);
            currentViewHolder.ivPlayButton.setVisibility(android.view.View.VISIBLE);
        }

        currentViewHolder = vh;

        // 初始先显示封面和播放按钮，避免一开始看到黑屏
        vh.ivCover.setVisibility(android.view.View.VISIBLE);
        vh.ivPlayButton.setVisibility(android.view.View.VISIBLE);

        // 把全局 PlayerView 挂到当前卡片上
        if (playerView.getParent() != null) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }
        vh.playerContainer.addView(playerView, 0);

        // 从头开始播，避免接着上一次的进度
        player.seekTo(0);
        player.setPlayWhenReady(true);
    }

    /**
     * 暂停当前播放，并解绑视图，但不销毁播放器和媒体。
     */
    public void stop() {
        // 暂停播放
        player.setPlayWhenReady(false);

        if (currentViewHolder != null) {
            currentViewHolder.ivCover.setVisibility(android.view.View.VISIBLE);
            currentViewHolder.ivPlayButton.setVisibility(android.view.View.VISIBLE);
        }

        // 从旧的 ViewHolder 上卸载 PlayerView
        if (playerView.getParent() != null) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }

        currentViewHolder = null;
    }

    public void release() {
        player.release();
        instance = null;
    }
}
