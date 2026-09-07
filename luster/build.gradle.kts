import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
    `java-library`
    signing
    // Applies Gradle's own maven-publish, then targets the Sonatype Central Portal. `.base` rather
    // than the full `com.vanniktech.maven.publish`, because the full plugin reads its
    // configuration from Gradle properties and this build says it all in one file instead.
    id("com.vanniktech.maven.publish.base") version "0.37.0"
}

group = "com.tagadvance"
version = "3.0.0"
description =
    "Small, sharp tools for Java: declarative caching, log coalescing, record-and-replay mocks, " +
        "stream-friendly reflection, scoped locks, and deadlock detection the JVM cannot do."

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
    testImplementation("org.mockito:mockito-core:5.23.0")

    api("org.slf4j:slf4j-api:2.0.19")
    api("org.jspecify:jspecify:1.0.1")

    implementation("com.google.guava:guava:33.7.1-jre")
    implementation("com.google.code.gson:gson:2.14.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
    withJavadocJar()
}

// The published bytecode is always 17; this only changes the JVM the tests run on, so CI can prove
// the library still behaves on a newer runtime.
// ./gradlew test -PtestJavaVersion=21
val testJavaVersion = (project.findProperty("testJavaVersion") as String?)?.toInt()
if (testJavaVersion != null) {
    val toolchains = extensions.getByType<JavaToolchainService>()
    tasks.withType<Test>().configureEach {
        javaLauncher = toolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(testJavaVersion)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Explicit here as well as in the toolchain so the bytecode target is visible in this file.
    options.release = 17
    options.compilerArgs.add("-Xlint:all")
}

tasks.withType<Javadoc>().configureEach {
    // `missing` stays ON: every public element is documented and the build keeps it that way.
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all", "-quiet")
        links("https://docs.oracle.com/en/java/javase/17/docs/api/")
    }
}

// Byte-for-byte identical archives from identical sources.
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.named<Jar>("jar") {
    manifest {
        // Without this a JPMS consumer derives the module name from the file name, which is not
        // stable. Once published it is effectively permanent, so it is set before the first release.
        attributes("Automatic-Module-Name" to "com.tagadvance.luster")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// What switches signing on, and the only thing that does. Keep it a condition: the plugin's
// signAllPublications() makes signing *required* for any version that is not a -SNAPSHOT, so
// calling it unconditionally would break publishToMavenLocal for anyone without a GPG key.
val signingKey = providers.environmentVariable("GPG_SIGNING_KEY").orNull

// Releasing needs three things from the account holder, none of them in this file:
//
//   1. `com.tagadvance` verified as a namespace at https://central.sonatype.com. Luster already
//      published 2.1.0 under it via the legacy OSSRH, so it should have been migrated across --
//      confirm before the first Portal publish rather than after.
//   2. a Portal token, not the old OSSRH login, exported under the names the plugin reads:
//        export ORG_GRADLE_PROJECT_mavenCentralUsername="$SONATYPE_USER"
//        export ORG_GRADLE_PROJECT_mavenCentralPassword="$SONATYPE_PASSWORD"
//   3. GPG_SIGNING_KEY, and GPG_SIGNING_PASSWORD if the key has one.
//
// Then `./gradlew publishToMavenCentral`, and release the deployment by hand from the portal.
// publishToMavenLocal needs none of the three and must stay that way.
mavenPublishing {
    // 0.37.0 speaks to the Central Portal and nothing else; OSSRH is sunset. No argument means no
    // automatic release -- the deployment waits in the portal until released by hand.
    publishToMavenCentral()

    // None()/None() because `withSourcesJar()`/`withJavadocJar()` above already produce both jars
    // and the variants the published .module carries. Letting the plugin add its own would be a
    // second artifact under the same classifier.
    configure(JavaLibrary(javadocJar = JavadocJar.None(), sourcesJar = SourcesJar.None()))

    if (!signingKey.isNullOrBlank()) {
        signAllPublications()
    }

    pom {
        name.set("Luster")
        description.set(provider { project.description })
        url.set("https://github.com/tagadvance/Luster")
        inceptionYear.set("2023")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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

        issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/tagadvance/Luster/issues")
        }
    }
}

// The plugin's own in-memory key comes from a `signingInMemoryKey` Gradle property. The key here is
// GPG_SIGNING_KEY, an environment variable, so it is handed to Gradle's signing extension directly.
if (!signingKey.isNullOrBlank()) {
    signing {
        useInMemoryPgpKeys(signingKey, providers.environmentVariable("GPG_SIGNING_PASSWORD").orNull)
    }
}
