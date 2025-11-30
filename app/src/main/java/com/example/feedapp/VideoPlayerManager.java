package com.example.feedapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

// 【核心修改】在类声明的上方添加此注解
@OptIn(markerClass = UnstableApi.class)
public class VideoPlayerManager {
    private static VideoPlayerManager instance;
    private ExoPlayer player;

    // 1. 直接持有一个全局唯一的 PlayerView 实例
    private PlayerView playerView;

    private FeedAdapter.VideoVH currentViewHolder;

    private VideoPlayerManager(Context context) {
        player = new ExoPlayer.Builder(context).build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);

        // 2. 在这里创建全局唯一的 PlayerView，而不是在 ViewHolder 中
        playerView = new PlayerView(context);
        playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
    }

    // ... (getInstance, play, stop, release 方法保持不变)
    public static synchronized VideoPlayerManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoPlayerManager(context.getApplicationContext());
        }
        return instance;
    }

    public void play(FeedAdapter.VideoVH vh, String videoUrl) {
        if (vh == null || vh == currentViewHolder) {
            return;
        }

        stop(); // 停止上一个播放

        currentViewHolder = vh;

        // 3. 将全局的 PlayerView 动态地“挂载”到新的 ViewHolder 上
        if (playerView.getParent() != null) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }
        vh.playerContainer.addView(playerView, 0);

        // 绑定播放器并开始播放
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    if(currentViewHolder == vh) { // 确保回调是针对当前ViewHolder的
                        vh.ivCover.setVisibility(View.INVISIBLE);
                        vh.ivPlayButton.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    public void stop() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
            player.clearVideoSurface(); // 及时清除视频表面
        }
        if (currentViewHolder != null) {
            currentViewHolder.ivCover.setVisibility(View.VISIBLE);
            currentViewHolder.ivPlayButton.setVisibility(View.VISIBLE);
        }
        // 4. 从旧的 ViewHolder 上“卸载”全局的 PlayerView
        if (playerView != null && playerView.getParent() != null) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
        }

        currentViewHolder = null;
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        playerView = null; // 释放 playerView
        instance = null;
    }
}
