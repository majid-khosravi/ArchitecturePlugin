plugins {
    `maven-publish`
    `java-gradle-plugin`
}

group = "com.github.majid-khosravi"
version = "0.3"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13")
}

gradlePlugin {
    val greeting by plugins.creating {
        id = "ir.majidkhosravi.archplug"
        implementationClass = "ir.majidkhosravi.archplug.CreateFeaturePlugin"
        displayName = "ArchPlug Gradle Plugin"
        description = "A Gradle plugin for architecture features"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Add a source set and a task for a functional test suite
val functionalTest by sourceSets.creating
gradlePlugin.testSourceSets(functionalTest)

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
}

tasks.check {
    dependsOn(functionalTestTask)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("ArchPlug")
                description.set("A Gradle plugin for architecture features")
                url.set("https://github.com/majid-khosravi/ArchPlug")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("majid-khosravi")
                        name.set("Majid Khosravi")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/majid-khosravi/ArchPlug.git")
                    developerConnection.set("scm:git:ssh://github.com/majid-khosravi/ArchPlug.git")
                    url.set("https://github.com/majid-khosravi/ArchPlug")
                }
            }
        }
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
        }
    }
}