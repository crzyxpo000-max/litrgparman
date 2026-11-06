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
    // İçerik ağacı engine'in YamlTree'siyle okunur (JsonElement).
    implementation(project(":engine"))
    testImplementation(libs.junit)
}

application {
    mainClass.set("com.yukselis.tarama.TaramaKt")
}

// İçerik tarayıcı (docs/QA.md): çeviri, belirteç, uzunluk, tekrar, ölü dal ve bayrak sorunlarını sayar.
//   ./gradlew :tools:tarama:run                 → docs/TARAMA.md (ayrıntı) + konsol özeti
//   ./gradlew :tools:tarama:run --args="çıktı.md"
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}
