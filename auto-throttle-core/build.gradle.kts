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
    // 벤치마크 실행 설정
    fork = 1
    warmupIterations = 1
    iterations = 1
    // 실수로 벤치마크가 너무 오래 도는 걸 방지 (개발 단계)
}