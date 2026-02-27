import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.vanniktech.maven.publish") version "0.32.0"
}

android {
    namespace = "com.ml.shubham0204.sentence_embedding"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    androidResources {
        noCompress += "onnx"
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(
        "io.gitlab.shubham0204",
        "sentence-embeddings",
        "v6.1",
    )
    pom {
        name = "Sentence-Embeddings-Android"
        description =
            "Embeddings from sentence-transformers in Android! Supports all-MiniLM-L6-V2, bge-small-en, snowflake-arctic and custom models"
        inceptionYear = "2024"
        url = "https://github.com/shubham0204/Sentence-Embeddings-Android"
        version = "v6.1"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "shubham0204"
                name = "Shubham Panchal"
                url = "https://github.com/shubham0204"
            }
        }
        scm {
            url = "https://github.com/shubham0204/Sentence-Embeddings-Android"
            connection = "scm:git:git://github.com/shubham0204/Sentence-Embeddings-Android.git"
            developerConnection = "scm:git:ssh://git@github.com/shubham0204/Sentence-Embeddings-Android.git"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.23.0")
}
