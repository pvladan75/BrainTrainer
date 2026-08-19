import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.program.braintrainer"
    compileSdk = 36

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.program.braintrainer"
        minSdk = 27
        targetSdk = 36
        versionCode = 7
        versionName = "7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "version"
    productFlavors {
        // Flavor za vaše testere - PREIMENOVAN
        create("internal") {
            dimension = "version"
            buildConfigField("boolean", "IS_TEST_BUILD", "true")
        }
        // Flavor za javnu objavu na Google Play
        create("googlePlay") {
            dimension = "version"
            buildConfigField("boolean", "IS_TEST_BUILD", "false")
        }
    }

    buildTypes {
        getByName("debug") {
            configure<CrashlyticsExtension> {
                // Debug nema obfuskaciju, pa nema ni šta da se uploaduje.
                mappingFileUploadEnabled = false
            }
        }
        getByName("release") {
            // Bez mapping fajla su stack trace-ovi iz produkcije nečitljivi,
            // jer R8 obfuskuje imena klasa i metoda.
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Solver loguje preko android.util.Log; bez ovoga svaki poziv u
            // JVM testu baca "not mocked".
            isReturnDefaultValues = true
        }
    }
}

// Šema baze se izvozi u repozitorijum: migracija bez zapisane šeme je pogađanje,
// a Room bez zapisane šeme ne može ni da proveri da li je tačna.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Google Play services dovlače androidx.fragment 1.1.0, koji je njegov autor
    // označio kao zastareo i na koji se Play Console žali pri svakom izdanju.
    // Aplikacija nema nijedan Fragment; ovo samo podiže verziju koja i tako ulazi
    // u build, pa se ne dodaje kao prava zavisnost nego kao ograničenje.
    constraints {
        implementation(libs.androidx.fragment) {
            because("Play services vuku 1.1.0; Play Console traži 1.2.1+")
        }
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.core.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material.icons.extended.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // Lokalna baza odigranih zagonetki
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
    implementation(libs.billing)
    implementation(libs.billing.ktx)
}