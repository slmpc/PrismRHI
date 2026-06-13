package com.github.slmpc.prismrhi.command;

import com.github.slmpc.prismrhi.resource.RhiResource;

public interface RhiCommandPool extends RhiResource {
    RhiCommandBuffer allocateCommandBuffer(RhiCommandBufferLevel level);
}
