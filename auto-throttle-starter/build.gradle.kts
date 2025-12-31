plugins {
    // 의존성 관리 플러그인만 적용 (버전은 Root에서 상속받으므로 생략)
    id("io.spring.dependency-management")
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