import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

// Release signing is read from environment variables (CI) or from an untracked
// keystore.properties file (local builds). Without either, release builds are
// produced unsigned and debug builds use the standard Android debug key.
val keystoreProperties =
  Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
  }

fun signingValue(envName: String, propName: String): String? =
  System.getenv(envName)?.takeIf { it.isNotBlank() } ?: keystoreProperties.getProperty(propName)

val releaseStoreFile = signingValue("SERENE_KEYSTORE_PATH", "storeFile")

android {
  namespace = "app.serene.launcher"
  // Compose 1.12 needs API 37 to compile against. targetSdk stays 36, so runtime behaviour is unchanged.
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "app.serene.launcher"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "0.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (releaseStoreFile != null) {
      create("release") {
        storeFile = rootProject.file(releaseStoreFile)
        storePassword = signingValue("SERENE_KEYSTORE_PASSWORD", "storePassword")
        keyAlias = signingValue("SERENE_KEY_ALIAS", "keyAlias")
        keyPassword = signingValue("SERENE_KEY_PASSWORD", "keyPassword")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfigs.findByName("release")?.let { signingConfig = it }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      // Print the whole stack trace in CI, since the HTML report isn't uploaded.
      all { it.testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL } }
    }
  }
  lint {
    // Fail the build on lint errors; warnings are reported but don't block.
    abortOnError = true
    checkDependencies = false
  }
  dependenciesInfo {
    // Only Google Play reads this block, and the app is distributed as a plain APK.
    includeInApk = false
    includeInBundle = false
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
