package com.github.slmpc.prismrhi.rendering;

public record RhiExtent2D(int width, int height) {
    public RhiExtent2D {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("extent dimensions must be positive");
        }
    }
}
