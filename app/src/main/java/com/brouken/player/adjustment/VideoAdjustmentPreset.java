package com.brouken.player.adjustment;

public final class VideoAdjustmentPreset {

    public final String id;
    public final int titleRes;
    public final float brightness;
    public final float contrast;
    public final float saturation;
    public final float hue;
    public final float temperature;
    public final float tint;
    public final float gamma;
    public final float highlights;
    public final float shadows;
    public final float blackLevel;
    public final float sharpness;
    public final float vignette;

    public VideoAdjustmentPreset(String id, int titleRes,
                                 float brightness, float contrast, float saturation, float hue,
                                 float temperature, float tint, float gamma,
                                 float highlights, float shadows, float blackLevel,
                                 float sharpness, float vignette) {
        this.id = id;
        this.titleRes = titleRes;
        this.brightness = brightness;
        this.contrast = contrast;
        this.saturation = saturation;
        this.hue = hue;
        this.temperature = temperature;
        this.tint = tint;
        this.gamma = gamma;
        this.highlights = highlights;
        this.shadows = shadows;
        this.blackLevel = blackLevel;
        this.sharpness = sharpness;
        this.vignette = vignette;
    }

    public static final VideoAdjustmentPreset ORIGINAL = new VideoAdjustmentPreset(
            "original", com.brouken.player.R.string.picture_preset_original,
            0, 0, 0, 0, 0, 0, 1f, 0, 0, 0, 0, 0);

    public static final VideoAdjustmentPreset CINEMA = new VideoAdjustmentPreset(
            "cinema", com.brouken.player.R.string.picture_preset_cinema,
            -8, 12, -6, 0, 8, 0, 1.05f, -10, 8, 6, 8, 18);

    public static final VideoAdjustmentPreset VIVID = new VideoAdjustmentPreset(
            "vivid", com.brouken.player.R.string.picture_preset_vivid,
            4, 22, 28, 0, 4, 0, 0.95f, 8, 4, 0, 18, 0);

    public static final VideoAdjustmentPreset WARM = new VideoAdjustmentPreset(
            "warm", com.brouken.player.R.string.picture_preset_warm,
            2, 4, 6, 0, 35, 4, 1f, 0, 0, 0, 0, 0);

    public static final VideoAdjustmentPreset COOL = new VideoAdjustmentPreset(
            "cool", com.brouken.player.R.string.picture_preset_cool,
            2, 4, 4, 0, -35, -4, 1f, 0, 0, 0, 0, 0);

    public static final VideoAdjustmentPreset NIGHT = new VideoAdjustmentPreset(
            "night", com.brouken.player.R.string.picture_preset_night,
            -22, 10, -8, 0, -6, 0, 1.12f, -16, 12, 8, 0, 12);

    public static final VideoAdjustmentPreset NATURAL = new VideoAdjustmentPreset(
            "natural", com.brouken.player.R.string.picture_preset_natural,
            2, 6, 8, 0, 6, 2, 0.98f, 4, 4, 0, 6, 0);

    public static final VideoAdjustmentPreset[] ALL = {
            ORIGINAL, CINEMA, VIVID, WARM, COOL, NIGHT, NATURAL
    };

    public static VideoAdjustmentPreset fromId(String id) {
        if (id == null) {
            return ORIGINAL;
        }
        for (VideoAdjustmentPreset p : ALL) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return ORIGINAL;
    }
}
