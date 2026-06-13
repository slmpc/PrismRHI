package com.github.slmpc.prismrhi.barrier;

import java.util.ArrayList;
import java.util.List;

public record RhiPipelineBarrier(
        List<RhiImageBarrier> imageBarriers,
        List<RhiBufferBarrier> bufferBarriers
) {
    public RhiPipelineBarrier {
        imageBarriers = List.copyOf(imageBarriers == null ? List.of() : imageBarriers);
        bufferBarriers = List.copyOf(bufferBarriers == null ? List.of() : bufferBarriers);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return imageBarriers.isEmpty() && bufferBarriers.isEmpty();
    }

    public static final class Builder {
        private final List<RhiImageBarrier> imageBarriers = new ArrayList<>();
        private final List<RhiBufferBarrier> bufferBarriers = new ArrayList<>();

        public Builder image(RhiImageBarrier barrier) {
            if (barrier != null && barrier.transitionNeeded()) {
                imageBarriers.add(barrier);
            }
            return this;
        }

        public Builder buffer(RhiBufferBarrier barrier) {
            if (barrier != null && barrier.transitionNeeded()) {
                bufferBarriers.add(barrier);
            }
            return this;
        }

        public RhiPipelineBarrier build() {
            return new RhiPipelineBarrier(imageBarriers, bufferBarriers);
        }
    }
}
