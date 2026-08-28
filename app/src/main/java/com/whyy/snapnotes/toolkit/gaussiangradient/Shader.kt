package com.whyy.snapnotes.toolkit.gaussiangradient

import org.intellij.lang.annotations.Language

@Language("AGSL")
const val GRADIENT_SHADER = """
uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;
uniform float2 tintRange;

float verticalMask(float y, float2 range) {
    float p = clamp(y / max(size.y, 1.0), 0.0, 1.0);
    return smoothstep(range.x, range.y, p);
}

half4 main(float2 coord) {
    float tintAlpha = verticalMask(coord.y, tintRange);
    float alpha = clamp(tintIntensity * tintAlpha, 0.0, 1.0);
    return half4(tint.rgb, alpha);
}
"""
