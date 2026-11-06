import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":engine"))
    testImplementation(libs.junit)
}

application {
    mainClass.set("com.yukselis.simulator.SimulatorKt")
}

// Denge simülatörü: ./gradlew :tools:simulator:run --args="1000"
// Rapor docs/BALANCE.md'ye yazılır.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Xss4m")
}
