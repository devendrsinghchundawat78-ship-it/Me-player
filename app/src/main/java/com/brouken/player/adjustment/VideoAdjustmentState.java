package com.brouken.player.adjustment;

/**
 * Live picture-adjustment parameters. The GPU shader reads these every frame;
 * sliders only mutate this object (no player / surface recreation).
 */
public final class VideoAdjustmentState {

    public volatile boolean enabled = true;
    public volatile boolean compareHold;
    public volatile String presetId = VideoAdjustmentPreset.ORIGINAL.id;

    public volatile float brightness;
    public volatile float contrast;
    public volatile float saturation;
    public volatile float hue;
    public volatile float temperature;
    public volatile float tint;
    public volatile float gamma = 1f;
    public volatile float highlights;
    public volatile float shadows;
    public volatile float blackLevel;
    public volatile float sharpness;
    public volatile float vignette;

    public boolean isIdentity() {
        return brightness == 0f && contrast == 0f && saturation == 0f && hue == 0f
                && temperature == 0f && tint == 0f && Math.abs(gamma - 1f) < 0.001f
                && highlights == 0f && shadows == 0f && blackLevel == 0f
                && sharpness == 0f && vignette == 0f;
    }

    public boolean shouldBypass() {
        return !enabled || compareHold || isIdentity();
    }

    public void resetAll() {
        applyPreset(VideoAdjustmentPreset.ORIGINAL);
    }

    public void applyPreset(VideoAdjustmentPreset preset) {
        presetId = preset.id;
        brightness = preset.brightness;
        contrast = preset.contrast;
        saturation = preset.saturation;
        hue = preset.hue;
        temperature = preset.temperature;
        tint = preset.tint;
        gamma = preset.gamma;
        highlights = preset.highlights;
        shadows = preset.shadows;
        blackLevel = preset.blackLevel;
        sharpness = preset.sharpness;
        vignette = preset.vignette;
    }

    public VideoAdjustmentState copy() {
        VideoAdjustmentState c = new VideoAdjustmentState();
        c.enabled = enabled;
        c.presetId = presetId;
        c.brightness = brightness;
        c.contrast = contrast;
        c.saturation = saturation;
        c.hue = hue;
        c.temperature = temperature;
        c.tint = tint;
        c.gamma = gamma;
        c.highlights = highlights;
        c.shadows = shadows;
        c.blackLevel = blackLevel;
        c.sharpness = sharpness;
        c.vignette = vignette;
        return c;
    }
}
