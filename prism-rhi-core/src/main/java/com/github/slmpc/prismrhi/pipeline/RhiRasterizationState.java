package com.github.slmpc.prismrhi.pipeline;

public record RhiRasterizationState(
        RhiPolygonMode polygonMode,
        RhiCullMode cullMode,
        RhiFrontFace frontFace,
        float lineWidth
) {
    public RhiRasterizationState {
        polygonMode = polygonMode == null ? RhiPolygonMode.FILL : polygonMode;
        cullMode = cullMode == null ? RhiCullMode.BACK : cullMode;
        frontFace = frontFace == null ? RhiFrontFace.COUNTER_CLOCKWISE : frontFace;
        lineWidth = lineWidth <= 0.0f ? 1.0f : lineWidth;
    }

    public static RhiRasterizationState defaults() {
        return new RhiRasterizationState(RhiPolygonMode.FILL, RhiCullMode.BACK, RhiFrontFace.COUNTER_CLOCKWISE, 1.0f);
    }
}
