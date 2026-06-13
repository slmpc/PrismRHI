package com.github.slmpc.prismrhi.barrier;

import com.github.slmpc.prismrhi.resource.RhiBuffer;

import java.util.Objects;

public record RhiBufferBarrier(
        RhiBuffer buffer,
        RhiResourceState oldState,
        RhiResourceState newState,
        long offset,
        long size
) {
    public RhiBufferBarrier {
        buffer = Objects.requireNonNull(buffer, "buffer");
        oldState = oldState == null ? RhiResourceState.UNDEFINED : oldState;
        newState = Objects.requireNonNull(newState, "newState");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
    }

    public static RhiBufferBarrier of(RhiBuffer buffer, RhiResourceState oldState, RhiResourceState newState) {
        return new RhiBufferBarrier(buffer, oldState, newState, 0, 0);
    }

    public boolean transitionNeeded() {
        return oldState != newState;
    }
}
