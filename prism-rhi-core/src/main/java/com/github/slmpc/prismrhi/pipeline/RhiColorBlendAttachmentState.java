package com.github.slmpc.prismrhi.pipeline;

import java.util.EnumSet;
import java.util.Set;

public record RhiColorBlendAttachmentState(
        boolean blendEnable,
        RhiBlendFactor srcColorBlendFactor,
        RhiBlendFactor dstColorBlendFactor,
        RhiBlendOp colorBlendOp,
        RhiBlendFactor srcAlphaBlendFactor,
        RhiBlendFactor dstAlphaBlendFactor,
        RhiBlendOp alphaBlendOp,
        Set<RhiColorWriteMask> colorWriteMask
) {
    public RhiColorBlendAttachmentState {
        srcColorBlendFactor = srcColorBlendFactor == null ? RhiBlendFactor.ONE : srcColorBlendFactor;
        dstColorBlendFactor = dstColorBlendFactor == null ? RhiBlendFactor.ZERO : dstColorBlendFactor;
        colorBlendOp = colorBlendOp == null ? RhiBlendOp.ADD : colorBlendOp;
        srcAlphaBlendFactor = srcAlphaBlendFactor == null ? RhiBlendFactor.ONE : srcAlphaBlendFactor;
        dstAlphaBlendFactor = dstAlphaBlendFactor == null ? RhiBlendFactor.ZERO : dstAlphaBlendFactor;
        alphaBlendOp = alphaBlendOp == null ? RhiBlendOp.ADD : alphaBlendOp;
        colorWriteMask = Set.copyOf(colorWriteMask == null || colorWriteMask.isEmpty()
                ? EnumSet.allOf(RhiColorWriteMask.class)
                : colorWriteMask);
    }

    public static RhiColorBlendAttachmentState opaque() {
        return new RhiColorBlendAttachmentState(
                false,
                RhiBlendFactor.ONE,
                RhiBlendFactor.ZERO,
                RhiBlendOp.ADD,
                RhiBlendFactor.ONE,
                RhiBlendFactor.ZERO,
                RhiBlendOp.ADD,
                EnumSet.allOf(RhiColorWriteMask.class)
        );
    }

    public static RhiColorBlendAttachmentState alphaBlend() {
        return new RhiColorBlendAttachmentState(
                true,
                RhiBlendFactor.SRC_ALPHA,
                RhiBlendFactor.ONE_MINUS_SRC_ALPHA,
                RhiBlendOp.ADD,
                RhiBlendFactor.ONE,
                RhiBlendFactor.ONE_MINUS_SRC_ALPHA,
                RhiBlendOp.ADD,
                EnumSet.allOf(RhiColorWriteMask.class)
        );
    }
}
