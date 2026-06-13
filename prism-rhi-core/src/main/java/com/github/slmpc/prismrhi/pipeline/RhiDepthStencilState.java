package com.github.slmpc.prismrhi.pipeline;

public record RhiDepthStencilState(
        boolean depthTestEnable,
        boolean depthWriteEnable,
        RhiCompareOp depthCompareOp,
        boolean stencilTestEnable
) {
    public RhiDepthStencilState {
        depthCompareOp = depthCompareOp == null ? RhiCompareOp.LESS_OR_EQUAL : depthCompareOp;
    }

    public static RhiDepthStencilState disabled() {
        return new RhiDepthStencilState(false, false, RhiCompareOp.ALWAYS, false);
    }

    public static RhiDepthStencilState depthTest() {
        return new RhiDepthStencilState(true, true, RhiCompareOp.LESS_OR_EQUAL, false);
    }
}
