package com.github.slmpc.prismrhi.resource;

import com.github.slmpc.prismrhi.format.RhiFormat;

import java.util.Objects;

public record RhiImageUploadInfo(
        int x,
        int y,
        int z,
        int width,
        int height,
        int depth,
        int mipLevel,
        int arrayLayer,
        int layerCount,
        long bufferOffset,
        long bytesPerRow,
        long bytesPerImage
) {
    public RhiImageUploadInfo {
        if (x < 0 || y < 0 || z < 0) {
            throw new IllegalArgumentException("image upload offset must not be negative");
        }
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("image upload extent must be positive");
        }
        if (mipLevel < 0) {
            throw new IllegalArgumentException("mipLevel must not be negative");
        }
        if (arrayLayer < 0 || layerCount < 1) {
            throw new IllegalArgumentException("array layer range must be valid");
        }
        if (bufferOffset < 0 || bytesPerRow < 0 || bytesPerImage < 0) {
            throw new IllegalArgumentException("image upload buffer layout must not be negative");
        }
    }

    public static RhiImageUploadInfo of(RhiImage image) {
        Objects.requireNonNull(image, "image");
        return builder(image.extent().width(), image.extent().height(), image.extent().depth()).build();
    }

    public static Builder builder(int width, int height) {
        return builder(width, height, 1);
    }

    public static Builder builder(int width, int height, int depth) {
        return new Builder(width, height, depth);
    }

    public void validateFor(RhiImage image) {
        Objects.requireNonNull(image, "image");
        if (x + width > image.extent().width()
                || y + height > image.extent().height()
                || z + depth > image.extent().depth()) {
            throw new IllegalArgumentException("image upload region is out of bounds");
        }
        requiredBytes(image.format());
    }

    public long requiredBytes(RhiFormat format) {
        int texelSize = Objects.requireNonNull(format, "format").bytesPerPixel();
        long rowBytes = Math.multiplyExact((long) width, texelSize);
        long rowPitch = resolvedBytesPerRow(format);
        long imagePitch = resolvedBytesPerImage(format);
        long imageCount = Math.multiplyExact((long) depth, layerCount);
        return Math.addExact(
                bufferOffset,
                Math.addExact(
                        Math.multiplyExact(imagePitch, imageCount - 1L),
                        Math.addExact(Math.multiplyExact(rowPitch, height - 1L), rowBytes)
                )
        );
    }

    public int bufferRowLength(RhiFormat format) {
        int texelSize = Objects.requireNonNull(format, "format").bytesPerPixel();
        long rowBytes = Math.multiplyExact((long) width, texelSize);
        long rowPitch = resolvedBytesPerRow(format);
        return rowPitch == rowBytes ? 0 : Math.toIntExact(rowPitch / texelSize);
    }

    public int bufferImageHeight(RhiFormat format) {
        long rowPitch = resolvedBytesPerRow(format);
        long tightImagePitch = Math.multiplyExact(rowPitch, height);
        long imagePitch = resolvedBytesPerImage(format);
        return imagePitch == tightImagePitch ? 0 : Math.toIntExact(imagePitch / rowPitch);
    }

    public long resolvedBytesPerRow(RhiFormat format) {
        int texelSize = Objects.requireNonNull(format, "format").bytesPerPixel();
        long rowBytes = Math.multiplyExact((long) width, texelSize);
        long rowPitch = bytesPerRow == 0 ? rowBytes : bytesPerRow;
        if (rowPitch < rowBytes) {
            throw new IllegalArgumentException("bytesPerRow is smaller than the upload row size");
        }
        if (rowPitch % texelSize != 0) {
            throw new IllegalArgumentException("bytesPerRow must be a multiple of the texel size");
        }
        return rowPitch;
    }

    public long resolvedBytesPerImage(RhiFormat format) {
        long rowPitch = resolvedBytesPerRow(format);
        long imagePitch = bytesPerImage == 0 ? Math.multiplyExact(rowPitch, height) : bytesPerImage;
        long minimumImagePitch = Math.multiplyExact(rowPitch, height);
        if (imagePitch < minimumImagePitch) {
            throw new IllegalArgumentException("bytesPerImage is smaller than the upload image size");
        }
        if (imagePitch % rowPitch != 0) {
            throw new IllegalArgumentException("bytesPerImage must be a multiple of bytesPerRow");
        }
        return imagePitch;
    }

    public static final class Builder {
        private int x;
        private int y;
        private int z;
        private int width;
        private int height;
        private int depth;
        private int mipLevel;
        private int arrayLayer;
        private int layerCount = 1;
        private long bufferOffset;
        private long bytesPerRow;
        private long bytesPerImage;

        private Builder(int width, int height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public Builder offset(int x, int y) {
            return offset(x, y, 0);
        }

        public Builder offset(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public Builder extent(int width, int height) {
            return extent(width, height, 1);
        }

        public Builder extent(int width, int height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
            return this;
        }

        public Builder mipLevel(int mipLevel) {
            this.mipLevel = mipLevel;
            return this;
        }

        public Builder arrayRange(int arrayLayer, int layerCount) {
            this.arrayLayer = arrayLayer;
            this.layerCount = layerCount;
            return this;
        }

        public Builder bufferOffset(long bufferOffset) {
            this.bufferOffset = bufferOffset;
            return this;
        }

        public Builder bytesPerRow(long bytesPerRow) {
            this.bytesPerRow = bytesPerRow;
            return this;
        }

        public Builder bytesPerImage(long bytesPerImage) {
            this.bytesPerImage = bytesPerImage;
            return this;
        }

        public RhiImageUploadInfo build() {
            return new RhiImageUploadInfo(
                    x,
                    y,
                    z,
                    width,
                    height,
                    depth,
                    mipLevel,
                    arrayLayer,
                    layerCount,
                    bufferOffset,
                    bytesPerRow,
                    bytesPerImage
            );
        }
    }
}
