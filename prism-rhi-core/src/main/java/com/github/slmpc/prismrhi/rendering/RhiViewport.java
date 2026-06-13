package com.github.slmpc.prismrhi.rendering;

public record RhiViewport(float x, float y, float width, float height, float minDepth, float maxDepth) {
    public RhiViewport {
        if (width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("viewport dimensions must not be negative");
        }
    }

    public static RhiViewport of(float width, float height) {
        return new RhiViewport(0.0f, 0.0f, width, height, 0.0f, 1.0f);
    }

    public static RhiViewport of(float x, float y, float width, float height) {
        return new RhiViewport(x, y, width, height, 0.0f, 1.0f);
    }
}
