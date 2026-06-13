package com.github.slmpc.prismrhi.queue;

public record RhiQueueRequest(RhiQueueType type, int count) {
    public RhiQueueRequest {
        type = type == null ? RhiQueueType.GRAPHICS : type;
        if (count < 1) {
            throw new IllegalArgumentException("queue count must be at least 1");
        }
    }
}
