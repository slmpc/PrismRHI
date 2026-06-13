package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorBinding;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorWrite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

final class GlDsaDescriptors {
    private static final AtomicLong NEXT_HANDLE = new AtomicLong(1);

    private GlDsaDescriptors() {
    }

    static GlDsaDescriptorSetLayout layout(List<RhiDescriptorBinding> bindings) {
        return new GlDsaDescriptorSetLayout(NEXT_HANDLE.getAndIncrement(), bindings);
    }

    static GlDsaDescriptorSet set(RhiDescriptorSetLayout layout) {
        if (!(layout instanceof GlDsaDescriptorSetLayout glLayout)) {
            throw new RhiException("OpenGL DSA descriptor set requires an OpenGL DSA layout");
        }
        return new GlDsaDescriptorSet(NEXT_HANDLE.getAndIncrement(), glLayout);
    }

    record GlDsaDescriptorSetLayout(long nativeHandle, List<RhiDescriptorBinding> bindings) implements RhiDescriptorSetLayout {
        GlDsaDescriptorSetLayout {
            bindings = List.copyOf(bindings);
        }

        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_DSA;
        }

        @Override
        public void close() {
        }
    }

    static final class GlDsaDescriptorSet implements RhiDescriptorSet {
        private final long handle;
        private final GlDsaDescriptorSetLayout layout;
        private final List<RhiDescriptorWrite> writes = new ArrayList<>();

        GlDsaDescriptorSet(long handle, GlDsaDescriptorSetLayout layout) {
            this.handle = handle;
            this.layout = layout;
        }

        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_DSA;
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
