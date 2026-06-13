package com.github.slmpc.prismrhi.resource;

import com.github.slmpc.prismrhi.RhiException;

import java.nio.ByteBuffer;

public interface RhiBuffer extends RhiResource {
    long size();

    default ByteBuffer map() {
        return map(0, size());
    }

    default ByteBuffer map(long offset, long size) {
        throw new RhiException(api() + " buffer does not support host mapping");
    }

    default void unmap() {
        throw new RhiException(api() + " buffer does not support host mapping");
    }

    default void write(ByteBuffer data) {
        write(0, data);
    }

    default void write(long offset, ByteBuffer data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        ByteBuffer mapped = map(offset, data.remaining());
        try {
            mapped.put(data.slice());
        } finally {
            unmap();
        }
    }
}
