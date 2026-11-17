import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Windows (and any desktop JVM) front end for the Redgifs part of Upvote Saver: Swing UI over :reddit:core.
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

application {
    mainClass.set("com.upvotesaver.desktop.MainKt")
}

dependencies {
    implementation(project(":reddit:core"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}

// One self-contained jar; scripts/package-windows.sh puts it next to a Windows Java runtime.
tasks.register<Jar>("fatJar") {
    archiveFileName.set("upvote-saver.jar")
    manifest { attributes["Main-Class"] = "com.upvotesaver.desktop.MainKt" }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/9/module-info.class", "module-info.class")
}
