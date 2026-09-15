package com.agroland.core.network.error

/** 403 TARIFF_LIMIT_* — UI «Тариф жаңарту» диалогын көрсетеді. */
class TariffLimitException(val error: ApiError) : RuntimeException(error.code ?: "TARIFF_LIMIT")