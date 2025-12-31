plugins {
    java
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "io.github.tenoenc"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"]) // Java 컴포넌트(Jar 등)를 배포 대상으로 지정
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}