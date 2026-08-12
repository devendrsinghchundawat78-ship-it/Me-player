package com.brouken.player.adjustment;

import android.content.Context;

import androidx.media3.exoplayer.ExoPlayer;

import java.util.Collections;

/**
 * Owns adjustment state and attaches a single live GPU effect to ExoPlayer.
 * Slider updates never recreate the player, activity, or surface.
 */
public final class VideoAdjustmentController {

    private final VideoAdjustmentState state = new VideoAdjustmentState();
    private final VideoAdjustmentRepository repository;
    private boolean effectAttached;
    private boolean attachFailed;

    public VideoAdjustmentController(Context context) {
        repository = new VideoAdjustmentRepository(context);
        repository.load(state);
    }

    public VideoAdjustmentState getState() {
        return state;
    }

    public void persist() {
        repository.save(state);
    }

    public void attachToPlayer(ExoPlayer player, boolean tunneling) {
        effectAttached = false;
        if (player == null || tunneling || attachFailed) {
            return;
        }
        try {
            player.setVideoEffects(Collections.singletonList(new VideoAdjustmentEffect(state)));
            effectAttached = true;
        } catch (Exception e) {
            attachFailed = true;
            e.printStackTrace();
        }
    }

    public boolean isEffectAttached() {
        return effectAttached;
    }

    public void markCustom() {
        state.presetId = "custom";
    }
}
