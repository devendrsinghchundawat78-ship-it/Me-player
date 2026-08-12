package com.brouken.player.adjustment;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class VideoAdjustmentRepository {

    private static final String K_ENABLED = "pictureAdj_enabled";
    private static final String K_PRESET = "pictureAdj_preset";
    private static final String K_BRIGHTNESS = "pictureAdj_brightness";
    private static final String K_CONTRAST = "pictureAdj_contrast";
    private static final String K_SATURATION = "pictureAdj_saturation";
    private static final String K_HUE = "pictureAdj_hue";
    private static final String K_TEMP = "pictureAdj_temperature";
    private static final String K_TINT = "pictureAdj_tint";
    private static final String K_GAMMA = "pictureAdj_gamma";
    private static final String K_HIGHLIGHTS = "pictureAdj_highlights";
    private static final String K_SHADOWS = "pictureAdj_shadows";
    private static final String K_BLACK = "pictureAdj_blackLevel";
    private static final String K_SHARP = "pictureAdj_sharpness";
    private static final String K_VIGNETTE = "pictureAdj_vignette";

    private final SharedPreferences prefs;

    public VideoAdjustmentRepository(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public void load(VideoAdjustmentState state) {
        state.enabled = prefs.getBoolean(K_ENABLED, true);
        state.presetId = prefs.getString(K_PRESET, VideoAdjustmentPreset.ORIGINAL.id);
        state.brightness = prefs.getFloat(K_BRIGHTNESS, 0f);
        state.contrast = prefs.getFloat(K_CONTRAST, 0f);
        state.saturation = prefs.getFloat(K_SATURATION, 0f);
        state.hue = prefs.getFloat(K_HUE, 0f);
        state.temperature = prefs.getFloat(K_TEMP, 0f);
        state.tint = prefs.getFloat(K_TINT, 0f);
        state.gamma = prefs.getFloat(K_GAMMA, 1f);
        state.highlights = prefs.getFloat(K_HIGHLIGHTS, 0f);
        state.shadows = prefs.getFloat(K_SHADOWS, 0f);
        state.blackLevel = prefs.getFloat(K_BLACK, 0f);
        state.sharpness = prefs.getFloat(K_SHARP, 0f);
        state.vignette = prefs.getFloat(K_VIGNETTE, 0f);
    }

    public void save(VideoAdjustmentState state) {
        prefs.edit()
                .putBoolean(K_ENABLED, state.enabled)
                .putString(K_PRESET, state.presetId)
                .putFloat(K_BRIGHTNESS, state.brightness)
                .putFloat(K_CONTRAST, state.contrast)
                .putFloat(K_SATURATION, state.saturation)
                .putFloat(K_HUE, state.hue)
                .putFloat(K_TEMP, state.temperature)
                .putFloat(K_TINT, state.tint)
                .putFloat(K_GAMMA, state.gamma)
                .putFloat(K_HIGHLIGHTS, state.highlights)
                .putFloat(K_SHADOWS, state.shadows)
                .putFloat(K_BLACK, state.blackLevel)
                .putFloat(K_SHARP, state.sharpness)
                .putFloat(K_VIGNETTE, state.vignette)
                .apply();
    }
}
