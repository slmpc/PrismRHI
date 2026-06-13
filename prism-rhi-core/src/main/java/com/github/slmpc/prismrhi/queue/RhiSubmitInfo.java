package com.github.slmpc.prismrhi.queue;

import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.sync.RhiPipelineStage;
import com.github.slmpc.prismrhi.sync.RhiSemaphore;

import java.util.ArrayList;
import java.util.List;

public record RhiSubmitInfo(
        List<RhiCommandBuffer> commandBuffers,
        List<RhiSemaphore> waitSemaphores,
        List<RhiPipelineStage> waitStages,
        List<RhiSemaphore> signalSemaphores
) {
    public RhiSubmitInfo {
        commandBuffers = List.copyOf(commandBuffers == null ? List.of() : commandBuffers);
        waitSemaphores = List.copyOf(waitSemaphores == null ? List.of() : waitSemaphores);
        waitStages = List.copyOf(waitStages == null ? List.of() : waitStages);
        signalSemaphores = List.copyOf(signalSemaphores == null ? List.of() : signalSemaphores);
        if (!waitStages.isEmpty() && waitStages.size() != waitSemaphores.size()) {
            throw new IllegalArgumentException("waitStages must be empty or match waitSemaphores");
        }
    }

    public RhiSubmitInfo(List<RhiCommandBuffer> commandBuffers) {
        this(commandBuffers, List.of(), List.of(), List.of());
    }

    public static RhiSubmitInfo of(RhiCommandBuffer commandBuffer) {
        return new RhiSubmitInfo(List.of(commandBuffer));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<RhiCommandBuffer> commandBuffers = new ArrayList<>();
        private final List<RhiSemaphore> waitSemaphores = new ArrayList<>();
        private final List<RhiPipelineStage> waitStages = new ArrayList<>();
        private final List<RhiSemaphore> signalSemaphores = new ArrayList<>();

        public Builder commandBuffer(RhiCommandBuffer commandBuffer) {
            commandBuffers.add(commandBuffer);
            return this;
        }

        public Builder wait(RhiSemaphore semaphore, RhiPipelineStage stage) {
            waitSemaphores.add(semaphore);
            waitStages.add(stage == null ? RhiPipelineStage.ALL_COMMANDS : stage);
            return this;
        }

        public Builder signal(RhiSemaphore semaphore) {
            signalSemaphores.add(semaphore);
            return this;
        }

        public RhiSubmitInfo build() {
            return new RhiSubmitInfo(commandBuffers, waitSemaphores, waitStages, signalSemaphores);
        }
    }
}
