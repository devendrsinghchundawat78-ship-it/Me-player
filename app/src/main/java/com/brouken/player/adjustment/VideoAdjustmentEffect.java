package com.brouken.player.adjustment;

import android.content.Context;

import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.effect.GlEffect;
import androidx.media3.effect.GlShaderProgram;

public final class VideoAdjustmentEffect implements GlEffect {

    private final VideoAdjustmentState state;

    public VideoAdjustmentEffect(VideoAdjustmentState state) {
        this.state = state;
    }

    @Override
    public GlShaderProgram toGlShaderProgram(Context context, boolean useHdr)
            throws VideoFrameProcessingException {
        try {
            return new VideoAdjustmentShaderProgram(state, useHdr);
        } catch (Exception e) {
            throw VideoFrameProcessingException.from(e);
        }
    }

    @Override
    public boolean isNoOp(int inputWidth, int inputHeight) {
        // Keep the program attached so uniforms can update without restarting playback.
        return false;
    }
}
