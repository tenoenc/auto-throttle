// Vanniktech 플러그인 전역 설정
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    java
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("com.vanniktech.maven.publish") version "0.30.0"
}

allprojects {
    group = "io.github.tenoenc"
    version = "1.0.0"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.vanniktech.maven.publish")

    java {
        toolchain {
            // Enforce Java 21+ to support Virtual Threads.
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    mavenPublishing {
        // 신형 포털(Central Portal)로 배포
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

        pom {
            name.set("Auto Throttle")
            description.set("Adaptive Concurrency Limits for Spring Boot based on TCP Vegas")
            url.set("https://github.com/tenoenc/auto-throttle")
            licenses {
                license {
                    name.set("The MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("tenoenc")
                    name.set("tenoenc")
                    email.set("tenoenc@gmail.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/tenoenc/auto-throttle.git")
                developerConnection.set("scm:git:ssh://github.com/tenoenc/auto-throttle.git")
                url.set("https://github.com/tenoenc/auto-throttle")
            }
        }
    }

    // GitHub Packages 배포
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/tenoenc/auto-throttle")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}