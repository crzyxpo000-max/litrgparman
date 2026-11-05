import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
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
    // Kayıt dosyası ve içerik JSON'u için. Derleyici eklentisi gerekmez; JsonElement API'si elle kullanılıyor.
    api(libs.kotlinx.serialization.json)
    // İçerik YAML'la yazılır; kaml'ın düğüm API'si kullanılır (derleyici eklentisi gerekmez).
    implementation(libs.kaml)
    testImplementation(libs.junit)
}

tasks.test {
    // İçerik testleri ../content klasörünü okur.
    workingDir = projectDir
}

// İçerik doğrulayıcı: tanımsız sahne, çözülemeyen isim belirteci, eksik çeviri vb.
tasks.register<JavaExec>("validateContent") {
    group = "verification"
    description = "content/ klasöründeki tüm oyun içeriğini doğrular."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.yukselis.engine.content.ContentValidatorKt")
    args = listOf(rootProject.file("content").absolutePath)
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}
