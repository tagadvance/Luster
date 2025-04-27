plugins {
    `java-library`
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.+")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:3.+")

    api("org.slf4j:slf4j-api:2.0.17")

    implementation("com.google.guava:guava:33.4.8-jre")
    implementation("com.google.code.gson:gson:2.13.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

group = "com.tagadvance"
version = "2.0.0"

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "luster"
            from(components["java"])

            pom {
                name.set("Luster")
                description.set("...")
                url.set("https://github.com/tagadvance/Luster")


                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://raw.githubusercontent.com/tagadvance/Luster/main/LICENSE")
                    }
                }

                organization {
                    name.set("tagadvance")
                    url.set("https://tagadvance.com")
                }

                developers {
                    developer {
                        id.set("tagadvance")
                        name.set("Tag Spilman")
                        email.set("tagadvance+Luster@gmail.com")
                        organization.set("tagadvance")
                        organizationUrl.set("https://tagadvance.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com:tagadvance/Luster.git")
                    developerConnection.set("scm:git:ssh://git@github.com:tagadvance/Luster.git")
                    url.set("https://github.com/tagadvance/Luster")
                }
            }
        }
    }

    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            name = "SonatypeSnapshot"
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
        maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
            name = "SonatypeStaging"
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
