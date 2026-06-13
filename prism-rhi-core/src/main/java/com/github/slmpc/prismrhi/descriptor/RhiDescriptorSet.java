package com.github.slmpc.prismrhi.descriptor;

import com.github.slmpc.prismrhi.resource.RhiResource;

import java.util.function.Consumer;

public interface RhiDescriptorSet extends RhiResource {
    RhiDescriptorSetLayout layout();

    void update(Iterable<RhiDescriptorWrite> writes);

    default void update(Consumer<RhiDescriptorWriter> writerConsumer) {
        RhiDescriptorWriter writer = new RhiDescriptorWriter();
        writerConsumer.accept(writer);
        update(writer.writes());
    }
}
