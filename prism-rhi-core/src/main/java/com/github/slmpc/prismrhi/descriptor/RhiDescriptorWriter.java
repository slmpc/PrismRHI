package com.github.slmpc.prismrhi.descriptor;

import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import com.github.slmpc.prismrhi.resource.RhiSampler;

import java.util.ArrayList;
import java.util.List;

public final class RhiDescriptorWriter {
    private final List<RhiDescriptorWrite> writes = new ArrayList<>();

    public RhiDescriptorWriter uniformBuffer(int binding, RhiBuffer buffer) {
        return uniformBuffer(binding, 0, buffer, 0, buffer.size());
    }

    public RhiDescriptorWriter uniformBuffer(int binding, int arrayElement, RhiBuffer buffer, long offset, long range) {
        writes.add(RhiDescriptorWrite.buffer(binding, arrayElement, RhiDescriptorType.UNIFORM_BUFFER, buffer, offset, range));
        return this;
    }

    public RhiDescriptorWriter storageBuffer(int binding, RhiBuffer buffer) {
        return storageBuffer(binding, 0, buffer, 0, buffer.size());
    }

    public RhiDescriptorWriter storageBuffer(int binding, int arrayElement, RhiBuffer buffer, long offset, long range) {
        writes.add(RhiDescriptorWrite.buffer(binding, arrayElement, RhiDescriptorType.STORAGE_BUFFER, buffer, offset, range));
        return this;
    }

    public RhiDescriptorWriter sampler(int binding, int arrayElement, RhiSampler sampler) {
        writes.add(new RhiDescriptorWrite(binding, arrayElement, RhiDescriptorType.SAMPLER, null, 0, 0, null, sampler));
        return this;
    }

    public RhiDescriptorWriter sampledImage(int binding, int arrayElement, RhiImageView imageView) {
        writes.add(RhiDescriptorWrite.image(binding, arrayElement, RhiDescriptorType.SAMPLED_IMAGE, imageView, null));
        return this;
    }

    public RhiDescriptorWriter combinedImageSampler(int binding, int arrayElement, RhiImageView imageView, RhiSampler sampler) {
        writes.add(RhiDescriptorWrite.image(binding, arrayElement, RhiDescriptorType.COMBINED_IMAGE_SAMPLER, imageView, sampler));
        return this;
    }

    public RhiDescriptorWriter storageImage(int binding, int arrayElement, RhiImageView imageView) {
        writes.add(RhiDescriptorWrite.image(binding, arrayElement, RhiDescriptorType.STORAGE_IMAGE, imageView, null));
        return this;
    }

    public List<RhiDescriptorWrite> writes() {
        return List.copyOf(writes);
    }
}
