package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.resource.RhiFilter;
import com.github.slmpc.prismrhi.resource.RhiIndexType;
import com.github.slmpc.prismrhi.resource.RhiMemoryUsage;
import com.github.slmpc.prismrhi.device.RhiPhysicalDeviceType;
import com.github.slmpc.prismrhi.command.RhiPrimitiveTopology;
import com.github.slmpc.prismrhi.resource.RhiSamplerAddressMode;
import com.github.slmpc.prismrhi.shader.RhiShaderCodeType;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_STRIP;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_READ;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_DEPTH_COMPONENT32F;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL30.GL_RG8;
import static org.lwjgl.opengl.GL30.GL_RGB8;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.GL_RGBA8;
import static org.lwjgl.opengl.GL21.GL_SRGB8_ALPHA8;
import static org.lwjgl.opengl.GL30.GL_UNSIGNED_INT_24_8;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL40.GL_PATCHES;

final class GlDsaSupport {
    private GlDsaSupport() {
    }

    static boolean hasLwjglOpenGl() {
        try {
            Class.forName("org.lwjgl.opengl.GL", false, GlDsaSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    static GLCapabilities requireCapabilities() {
        try {
            GLCapabilities capabilities = GL.getCapabilities();
            if (!capabilities.OpenGL45) {
                throw new RhiException("The current OpenGL context does not expose OpenGL 4.5 DSA");
            }
            return capabilities;
        } catch (IllegalStateException exception) {
            throw new RhiException("OpenGL DSA backend requires a current LWJGL OpenGL context", exception);
        }
    }

    static String rendererName() {
        try {
            String renderer = glGetString(GL_RENDERER);
            return renderer == null || renderer.isBlank() ? "Current OpenGL device" : renderer;
        } catch (RuntimeException exception) {
            return "Current OpenGL device";
        }
    }

    static String vendorName() {
        try {
            String vendor = glGetString(GL_VENDOR);
            return vendor == null ? "" : vendor;
        } catch (RuntimeException exception) {
            return "";
        }
    }

    static RhiPhysicalDeviceType inferDeviceType(String vendor) {
        String normalized = vendor.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("intel") || normalized.contains("apple")) {
            return RhiPhysicalDeviceType.INTEGRATED_GPU;
        }
        if (normalized.contains("nvidia") || normalized.contains("amd") || normalized.contains("ati")) {
            return RhiPhysicalDeviceType.DISCRETE_GPU;
        }
        return RhiPhysicalDeviceType.OTHER;
    }

    static int bufferUsage(RhiMemoryUsage memoryUsage) {
        return switch (memoryUsage) {
            case CPU_TO_GPU -> GL_DYNAMIC_DRAW;
            case GPU_TO_CPU -> GL_STREAM_READ;
            case GPU_ONLY -> GL_STATIC_DRAW;
        };
    }

    static int primitiveTopology(RhiPrimitiveTopology topology) {
        return switch (topology) {
            case POINT_LIST -> GL_POINTS;
            case LINE_LIST -> GL_LINES;
            case LINE_STRIP -> GL_LINE_STRIP;
            case TRIANGLE_LIST -> GL_TRIANGLES;
            case TRIANGLE_STRIP -> GL_TRIANGLE_STRIP;
            case TRIANGLE_FAN -> GL_TRIANGLE_FAN;
            case PATCH_LIST -> GL_PATCHES;
        };
    }

    static int indexType(RhiIndexType indexType) {
        return switch (indexType) {
            case UINT16 -> GL_UNSIGNED_SHORT;
            case UINT32 -> GL_UNSIGNED_INT;
        };
    }

    static boolean depthFormat(RhiFormat format) {
        return format == RhiFormat.D24_UNORM_S8_UINT || format == RhiFormat.D32_FLOAT;
    }

    static boolean stencilFormat(RhiFormat format) {
        return format == RhiFormat.D24_UNORM_S8_UINT;
    }

    static int colorAttachment(int index) {
        return GL_COLOR_ATTACHMENT0 + index;
    }

    static int depthStencilAttachment(RhiFormat format) {
        return stencilFormat(format) ? GL_DEPTH_STENCIL_ATTACHMENT : GL_DEPTH_ATTACHMENT;
    }

    static int filter(RhiFilter filter) {
        return switch (filter == null ? RhiFilter.LINEAR : filter) {
            case NEAREST -> GL_NEAREST;
            case LINEAR -> GL_LINEAR;
        };
    }

    static int addressMode(RhiSamplerAddressMode mode) {
        return switch (mode == null ? RhiSamplerAddressMode.REPEAT : mode) {
            case REPEAT -> GL_REPEAT;
            case MIRRORED_REPEAT -> GL_MIRRORED_REPEAT;
            case CLAMP_TO_EDGE -> GL_CLAMP_TO_EDGE;
            case CLAMP_TO_BORDER -> GL_CLAMP_TO_BORDER;
        };
    }

    static VertexAttributeFormat vertexAttributeFormat(RhiFormat format) {
        return switch (format) {
            case R8_UNORM -> new VertexAttributeFormat(1, GL_UNSIGNED_BYTE, true);
            case RG8_UNORM -> new VertexAttributeFormat(2, GL_UNSIGNED_BYTE, true);
            case RGB8_UNORM -> new VertexAttributeFormat(3, GL_UNSIGNED_BYTE, true);
            case RGBA8_UNORM, BGRA8_UNORM -> new VertexAttributeFormat(4, GL_UNSIGNED_BYTE, true);
            case RGBA16_FLOAT -> new VertexAttributeFormat(4, GL_FLOAT, false);
            case RGBA32_FLOAT -> new VertexAttributeFormat(4, GL_FLOAT, false);
            case RGBA8_SRGB, BGRA8_SRGB, D24_UNORM_S8_UINT, D32_FLOAT, UNDEFINED ->
                    throw new RhiException(format + " is not a valid OpenGL vertex attribute format");
        };
    }

    static boolean supportsIndirectCount() {
        GLCapabilities capabilities = GL.getCapabilities();
        return capabilities.OpenGL46 || capabilities.GL_ARB_indirect_parameters;
    }

    static int shaderType(RhiShaderStage stage, RhiShaderCodeType codeType) {
        if (codeType != RhiShaderCodeType.GLSL) {
            throw new RhiException("OpenGL DSA backend accepts GLSL shader modules only");
        }
        return switch (stage) {
            case VERTEX -> GL_VERTEX_SHADER;
            case FRAGMENT -> GL_FRAGMENT_SHADER;
            case COMPUTE -> GL_COMPUTE_SHADER;
        };
    }

    static void checkShaderCompile(int shader) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            throw new RhiException(glGetShaderInfoLog(shader));
        }
    }

    static TextureFormat textureFormat(RhiFormat format) {
        return switch (format) {
            case R8_UNORM -> new TextureFormat(GL_R8, GL_RG, GL_UNSIGNED_BYTE);
            case RG8_UNORM -> new TextureFormat(GL_RG8, GL_RG, GL_UNSIGNED_BYTE);
            case RGB8_UNORM -> new TextureFormat(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE);
            case RGBA8_UNORM -> new TextureFormat(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE);
            case BGRA8_UNORM -> new TextureFormat(GL_RGBA8, GL_BGRA, GL_UNSIGNED_BYTE);
            case RGBA8_SRGB -> new TextureFormat(GL_SRGB8_ALPHA8, GL_RGBA, GL_UNSIGNED_BYTE);
            case BGRA8_SRGB -> new TextureFormat(GL_SRGB8_ALPHA8, GL_BGRA, GL_UNSIGNED_BYTE);
            case RGBA16_FLOAT -> new TextureFormat(GL_RGBA16F, GL_RGBA, GL_FLOAT);
            case RGBA32_FLOAT -> new TextureFormat(GL_RGBA32F, GL_RGBA, GL_FLOAT);
            case D24_UNORM_S8_UINT -> new TextureFormat(GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8);
            case D32_FLOAT -> new TextureFormat(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT);
            case UNDEFINED -> throw new RhiException("UNDEFINED is not a valid OpenGL texture format");
        };
    }

    static void initializeTextureParameters(int texture) {
        org.lwjgl.opengl.GL45.glTextureParameteri(texture, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        org.lwjgl.opengl.GL45.glTextureParameteri(texture, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        org.lwjgl.opengl.GL45.glTextureParameteri(texture, GL_TEXTURE_WRAP_S, GL_REPEAT);
        org.lwjgl.opengl.GL45.glTextureParameteri(texture, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }

    static int textureTarget() {
        return GL_TEXTURE_2D;
    }

    record TextureFormat(int internalFormat, int externalFormat, int type) {
    }

    record VertexAttributeFormat(int components, int type, boolean normalized) {
    }
}
