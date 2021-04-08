plugins {
    kotlin("jvm") version "1.3.70"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    jcenter {
        content {
            includeGroup("org.jetbrains.kotlinx")
        }
    }
}

dependencies {
    val kotlinxVersion = "1.3.5"

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")

    implementation("dev.gitlive:kotlin-diff-utils:5.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform()
    }
}