plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.agroland.feature.chat"
    compileSdk = 36

    defaultConfig {
        minSdk = 25
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:l10n"))
    implementation(project(":core:network"))
    // currentUserId (GET /user/profile) — Flutter userNotifierProvider баламасы.
    implementation(project(":feature:auth"))
    implementation(project(":feature:profile"))
    // Announcement карточкасының Option B префетчі (getAnnouncement).
    implementation(project(":feature:marketplace"))
    // Локация жіберу: backend /location/reverse fallback.
    implementation(project(":feature:location"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Socket.IO клиенті (Flutter io.socket: socket.io_client үйлесімді).
    // org.json Android-та платформалық кітапханада бар — байканы қақтығыстан
    // сақ үшін exclude қыламыз.
    implementation(libs.socket.io.client) {
        exclude(group = "org.json", module = "json")
    }

    // Дауыстық хабарламалар (ExoPlayer) + видео компрессия (Transformer).
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)

    // Локация жіберу: ағымдағы координат алу.
    implementation(libs.play.services.location)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}