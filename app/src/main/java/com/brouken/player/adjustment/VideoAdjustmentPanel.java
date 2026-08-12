package com.brouken.player.adjustment;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.brouken.player.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public final class VideoAdjustmentPanel {

    public interface Host {
        void keepControlsVisible();
    }

    private final Context context;
    private final VideoAdjustmentController controller;
    private final Host host;
    private BottomSheetDialog dialog;

    public VideoAdjustmentPanel(Context context, VideoAdjustmentController controller, Host host) {
        this.context = context;
        this.controller = controller;
        this.host = host;
    }

    public void show() {
        if (dialog != null && dialog.isShowing()) {
            return;
        }
        dialog = new BottomSheetDialog(context, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        View root = LayoutInflater.from(context).inflate(R.layout.picture_adjustment_panel, null);
        dialog.setContentView(root);

        VideoAdjustmentState state = controller.getState();

        SwitchMaterial enable = root.findViewById(R.id.picture_enable);
        enable.setChecked(state.enabled);
        enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            state.enabled = isChecked;
            controller.persist();
            host.keepControlsVisible();
        });

        ChipGroup presets = root.findViewById(R.id.picture_presets);
        LinearLayout sliders = root.findViewById(R.id.picture_sliders);

        SliderRow brightness = addSlider(sliders, R.string.picture_brightness, -100, 100, 0,
                Math.round(state.brightness), v -> { state.brightness = v; });
        SliderRow contrast = addSlider(sliders, R.string.picture_contrast, -100, 100, 0,
                Math.round(state.contrast), v -> { state.contrast = v; });
        SliderRow saturation = addSlider(sliders, R.string.picture_saturation, -100, 100, 0,
                Math.round(state.saturation), v -> { state.saturation = v; });
        SliderRow hue = addSlider(sliders, R.string.picture_hue, -180, 180, 0,
                Math.round(state.hue), v -> { state.hue = v; });
        SliderRow temperature = addSlider(sliders, R.string.picture_temperature, -100, 100, 0,
                Math.round(state.temperature), v -> { state.temperature = v; });
        SliderRow tint = addSlider(sliders, R.string.picture_tint, -100, 100, 0,
                Math.round(state.tint), v -> { state.tint = v; });
        SliderRow gamma = addSlider(sliders, R.string.picture_gamma, 50, 200, 100,
                Math.round(state.gamma * 100f), v -> { state.gamma = v / 100f; });
        SliderRow highlights = addSlider(sliders, R.string.picture_highlights, -100, 100, 0,
                Math.round(state.highlights), v -> { state.highlights = v; });
        SliderRow shadows = addSlider(sliders, R.string.picture_shadows, -100, 100, 0,
                Math.round(state.shadows), v -> { state.shadows = v; });
        SliderRow blacks = addSlider(sliders, R.string.picture_blacks, -100, 100, 0,
                Math.round(state.blackLevel), v -> { state.blackLevel = v; });
        SliderRow sharpness = addSlider(sliders, R.string.picture_sharpness, 0, 100, 0,
                Math.round(state.sharpness), v -> { state.sharpness = v; });
        SliderRow vignette = addSlider(sliders, R.string.picture_vignette, 0, 100, 0,
                Math.round(state.vignette), v -> { state.vignette = v; });

        Runnable refreshFromState = () -> {
            brightness.setProgress(Math.round(state.brightness));
            contrast.setProgress(Math.round(state.contrast));
            saturation.setProgress(Math.round(state.saturation));
            hue.setProgress(Math.round(state.hue));
            temperature.setProgress(Math.round(state.temperature));
            tint.setProgress(Math.round(state.tint));
            gamma.setProgress(Math.round(state.gamma * 100f));
            highlights.setProgress(Math.round(state.highlights));
            shadows.setProgress(Math.round(state.shadows));
            blacks.setProgress(Math.round(state.blackLevel));
            sharpness.setProgress(Math.round(state.sharpness));
            vignette.setProgress(Math.round(state.vignette));
        };

        for (VideoAdjustmentPreset preset : VideoAdjustmentPreset.ALL) {
            Chip chip = new Chip(context);
            chip.setText(preset.titleRes);
            chip.setCheckable(true);
            chip.setChecked(preset.id.equals(state.presetId));
            chip.setContentDescription(context.getString(preset.titleRes));
            chip.setOnClickListener(v -> {
                state.applyPreset(preset);
                refreshFromState.run();
                controller.persist();
                host.keepControlsVisible();
            });
            presets.addView(chip);
        }

        MaterialButton compare = root.findViewById(R.id.picture_compare);
        compare.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    state.compareHold = true;
                    v.setPressed(true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    state.compareHold = false;
                    v.setPressed(false);
                    return true;
                default:
                    return false;
            }
        });

        root.findViewById(R.id.picture_reset).setOnClickListener(v -> {
            state.resetAll();
            refreshFromState.run();
            for (int i = 0; i < presets.getChildCount(); i++) {
                View child = presets.getChildAt(i);
                if (child instanceof Chip) {
                    ((Chip) child).setChecked(i == 0);
                }
            }
            controller.persist();
            host.keepControlsVisible();
        });

        dialog.setOnDismissListener(d -> {
            state.compareHold = false;
            controller.persist();
        });

        dialog.setOnShowListener(d -> {
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                boolean landscape = context.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE;
                if (landscape) {
                    ViewGroup.LayoutParams lp = sheet.getLayoutParams();
                    lp.width = Math.min(dp(400), context.getResources().getDisplayMetrics().widthPixels / 2);
                    sheet.setLayoutParams(lp);
                }
            }
        });

        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private SliderRow addSlider(ViewGroup parent, int labelRes, int min, int max, int reset,
                                int current, ValueSink sink) {
        View row = LayoutInflater.from(context).inflate(R.layout.picture_adjustment_row, parent, false);
        TextView label = row.findViewById(R.id.row_label);
        TextView value = row.findViewById(R.id.row_value);
        SeekBar seek = row.findViewById(R.id.row_seek);
        ImageButton resetBtn = row.findViewById(R.id.row_reset);
        label.setText(labelRes);
        seek.setMax(max - min);
        seek.setProgress(current - min);
        value.setText(formatValue(labelRes, current, min == 50));
        seek.setContentDescription(context.getString(labelRes));

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int mapped = progress + min;
                sink.accept(mapped);
                value.setText(formatValue(labelRes, mapped, min == 50));
                if (fromUser) {
                    controller.markCustom();
                }
                host.keepControlsVisible();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                host.keepControlsVisible();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                controller.persist();
            }
        });
        resetBtn.setOnClickListener(v -> {
            seek.setProgress(reset - min);
            sink.accept(reset);
            controller.markCustom();
            controller.persist();
        });
        parent.addView(row);
        return new SliderRow(seek, min);
    }

    private String formatValue(int labelRes, int mapped, boolean gammaScale) {
        if (gammaScale) {
            return String.format(java.util.Locale.US, "%.2f", mapped / 100f);
        }
        if (labelRes == R.string.picture_hue) {
            return mapped + "°";
        }
        if (mapped > 0) {
            return "+" + mapped;
        }
        return String.valueOf(mapped);
    }

    private static final class SliderRow {
        final SeekBar seek;
        final int min;

        SliderRow(SeekBar seek, int min) {
            this.seek = seek;
            this.min = min;
        }

        void setProgress(int mapped) {
            seek.setProgress(mapped - min);
        }
    }

    private interface ValueSink {
        void accept(int value);
    }
}
