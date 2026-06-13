package com.github.slmpc.prismrhi.backend.opengl41;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorBinding;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorWrite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

final class Gl41Descriptors {
    private static final AtomicLong NEXT_HANDLE = new AtomicLong(1);

    private Gl41Descriptors() {
    }

    static Gl41DescriptorSetLayout layout(List<RhiDescriptorBinding> bindings) {
        return new Gl41DescriptorSetLayout(NEXT_HANDLE.getAndIncrement(), bindings);
    }

    static Gl41DescriptorSet set(RhiDescriptorSetLayout layout) {
        if (!(layout instanceof Gl41DescriptorSetLayout glLayout)) {
            throw new RhiException("OpenGL 4.1 descriptor set requires an OpenGL 4.1 layout");
        }
        return new Gl41DescriptorSet(NEXT_HANDLE.getAndIncrement(), glLayout);
    }

    record Gl41DescriptorSetLayout(long nativeHandle, List<RhiDescriptorBinding> bindings) implements RhiDescriptorSetLayout {
        Gl41DescriptorSetLayout {
            bindings = List.copyOf(bindings);
        }

        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_41;
        }

        @Override
        public void close() {
        }
    }

    static final class Gl41DescriptorSet implements RhiDescriptorSet {
        private final long handle;
        private final Gl41DescriptorSetLayout layout;
        private final List<RhiDescriptorWrite> writes = new ArrayList<>();

        Gl41DescriptorSet(long handle, Gl41DescriptorSetLayout layout) {
            this.handle = handle;
            this.layout = layout;
        }

        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_41;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public RhiDescriptorSetLayout layout() {
            return layout;
        }

        @Override
        public void update(Iterable<RhiDescriptorWrite> writes) {
            this.writes.clear();
            for (RhiDescriptorWrite write : writes) {
                this.writes.add(write);
            }
        }

        List<RhiDescriptorWrite> writes() {
            return List.copyOf(writes);
        }

        @Override
        public void close() {
            writes.clear();
        }
    }
}
