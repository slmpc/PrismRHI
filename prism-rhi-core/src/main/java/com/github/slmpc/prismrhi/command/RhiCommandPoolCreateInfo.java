package com.github.slmpc.prismrhi.command;

import com.github.slmpc.prismrhi.queue.RhiQueueType;

public record RhiCommandPoolCreateInfo(RhiQueueType queueType, boolean transientPool, boolean resetCommandBuffer) {
    public RhiCommandPoolCreateInfo {
        queueType = queueType == null ? RhiQueueType.GRAPHICS : queueType;
    }
}
