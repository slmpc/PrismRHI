group = "com.github.slmpc.prismrhi"
version = "1.0-SNAPSHOT"

val lwjglVersion = "3.4.1"
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
        "implementation"("org.lwjgl:lwjgl-vulkan")
        "implementation"("org.lwjgl:lwjgl-vma")
        "runtimeOnly"("org.lwjgl:lwjgl::$lwjglNatives")
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
