group = "com.github.slmpc.prismrhi"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.4.1"
val isMacOs = System.getProperty("os.name").lowercase().contains("mac")
val vulkanSdkPath = providers.environmentVariable("VULKAN_SDK")
    .orElse(providers.gradleProperty("vulkan_sdk"))
val vulkanValidationLayer = providers.environmentVariable("VULKAN_VALIDATION_LAYER")
    .orElse(providers.gradleProperty("vulkan_validation_layer"))
val lwjglNatives = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    when {
        os.contains("mac") && arch.contains("aarch64") -> "natives-macos-arm64"
        os.contains("mac") && arch.contains("arm") -> "natives-macos-arm64"
        os.contains("mac") -> "natives-macos"
        os.contains("win") && arch.contains("64") -> "natives-windows"
        os.contains("linux") && arch.contains("64") -> "natives-linux"
        else -> error("Unsupported LWJGL native platform: $os/$arch")
    }
}

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:6.0.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

project(":prism-rhi-backend-opengl41") {
    dependencies {
        "api"(project(":prism-rhi-core"))
        "implementation"(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
        "implementation"("org.lwjgl:lwjgl")
        "implementation"("org.lwjgl:lwjgl-opengl")
        "runtimeOnly"("org.lwjgl:lwjgl::$lwjglNatives")
        "runtimeOnly"("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    }
}

project(":prism-rhi-backend-opengl-dsa") {
    dependencies {
        "api"(project(":prism-rhi-core"))
        "implementation"(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
        "implementation"("org.lwjgl:lwjgl")
        "implementation"("org.lwjgl:lwjgl-opengl")
        "runtimeOnly"("org.lwjgl:lwjgl::$lwjglNatives")
        "runtimeOnly"("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    }
}

project(":prism-rhi-backend-vulkan") {
    dependencies {
        "api"(project(":prism-rhi-core"))
        "implementation"(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
        "implementation"("org.lwjgl:lwjgl")
        "implementation"("org.lwjgl:lwjgl-glfw")
        "implementation"("org.lwjgl:lwjgl-vulkan")
        "implementation"("org.lwjgl:lwjgl-vma")
        "runtimeOnly"("org.lwjgl:lwjgl::$lwjglNatives")
        "runtimeOnly"("org.lwjgl:lwjgl-glfw::$lwjglNatives")
        "runtimeOnly"("org.lwjgl:lwjgl-vulkan::$lwjglNatives")
        "runtimeOnly"("org.lwjgl:lwjgl-vma::$lwjglNatives")
    }
}

project(":prism-rhi-shaderc") {
    dependencies {
        "api"(project(":prism-rhi-core"))
        "implementation"(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
        "implementation"("org.lwjgl:lwjgl")
        "implementation"("org.lwjgl:lwjgl-shaderc")
        "runtimeOnly"("org.lwjgl:lwjgl::$lwjglNatives")
        "runtimeOnly"("org.lwjgl:lwjgl-shaderc::$lwjglNatives")
    }
}

project(":prism-rhi-demo-triangle") {
    apply(plugin = "application")

    extensions.configure<JavaApplication> {
        mainClass.set("com.github.slmpc.prismrhi.demo.triangle.VulkanTriangleDemo")
        if (isMacOs) {
            applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
        }
    }

    dependencies {
        "implementation"(project(":prism-rhi-core"))
        "implementation"(project(":prism-rhi-backend-vulkan"))
        "runtimeOnly"(project(":prism-rhi-shaderc"))
    }

    tasks.register("runTriangleDemo") {
        group = "application"
        description = "Runs the Vulkan triangle demo."
        dependsOn("run")
    }

    tasks.withType<JavaExec>().configureEach {
        if (isMacOs) {
            jvmArgs("-XstartOnFirstThread")
            configureMacOsVulkanSdk(this, "[triangle-demo]")
        }
        if (vulkanValidationLayer.orNull == "1") {
            args("--vulkanValidation")
            systemProperty("prismrhi.vulkanValidation", "true")
        }
    }
}

fun configureMacOsVulkanSdk(task: JavaExec, logPrefix: String) {
    val sdkPath = vulkanSdkPath.orNull
    if (sdkPath.isNullOrBlank()) {
        if (vulkanValidationLayer.orNull == "1") {
            logger.warn("$logPrefix Vulkan validation layers may be unavailable: set VULKAN_SDK or -Pvulkan_sdk=<path>.")
        }
        return
    }

    val sdkDir = file(sdkPath)
    val loader = sdkDir.resolve("lib/libvulkan.1.dylib")
    if (loader.exists()) {
        task.systemProperty("org.lwjgl.vulkan.libname", loader.absolutePath)
    } else {
        logger.warn("$logPrefix Vulkan loader was not found at ${loader.absolutePath}.")
    }

    val layerPath = sdkDir.resolve("share/vulkan/explicit_layer.d")
    if (layerPath.isDirectory) {
        task.environment("VK_LAYER_PATH", layerPath.absolutePath)
    } else if (vulkanValidationLayer.orNull == "1") {
        logger.warn("$logPrefix Vulkan validation layer manifests were not found at ${layerPath.absolutePath}.")
    }

    val icdManifest = sdkDir.resolve("share/vulkan/icd.d/MoltenVK_icd.json")
    if (icdManifest.exists()) {
        task.environment("VK_ICD_FILENAMES", icdManifest.absolutePath)
        task.environment("VK_DRIVER_FILES", icdManifest.absolutePath)
    }
}
