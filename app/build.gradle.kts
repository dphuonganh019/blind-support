import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Model nhận dạng giọng nói tiếng Việt chạy offline (Vosk). Được tải tự động lần build đầu.
val voskModelName = "vosk-model-small-vn-0.4"
val voskModelUrl = "https://alphacephei.com/vosk/models/$voskModelName.zip"
val voskModelDir = layout.projectDirectory.dir("src/main/assets/model-vn").asFile

val versionCodeFromCi = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "vn.blindsupport.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "vn.blindsupport.app"
        minSdk = 29
        targetSdk = 35
        versionCode = versionCodeFromCi
        versionName = "1.0.$versionCodeFromCi"

        ndk {
            // Máy Samsung đời mới đều là arm64; giữ armeabi-v7a cho máy rất rẻ chạy 32-bit.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            val ksFile = System.getenv("KEYSTORE_FILE")?.let { File(it) }
                ?: rootProject.file("keystore/release.jks")
            storeFile = ksFile
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "TroLyNgheGoi2026"
            keyAlias = System.getenv("KEY_ALIAS") ?: "blindsupport"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "TroLyNgheGoi2026"
        }
    }

    buildTypes {
        release {
            // Không rút gọn code để tránh làm hỏng thư viện nhận dạng giọng nói (JNA/Vosk).
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        // Không nén model để giải nén nhanh hơn trên máy yếu.
        noCompress += listOf("mdl", "fst", "int", "conf", "mat", "txt", "raw", "dubm", "ie", "stats")
    }

    lint {
        // Không để cảnh báo lint chặn việc build APK.
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Nhận dạng giọng nói offline
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
}

val downloadVoskModel by tasks.registering {
    description = "Tải model nhận dạng giọng nói tiếng Việt (Vosk) vào assets"
    outputs.dir(voskModelDir)
    onlyIf { !File(voskModelDir, "uuid").exists() }
    doLast {
        logger.lifecycle("Đang tải model giọng nói: $voskModelUrl")
        voskModelDir.deleteRecursively()
        voskModelDir.mkdirs()
        val prefix = "$voskModelName/"
        URI(voskModelUrl).toURL().openStream().use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.removePrefix(prefix)
                    if (name.isNotEmpty() && !name.contains("..")) {
                        val out = File(voskModelDir, name)
                        if (entry.isDirectory) {
                            out.mkdirs()
                        } else {
                            out.parentFile.mkdirs()
                            out.outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        // StorageService của Vosk dùng file uuid để biết khi nào cần giải nén lại model.
        File(voskModelDir, "uuid").writeText(voskModelName)
        logger.lifecycle("Đã tải xong model vào ${voskModelDir.path}")
    }
}

tasks.named("preBuild") {
    dependsOn(downloadVoskModel)
}
