plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // google-services.json — flavor source setтерінде (app/src/dev, app/src/prod);
    // плейсхолдер конфигте push үнсіз ыдырайды (ISSUES.md #1).
    alias(libs.plugins.google.services)
    // Crashlytics плагині build ID жасайды; ол болмаса FirebaseInitProvider
    // іске қосылған сәтте-ақ құлайды (try/catch қорғай алмайды — eager init).
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.agroland.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.agroland.app"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }

        // Google Maps API кілті — gradle.properties MAPS_API_KEY=... (команда бергенше бос).
        // Бос кілтте LocationSelectionPage каталог режиміне өтеді (карта плиткалары жүктелмейді).
        manifestPlaceholders["MAPS_API_KEY"] =
            (project.findProperty("MAPS_API_KEY") as String?).orEmpty()
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // dev: https://backend-test-42ygumvdeq-lm.a.run.app/api/v1
            buildConfigField("String", "BASE_API_URL", "\"https://backend-test-42ygumvdeq-lm.a.run.app/api/v1\"")
            buildConfigField("boolean", "IS_PRODUCTION", "false")
            // Фаза 19: мониторинг servisi бөлек Cloud Run host — ортада жалғыз,
            // dev-те де нақты URL ( Flutter app_config parity). TikTok dev-те өшірулі
            // (сәтсіз init TikTokAnalytics-та қауіпсіз skip).
            buildConfigField("String", "MONITORING_API_URL", "\"https://monitoring-service-42ygumvdeq-uc.a.run.app\"")
            buildConfigField("String", "TIKTOK_APP_ID", "\"\"")
            buildConfigField("String", "TIKTOK_TT_APP_ID", "\"\"")
            buildConfigField("String", "TIKTOK_ACCESS_TOKEN", "\"\"")
        }
        create("prod") {
            dimension = "environment"
            // prod: https://backend-237397542353.europe-central2.run.app/api/v1
            buildConfigField("String", "BASE_API_URL", "\"https://backend-237397542353.europe-central2.run.app/api/v1\"")
            buildConfigField("boolean", "IS_PRODUCTION", "true")
            // Flutter main.json нақты мәндері (Фаза 19): appId — Google Play package,
            // ttAppId — TikTok Events Manager. accessToken клиентте Android SDK
            // қолданбайды (iOS HMAC iOS-only) — тек сақтау/толық паритет үшін.
            buildConfigField("String", "MONITORING_API_URL", "\"https://monitoring-service-42ygumvdeq-uc.a.run.app\"")
            buildConfigField("String", "TIKTOK_APP_ID", "\"com.agroland.app\"")
            buildConfigField("String", "TIKTOK_TT_APP_ID", "\"7677876072366063636\"")
            buildConfigField("String", "TIKTOK_ACCESS_TOKEN", "\"TTKqizd4aEPuG1IanLtG1KzBDeNhwmrn\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":feature:shell"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:marketplace"))
    implementation(project(":feature:cart"))
    implementation(project(":feature:payment"))
    implementation(project(":feature:wallet"))
    implementation(project(":feature:push"))
    implementation(project(":feature:location"))
    implementation(project(":feature:chat"))
    implementation(project(":feature:call"))
    implementation(project(":feature:stories"))
    implementation(project(":feature:promo"))
    implementation(project(":feature:dealer"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:china"))
    implementation(project(":feature:services"))
    implementation(project(":feature:demand"))
    // Фаза 18: пікірлер (MyReviews/seller/announcement) + медиа көрсеткіштері.
    implementation(project(":feature:reviews"))
    implementation(project(":feature:media"))
    // Фаза 19: аналитика (monitoring fan-out + TikTok + Firebase wrapper).
    implementation(project(":core:analytics"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:l10n"))
    implementation(project(":core:network"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Push (Фаза 11): FCM токендері плейсхолдер конфигте де қауіпсіз ыдырайды.
    // Аналитика (Фаза 19): Analytics + Crashlytics осы BOM-нан; плейсхолдер
    // google-services.json-де FirebaseAnalyticsService try/catch қорғайды.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Фаза 17: MercuryX категория суреттері (SVG) — Coil-дің SVG декодері.
    implementation(libs.coil.svg)
    // SingletonImageLoader.Factory (Application) — coil3 bundle артефактында.
    implementation(libs.coil)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Фаза 20: force-update шешім-логикасының бірлік-тесттері (таза функциялар).
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
}