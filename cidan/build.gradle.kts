plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish`
    signing
}

android {
    namespace = "com.cidan.app"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "consumer-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")

    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = (project.findProperty("GROUP") as String?) ?: "io.github.arjunjr05"
                artifactId = (project.findProperty("POM_ARTIFACT_ID") as String?) ?: "cidan"
                version = (project.findProperty("VERSION_NAME") as String?) ?: "1.0.0"

                pom {
                    name.set("CIDAN Android SDK")
                    description.set("AI-powered regulatory compliance, circular ingestion, and policy validation engine for Android applications.")
                    url.set("https://github.com/ArjunJr05/cidan")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("ArjunJr05")
                            name.set("Arjun")
                            email.set("arjunfree256@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/ArjunJr05/cidan.git")
                        developerConnection.set("scm:git:ssh://github.com:ArjunJr05/cidan.git")
                        url.set("https://github.com/ArjunJr05/cidan")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "CentralPortalStaging"
                url = uri(layout.buildDirectory.dir("staging-repo"))
            }
        }
    }

    signing {
        useGpgCmd()
        sign(publishing.publications["release"])
    }
}

// Helper task to create a ready-to-upload ZIP bundle for Sonatype Central Portal (central.sonatype.com)
tasks.register<Zip>("bundleForMavenCentral") {
    dependsOn("publishReleasePublicationToCentralPortalStagingRepository")
    archiveFileName.set("cidan-bundle-mavencentral.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(layout.buildDirectory.dir("staging-repo"))
}
