package com.github.slmpc.prismrhi.barrier;

public enum RhiResourceState {
    UNDEFINED,
    PRESENT,
    TRANSFER_SRC,
    TRANSFER_DST,
    SAMPLED_IMAGE,
    STORAGE_IMAGE_READ,
    STORAGE_IMAGE_WRITE,
    COLOR_ATTACHMENT,
    DEPTH_STENCIL_ATTACHMENT,
    DEPTH_STENCIL_READ,
    UNIFORM_BUFFER,
    STORAGE_BUFFER_READ,
    STORAGE_BUFFER_WRITE,
    VERTEX_BUFFER,
    INDEX_BUFFER,
    INDIRECT_BUFFER;

    public boolean writes() {
        return switch (this) {
            case TRANSFER_DST,
                    STORAGE_IMAGE_WRITE,
                    COLOR_ATTACHMENT,
                    DEPTH_STENCIL_ATTACHMENT,
                    STORAGE_BUFFER_WRITE -> true;
            case UNDEFINED,
                    PRESENT,
                    TRANSFER_SRC,
                    SAMPLED_IMAGE,
                    STORAGE_IMAGE_READ,
                    DEPTH_STENCIL_READ,
                    UNIFORM_BUFFER,
                    STORAGE_BUFFER_READ,
                    VERTEX_BUFFER,
                    INDEX_BUFFER,
                    INDIRECT_BUFFER -> false;
        };
    }
}
