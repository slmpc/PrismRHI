package com.github.slmpc.prismrhi.resource;

public enum RhiIndexType {
    UINT16(2),
    UINT32(4);

    private final int bytes;

    RhiIndexType(int bytes) {
        this.bytes = bytes;
    }

    public int bytes() {
        return bytes;
    }
}
