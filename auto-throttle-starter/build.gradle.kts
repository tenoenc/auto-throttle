plugins {
    `java-library`
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4"
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api(project(":auto-throttle-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    // Actuator & Micrometer (모니터링 및 메트릭)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // 설정 파일 자동완성 지원 (application.yml)
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Jar>("jar") {
    enabled = true
}