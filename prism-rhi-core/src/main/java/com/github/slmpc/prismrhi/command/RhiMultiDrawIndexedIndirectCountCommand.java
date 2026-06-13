package com.github.slmpc.prismrhi.command;

import com.github.slmpc.prismrhi.resource.RhiBuffer;

import java.util.Objects;

public record RhiMultiDrawIndexedIndirectCountCommand(
        RhiBuffer buffer,
        long offset,
        RhiBuffer countBuffer,
        long countOffset,
        int maxDrawCount,
        int stride
) {
    public static final int PACKED_STRIDE = 0;

    public RhiMultiDrawIndexedIndirectCountCommand {
        buffer = Objects.requireNonNull(buffer, "buffer");
        countBuffer = Objects.requireNonNull(countBuffer, "countBuffer");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (countOffset < 0) {
            throw new IllegalArgumentException("countOffset must not be negative");
        }
        if (maxDrawCount < 0) {
            throw new IllegalArgumentException("maxDrawCount must not be negative");
        }
        if (stride < 0) {
            throw new IllegalArgumentException("stride must not be negative");
        }
    }
}
