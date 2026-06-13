package com.github.slmpc.prismrhi.pipeline;

public record RhiVertexInputBinding(int binding, int stride, RhiVertexInputRate inputRate) {
    public RhiVertexInputBinding {
        if (binding < 0) {
            throw new IllegalArgumentException("binding must not be negative");
        }
        if (stride < 0) {
            throw new IllegalArgumentException("stride must not be negative");
        }
        inputRate = inputRate == null ? RhiVertexInputRate.VERTEX : inputRate;
    }
}
