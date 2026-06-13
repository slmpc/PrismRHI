package com.github.slmpc.prismrhi.resource;

public record RhiSamplerCreateInfo(
        RhiFilter minFilter,
        RhiFilter magFilter,
        RhiSamplerAddressMode addressModeU,
        RhiSamplerAddressMode addressModeV,
        RhiSamplerAddressMode addressModeW,
        float maxAnisotropy
) {
    public RhiSamplerCreateInfo {
        minFilter = minFilter == null ? RhiFilter.LINEAR : minFilter;
        magFilter = magFilter == null ? RhiFilter.LINEAR : magFilter;
        addressModeU = addressModeU == null ? RhiSamplerAddressMode.REPEAT : addressModeU;
        addressModeV = addressModeV == null ? addressModeU : addressModeV;
        addressModeW = addressModeW == null ? addressModeU : addressModeW;
        if (maxAnisotropy < 0.0f) {
            throw new IllegalArgumentException("maxAnisotropy must not be negative");
        }
    }

    public static RhiSamplerCreateInfo linearRepeat() {
        return new RhiSamplerCreateInfo(
                RhiFilter.LINEAR,
                RhiFilter.LINEAR,
                RhiSamplerAddressMode.REPEAT,
                RhiSamplerAddressMode.REPEAT,
                RhiSamplerAddressMode.REPEAT,
                0.0f
        );
    }
}
