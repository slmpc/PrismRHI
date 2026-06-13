package com.github.slmpc.prismrhi.rendering;

import java.util.Objects;

public record RhiRect2D(RhiOffset2D offset, RhiExtent2D extent) {
    public RhiRect2D {
        offset = offset == null ? RhiOffset2D.ZERO : offset;
        extent = Objects.requireNonNull(extent, "extent");
    }

    public static RhiRect2D of(int width, int height) {
        return new RhiRect2D(RhiOffset2D.ZERO, new RhiExtent2D(width, height));
    }

    public static RhiRect2D of(int x, int y, int width, int height) {
        return new RhiRect2D(new RhiOffset2D(x, y), new RhiExtent2D(width, height));
    }
}
