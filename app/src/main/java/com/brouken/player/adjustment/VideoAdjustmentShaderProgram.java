package com.brouken.player.adjustment;

import android.opengl.GLES20;

import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.GlProgram;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.Size;
import androidx.media3.effect.BaseGlShaderProgram;

/**
 * Single-pass GLES fragment shader. Uniforms are refreshed every frame from
 * {@link VideoAdjustmentState} so sliders are live without pipeline rebuilds.
 */
final class VideoAdjustmentShaderProgram extends BaseGlShaderProgram {

    private static final String VERTEX = ""
            + "attribute vec4 aFramePosition;\n"
            + "uniform mat4 uTransformationMatrix;\n"
            + "uniform mat4 uTexTransformationMatrix;\n"
            + "varying vec2 vTexSamplingCoord;\n"
            + "void main() {\n"
            + "  gl_Position = uTransformationMatrix * aFramePosition;\n"
            + "  vec4 texturePosition = vec4(aFramePosition.x * 0.5 + 0.5,\n"
            + "      aFramePosition.y * 0.5 + 0.5, 0.0, 1.0);\n"
            + "  vTexSamplingCoord = (uTexTransformationMatrix * texturePosition).xy;\n"
            + "}\n";

    private static final String FRAGMENT = ""
            + "precision mediump float;\n"
            + "uniform sampler2D uTexSampler;\n"
            + "uniform float uEnabled;\n"
            + "uniform float uBrightness;\n"
            + "uniform float uContrast;\n"
            + "uniform float uSaturation;\n"
            + "uniform float uHue;\n"
            + "uniform float uTemperature;\n"
            + "uniform float uTint;\n"
            + "uniform float uGamma;\n"
            + "uniform float uHighlights;\n"
            + "uniform float uShadows;\n"
            + "uniform float uBlackLevel;\n"
            + "uniform float uSharpness;\n"
            + "uniform float uVignette;\n"
            + "uniform vec2 uTexelSize;\n"
            + "varying vec2 vTexSamplingCoord;\n"
            + "const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);\n"
            + "vec3 applyHue(vec3 color, float angle) {\n"
            + "  float c = cos(angle);\n"
            + "  float s = sin(angle);\n"
            + "  mat3 rot = mat3(\n"
            + "    0.299+0.701*c+0.168*s, 0.299-0.299*c-0.328*s, 0.299-0.300*c+1.250*s,\n"
            + "    0.587-0.587*c+0.330*s, 0.587+0.413*c+0.035*s, 0.587-0.588*c-1.050*s,\n"
            + "    0.114-0.114*c-0.497*s, 0.114-0.114*c+0.292*s, 0.114+0.886*c-0.203*s);\n"
            + "  return clamp(rot * color, 0.0, 1.0);\n"
            + "}\n"
            + "void main() {\n"
            + "  vec4 src = texture2D(uTexSampler, vTexSamplingCoord);\n"
            + "  if (uEnabled < 0.5) {\n"
            + "    gl_FragColor = src;\n"
            + "    return;\n"
            + "  }\n"
            + "  vec3 color = src.rgb;\n"
            + "  float black = uBlackLevel * 0.25;\n"
            + "  color = (color - vec3(black)) / max(1.0 - black, 0.001);\n"
            + "  color += vec3(uBrightness * 0.5);\n"
            + "  float cf = (1.0 + uContrast) / max(1.0001 - uContrast, 0.0001);\n"
            + "  color = (color - 0.5) * cf + 0.5;\n"
            + "  float g = max(uGamma, 0.01);\n"
            + "  color = pow(max(color, 0.0), vec3(1.0 / g));\n"
            + "  float lum = dot(color, LUMA);\n"
            + "  float hiMask = smoothstep(0.45, 0.95, lum);\n"
            + "  float shMask = 1.0 - smoothstep(0.05, 0.50, lum);\n"
            + "  color += vec3(uHighlights * 0.35 * hiMask);\n"
            + "  color += vec3(uShadows * 0.35 * shMask);\n"
            + "  float sat = uSaturation + 1.0;\n"
            + "  color = mix(vec3(dot(color, LUMA)), color, sat);\n"
            + "  if (abs(uHue) > 0.0001) {\n"
            + "    color = applyHue(color, uHue);\n"
            + "  }\n"
            + "  color.r += uTemperature * 0.18;\n"
            + "  color.b -= uTemperature * 0.18;\n"
            + "  color.g += uTint * 0.14;\n"
            + "  color.r -= uTint * 0.07;\n"
            + "  color.b -= uTint * 0.07;\n"
            + "  if (uSharpness > 0.001) {\n"
            + "    vec2 t = uTexelSize;\n"
            + "    vec3 blur = texture2D(uTexSampler, vTexSamplingCoord + vec2(-t.x, -t.y)).rgb;\n"
            + "    blur += texture2D(uTexSampler, vTexSamplingCoord + vec2(0.0, -t.y)).rgb;\n"
            + "    blur += texture2D(uTexSampler, vTexSamplingCoord + vec2(t.x, -t.y)).rgb;\n"
            + "    blur += texture2D(uTexSampler, vTexSamplingCoord + vec2(-t.x, 0.0)).rgb;\n"
            + "    blur += color;\n"
            + "    blur += texture2D(uTexSampler, vTexSamplingCoord + vec2(t.x, 0.0)).rgb;\n"
            + "    blur += texture2D(uTexSampler, vTexSamplingCoord + vec2(-t.x, t.y)).rgb;\n"
            + "    blur += texture2D(uTexSampler, vTexSamplingCoord + vec2(0.0, t.y)).rgb;\n"
            + "    blur += texture2D(uTexSampler, vTexSamplingCoord + vec2(t.x, t.y)).rgb;\n"
            + "    blur /= 9.0;\n"
            + "    float amount = uSharpness * 0.85;\n"
            + "    color = clamp(color + (color - blur) * amount, 0.0, 1.0);\n"
            + "  }\n"
            + "  if (uVignette > 0.001) {\n"
            + "    vec2 p = vTexSamplingCoord - 0.5;\n"
            + "    float d = length(p) * 1.41421356;\n"
            + "    float vig = smoothstep(0.35, 1.15, d);\n"
            + "    color *= 1.0 - vig * uVignette * 0.85;\n"
            + "  }\n"
            + "  gl_FragColor = vec4(clamp(color, 0.0, 1.0), src.a);\n"
            + "}\n";

    private final VideoAdjustmentState state;
    private final GlProgram glProgram;
    private int width = 1;
    private int height = 1;

    VideoAdjustmentShaderProgram(VideoAdjustmentState state, boolean useHdr)
            throws GlUtil.GlException {
        super(useHdr, 1);
        this.state = state;
        glProgram = new GlProgram(VERTEX, FRAGMENT);
        glProgram.setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
        float[] identity = GlUtil.create4x4IdentityMatrix();
        glProgram.setFloatsUniform("uTransformationMatrix", identity);
        glProgram.setFloatsUniform("uTexTransformationMatrix", identity);
    }

    @Override
    public Size configure(int inputWidth, int inputHeight) {
        width = Math.max(inputWidth, 1);
        height = Math.max(inputHeight, 1);
        return new Size(inputWidth, inputHeight);
    }

    @Override
    public void drawFrame(int inputTexId, long presentationTimeUs)
            throws VideoFrameProcessingException {
        try {
            glProgram.use();
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0);
            boolean apply = !state.shouldBypass();
            glProgram.setFloatUniform("uEnabled", apply ? 1f : 0f);
            // Map UI ranges into shader-friendly units.
            glProgram.setFloatUniform("uBrightness", state.brightness / 100f);
            glProgram.setFloatUniform("uContrast", state.contrast / 100f);
            glProgram.setFloatUniform("uSaturation", state.saturation / 100f);
            glProgram.setFloatUniform("uHue", (float) Math.toRadians(state.hue));
            glProgram.setFloatUniform("uTemperature", state.temperature / 100f);
            glProgram.setFloatUniform("uTint", state.tint / 100f);
            glProgram.setFloatUniform("uGamma", state.gamma);
            glProgram.setFloatUniform("uHighlights", state.highlights / 100f);
            glProgram.setFloatUniform("uShadows", state.shadows / 100f);
            glProgram.setFloatUniform("uBlackLevel", state.blackLevel / 100f);
            glProgram.setFloatUniform("uSharpness", state.sharpness / 100f);
            glProgram.setFloatUniform("uVignette", state.vignette / 100f);
            glProgram.setFloatsUniform("uTexelSize", new float[]{1f / width, 1f / height});
            glProgram.bindAttributesAndUniforms();
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        } catch (GlUtil.GlException e) {
            throw new VideoFrameProcessingException(e, presentationTimeUs);
        }
    }

    @Override
    public void release() throws VideoFrameProcessingException {
        super.release();
        try {
            glProgram.delete();
        } catch (GlUtil.GlException e) {
            throw new VideoFrameProcessingException(e);
        }
    }
}
