plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "faang.school"
version = "1.0"

val javaVersion = 25
val mapstructVersion = "1.6.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    /**
     * Spring boot starters
     */
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    /**
     * Utils & Logging
     */
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind")

    /**
     * Tests
     */

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.0")
}

tasks.withType<Test> {
    useJUnitPlatform()

        jvmArgs(
        "-XX:+EnableDynamicAgentLoading",
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.7.toBigDecimal()
            }
            excludes = listOf(
                "**/dto/**",
                "**/repository/**",
                "**/exception/**",
                "**/config/**"
            )
        }
    }
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.bootJar {
    archiveFileName.set("service.jar")
}
