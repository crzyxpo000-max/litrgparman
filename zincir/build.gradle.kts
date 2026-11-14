// Eklentiler burada yalnızca tanıtılır; asıl kullanım app/build.gradle.kts içindedir.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
