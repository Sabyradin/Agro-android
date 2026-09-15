package com.agroland.core.network

/**
 * Сеть конфигурациясы. BASE_API_URL /api/v1 префиксі БІР ғана жерде сақталады —
 * осы интерфейсті :app модулінің FlavorBuildConfig әрекеті іске қосады
 * (dev/prod flavor-дан келеді, кейін басқа ешбір жерде префикс қайталанбайды).
 */
interface NetworkConfig {
    /** Толық база: мыс. https://backend-test.../api/v1 — префикссіз жалғасады. */
    val baseUrl: String

    /** true → логтар толық (BODY деңгейі), false → тек негізгі. */
    val isDebug: Boolean
}