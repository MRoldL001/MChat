plugins {
    id("com.android.library")
}

android {
    namespace = "org.scilab.forge.jlatexmath"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
