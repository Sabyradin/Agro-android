package com.agroland.core.network.interceptors

/**
 * API оқиғалары туралы хабарлама тұғыры (Фаза 19).
 * core:network → core:analytics бағытында тікелей тәуелділік ЖОҚ (жүйелік
 * тәуелділік кері: analytics network-ті қажет етеді) — сондықтан осы
 * интерфейс арқылы қосылады: Hilt байлымын core:analytics береді
 * (MonitoringService жүзеге асырады), OkHttp тізбегі осыны шақырады.
 */
interface ApiEventReporter {
    fun apiRequestSuccess(endpoint: String, httpMethod: String, statusCode: Int)
    fun apiRequestFailure(endpoint: String, httpMethod: String, statusCode: Int?, errorMessage: String?)
}