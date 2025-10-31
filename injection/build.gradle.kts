plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    id("com.vanniktech.maven.publish") version "0.34.0"
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

group = "io.github.stedis23"
version = "1.0.0"

android {
    namespace = "com.stedis.injection"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {}
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {

            afterEvaluate {
                from(components["release"])
            }

            groupId = project.group.toString()
            artifactId = "injection"
            version = project.version.toString()

            pom {
                name = "Injection"
                description = "A lightweight dependency injection library for Kotlin"
                url = "https://github.com/Stedis23/Injection"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/license-2.0.txt"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/Stedis23/Injection.git"
                    developerConnection = "scm:git:ssh://github.com/Stedis23/Injection.git"
                    url = "https://github.com/Stedis23/Injection"
                }

                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/Stedis23/Injection/issues"
                }

                developers {
                    developer {
                        id = "Stedis23"
                        name = "Stepan Tokarev"
                        email = "stedis02@gmail.com"
                    }
                }
            }
        }

        // Список репозиториев куда публикуются артефакты
        repositories {
            // mavenCentral() // Публикация в Maven Central делается через REST API с помошью отдельного плагина
            mavenLocal() // Ищете файлы в директории ~/.m2/repository

            // Репозиторий в build папке корня проекта
            maven(url = uri(rootProject.layout.buildDirectory.file("maven-repo"))) {
                name = "BuildDir"
            }
        }
    }
}

mavenPublishing {
    pom {
        name = "Injection"
        description = "A lightweight dependency injection library for Kotlin"
        url = "https://github.com/Stedis23/Injection"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/license-2.0.txt"
            }
        }

        scm {
            connection = "scm:git:git://github.com/Stedis23/Injection.git"
            developerConnection = "scm:git:ssh://github.com/Stedis23/Injection.git"
            url = "https://github.com/Stedis23/Injection"
        }

        issueManagement {
            system = "GitHub"
            url = "https://github.com/Stedis23/Injection/issues"
        }

        developers {
            developer {
                id = "Stedis23"
                name = "Stepan Tokarev"
                email = "stedis02@gmail.com"
            }
        }
    }

    // Публикация в https://central.sonatype.com/
    publishToMavenCentral()
    signAllPublications()
}