plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.agroland.feature.dealer"
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
    // Логистика формасы — Country/Region/District таңдаушылары (LocationRepository).
    implementation(project(":feature:location"))
    // dealer_role қақпалары, VAT тумблеры (UserProfile), балансты көрсету.
    implementation(project(":feature:profile"))
    // Өнім карточкаларын өңдеу (EditAd) және жарнама деталы.
    implementation(project(":feature:marketplace"))
    // Promotion табы — белсенді промолар тізімі (getUserAnnouncementsPromotions).
    implementation(project(":feature:promo"))
    // Цикл жоқ: dealer ешқандай feature модулін кері қолданбайды.

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
    // Түсім диаграммасы — CartesianChartModelProducer + LineCartesianLayer.
    implementation(libs.vico.compose.m3)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
}