plugins {
    // Only apply the dependency management plugin.
    // The Spring Boot plugin version is inherited from the root project.
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        // Import Spring Boot BOM (Bill of Materials) to manage versions automatically.
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    // Core Logic
    api(project(":auto-throttle-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    // Actuator & Micrometer (Observability)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Configuration Processor
    // Generates metadata for IDEs (IntelliJ, Eclipse) to provide auto-completion for 'application.yml'.
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}