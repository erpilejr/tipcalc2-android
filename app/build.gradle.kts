import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// The app's name and id are the harness's: app/identity.properties is protected, the builder cannot change it.
val identity =
    Properties().apply {
        file("identity.properties").reader(Charsets.UTF_8).use { load(it) }
    }

// A resValue is written into an XML resource as it is: escape what aapt reads as markup or as a reference.
fun escapedForXml(text: String): String {
    var out = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    for (c in listOf("'", "\"", "@", "?")) out = out.replace(c, "\\" + c)
    return out
}

android {
    namespace = "dev.harness.app"
    compileSdk = 34

    defaultConfig {
        applicationId = identity.getProperty("applicationId")
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        resValue("string", "app_name", escapedForXml(identity.getProperty("displayName")))
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                // Robolectric keeps memory (not files or preferences) across tests in one JVM: one JVM per test class.
                test.forkEvery = 1
                // Set only inside the harness image; CI and Android Studio leave them unset.
                System.getProperty("robolectric.offline")?.let {
                    test.systemProperty("robolectric.offline", it)
                }
                System.getProperty("robolectric.dependency.dir")?.let {
                    test.systemProperty("robolectric.dependency.dir", it)
                }
            }
        }
    }
}

ktlint {
    version.set(libs.versions.ktlint.engine)
    // The protected scenario tests are the harness's generated code: ktlint skips them.
    filter {
        exclude { it.file.path.contains("/dev/harness/app/spec/") }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
}
