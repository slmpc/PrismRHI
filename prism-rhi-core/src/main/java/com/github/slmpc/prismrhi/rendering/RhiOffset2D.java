package com.github.slmpc.prismrhi.rendering;

public record RhiOffset2D(int x, int y) {
    public static final RhiOffset2D ZERO = new RhiOffset2D(0, 0);
}
