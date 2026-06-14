package com.github.slmpc.prismrhi.format;

public enum RhiFormat {
    UNDEFINED,
    R8_UNORM,
    RG8_UNORM,
    RGB8_UNORM,
    RGBA8_UNORM,
    BGRA8_UNORM,
    RGBA8_SRGB,
    BGRA8_SRGB,
    R32_FLOAT,
    RG32_FLOAT,
    RGB32_FLOAT,
    RGBA16_FLOAT,
    RGBA32_FLOAT,
    D24_UNORM_S8_UINT,
    D32_FLOAT;

    public int bytesPerPixel() {
        return switch (this) {
            case R8_UNORM -> 1;
            case RG8_UNORM -> 2;
            case RGB8_UNORM -> 3;
            case RGBA8_UNORM, BGRA8_UNORM, RGBA8_SRGB, BGRA8_SRGB -> 4;
            case R32_FLOAT, D24_UNORM_S8_UINT, D32_FLOAT -> 4;
            case RG32_FLOAT, RGBA16_FLOAT -> 8;
            case RGB32_FLOAT -> 12;
            case RGBA32_FLOAT -> 16;
            case UNDEFINED -> throw new IllegalArgumentException("UNDEFINED does not have a texel size");
        };
    }

    public boolean hasDepth() {
        return this == D24_UNORM_S8_UINT || this == D32_FLOAT;
    }

    public boolean hasStencil() {
        return this == D24_UNORM_S8_UINT;
    }
}
