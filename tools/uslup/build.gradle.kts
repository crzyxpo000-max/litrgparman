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
    mainClass.set("com.yukselis.uslup.UslupKt")
}

// Türkçe üslup doğrulayıcı (docs/USLUP.md §7.1):
//   ./gradlew :tools:uslup:run                       → HEAD'e göre değişen content/ dosyaları
//   ./gradlew :tools:uslup:run --args="--ayrinti"     → her bulguyu yeriyle yaz
//   ./gradlew :tools:uslup:run --args="--olcum"       → yalnız üslup ölçümü (tüm content/, karşılaştırmasız)
//   ./gradlew :tools:uslup:run --args="--ref HEAD~1 content/events/20_orman.yaml"
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}
