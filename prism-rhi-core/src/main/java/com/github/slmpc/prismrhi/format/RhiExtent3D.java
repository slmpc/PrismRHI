package com.github.slmpc.prismrhi.format;

public record RhiExtent3D(int width, int height, int depth) {
    public RhiExtent3D {
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("extent dimensions must be positive");
        }
    }

    public static RhiExtent3D of2D(int width, int height) {
        return new RhiExtent3D(width, height, 1);
    }
}
