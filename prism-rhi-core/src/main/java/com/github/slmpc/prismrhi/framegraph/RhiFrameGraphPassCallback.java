package com.github.slmpc.prismrhi.framegraph;

import com.github.slmpc.prismrhi.command.RhiCommandBuffer;

@FunctionalInterface
public interface RhiFrameGraphPassCallback {
    void record(RhiCommandBuffer commandBuffer, RhiFrameGraphPass pass);
}
