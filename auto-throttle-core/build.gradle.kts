plugins {
    `java-library`
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

jmh {
    // Benchmark Configuration
    // These settings are optimized for quick validation during development.
    // For production-grade benchmarks, increase iterations/warmup.
    fork = 1
    warmupIterations = 1
    iterations = 1
}