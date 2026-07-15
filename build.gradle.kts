plugins {
    id("java-library")
    id("maven-publish")
    jacoco
    checkstyle
}

group = "pe.edu.nova.java.starters"
version = findProperty("version") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    mavenLocal()
}

val micronautVersion = "5.0.4"
// micronaut-test-junit5 5.0.x caps at 5.0.1 on Maven Central (test artifact
// trails the core artifact by a few patch versions). 5.0.1 is binary-
// compatible with the 5.0.x line used by micronaut-core 5.0.4.
val micronautTestVersion = "5.0.1"
val junitVersion = "6.0.0"
val assertjVersion = "3.26.3"

dependencies {
    // The pure library.
    api("pe.edu.nova.java.libs:nova-notifications:1.0.0")

    // Micronaut core — compileOnly so the module doesn't pull the runtime into
    // consumers. This is a "colloquial module" per
    // docs/java/10-micronaut-analisis-integracion.md: a plain JAR with @Factory
    // and @Bean beans, discovered by Micronaut at runtime via its annotation
    // processor. No separate deployment/runtime module needed.
    compileOnly("io.micronaut:micronaut-core:$micronautVersion")
    compileOnly("io.micronaut:micronaut-context:$micronautVersion")

    // Annotation processor: Micronaut needs to discover @Factory beans at
    // compile time. The processor is supplied by consumers too, but declaring
    // it here makes the API contract self-contained.
    annotationProcessor("io.micronaut:micronaut-inject-java:$micronautVersion")

    // The same processor on the test source set: required for @MicronautTest
    // to generate a BeanDefinition for the test class itself (otherwise
    // Micronaut reports "no bean definition for the test present").
    testAnnotationProcessor("io.micronaut:micronaut-inject-java:$micronautVersion")

    testImplementation("io.micronaut:micronaut-inject:$micronautVersion")
    testImplementation("io.micronaut.test:micronaut-test-junit5:$micronautTestVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "--add-opens", "java.base/java.time=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED"
    )
    finalizedBy(tasks.jacocoTestReport)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all,-missing", "-quiet")
        encoding = "UTF-8"
        charSet = "UTF-8"
    }
}

checkstyle {
    toolVersion = "10.20.1"
    sourceSets = listOf(project.sourceSets.main.get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("Nova Platform Notifications Micronaut Module")
                description.set(
                    "Nova Platform Micronaut module (colloquial, no deployment/runtime split) " +
                    "that bridges nova-notifications (framework-agnostic, pe.edu.nova.java.libs) " +
                    "with Micronaut. Exposes NotificationConfiguration and NotificationFacade as " +
                    "Micronaut beans, configurable via @ConfigurationProperties under " +
                    "nova.notifications.*"
                )
                url.set("https://github.com/ahincho/nova-java-notifications-micronaut-module")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("ahincho")
                        name.set("ahincho")
                        email.set("ahincho@users.noreply.github.com")
                    }
                }
                scm {
                    url.set("https://github.com/ahincho/nova-java-notifications-micronaut-module")
                    connection.set("scm:git:git@github.com:ahincho/nova-java-notifications-micronaut-module.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-notifications-micronaut-module")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
