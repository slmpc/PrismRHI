package com.github.slmpc.prismrhi.rendering;

public record RhiClearValue(float r, float g, float b, float a, float depth, int stencil) {
    public static RhiClearValue color(float r, float g, float b, float a) {
        return new RhiClearValue(r, g, b, a, 1.0f, 0);
    }

    public static RhiClearValue depth(float depth) {
        return new RhiClearValue(0.0f, 0.0f, 0.0f, 0.0f, depth, 0);
    }

    public static RhiClearValue depthStencil(float depth, int stencil) {
        return new RhiClearValue(0.0f, 0.0f, 0.0f, 0.0f, depth, stencil);
    }
}
