plugins {
    id("com.android.application")
}

android {
    namespace = "com.joaofabijack.tripper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.joaofabijack.tripper"
        minSdk = 31
        targetSdk = 36
        versionCode = 9
        versionName = "0.6.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        // Toolchain upgrades are separate from correctness checks for this baseline.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "OldTargetApi")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
