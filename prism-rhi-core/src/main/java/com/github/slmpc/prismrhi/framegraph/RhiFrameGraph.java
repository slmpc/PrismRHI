package com.github.slmpc.prismrhi.framegraph;

import com.github.slmpc.prismrhi.barrier.RhiBufferBarrier;
import com.github.slmpc.prismrhi.barrier.RhiImageBarrier;
import com.github.slmpc.prismrhi.barrier.RhiPipelineBarrier;
import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiResource;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class RhiFrameGraph {
    private final List<RhiFrameGraphPass> passes = new ArrayList<>();
    private final Map<RhiResource, RhiResourceState> initialStates = new IdentityHashMap<>();
    private final Map<RhiResource, RhiResourceState> currentStates = new IdentityHashMap<>();

    public static RhiFrameGraph create() {
        return new RhiFrameGraph();
    }

    public RhiFrameGraph resource(RhiImage image, RhiResourceState initialState) {
        register(image, initialState);
        return this;
    }

    public RhiFrameGraph resource(RhiBuffer buffer, RhiResourceState initialState) {
        register(buffer, initialState);
        return this;
    }

    public RhiFrameGraphPass addPass(String name) {
        RhiFrameGraphPass pass = new RhiFrameGraphPass(name);
        passes.add(pass);
        return pass;
    }

    public List<RhiFrameGraphPass> passes() {
        return List.copyOf(passes);
    }

    public void reset() {
        currentStates.clear();
        currentStates.putAll(initialStates);
    }

    public void execute(RhiCommandBuffer commandBuffer) {
        if (currentStates.isEmpty() && !initialStates.isEmpty()) {
            reset();
        }
        for (RhiFrameGraphPass pass : passes) {
            RhiPipelineBarrier barrier = barrierFor(pass);
            if (!barrier.isEmpty()) {
                commandBuffer.pipelineBarrier(barrier);
            }
            pass.recordInto(commandBuffer);
        }
    }

    public Map<RhiResource, RhiResourceState> snapshotStates() {
        return Map.copyOf(currentStates);
    }

    private void register(RhiResource resource, RhiResourceState state) {
        initialStates.put(resource, state == null ? RhiResourceState.UNDEFINED : state);
        currentStates.put(resource, state == null ? RhiResourceState.UNDEFINED : state);
    }

    private RhiPipelineBarrier barrierFor(RhiFrameGraphPass pass) {
        RhiPipelineBarrier.Builder builder = RhiPipelineBarrier.builder();
        for (RhiFrameGraphResourceAccess read : pass.reads()) {
            transition(builder, read);
        }
        for (RhiFrameGraphResourceAccess write : pass.writes()) {
            transition(builder, write);
        }
        return builder.build();
    }

    private void transition(RhiPipelineBarrier.Builder builder, RhiFrameGraphResourceAccess access) {
        RhiResourceState oldState = currentStates.getOrDefault(access.resource(), RhiResourceState.UNDEFINED);
        RhiResourceState newState = access.state();
        if (oldState == newState) {
            return;
        }
        if (access.resource() instanceof RhiImage image) {
            builder.image(new RhiImageBarrier(
                    image,
                    oldState,
                    newState,
                    access.imageAspects(),
                    access.baseMipLevel(),
                    access.levelCount(),
                    access.baseArrayLayer(),
                    access.layerCount()
            ));
        } else if (access.resource() instanceof RhiBuffer buffer) {
            builder.buffer(new RhiBufferBarrier(
                    buffer,
                    oldState,
                    newState,
                    access.bufferOffset(),
                    access.bufferSize()
            ));
        }
        currentStates.put(access.resource(), newState);
    }
}
