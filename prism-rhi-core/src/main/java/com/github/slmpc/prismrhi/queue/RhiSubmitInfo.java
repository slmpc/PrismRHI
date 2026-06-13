package com.github.slmpc.prismrhi.queue;

import com.github.slmpc.prismrhi.command.RhiCommandBuffer;

import java.util.List;

public record RhiSubmitInfo(List<RhiCommandBuffer> commandBuffers) {
    public RhiSubmitInfo {
        commandBuffers = List.copyOf(commandBuffers == null ? List.of() : commandBuffers);
    }

    public static RhiSubmitInfo of(RhiCommandBuffer commandBuffer) {
        return new RhiSubmitInfo(List.of(commandBuffer));
    }
}
