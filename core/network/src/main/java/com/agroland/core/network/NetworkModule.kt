package com.agroland.core.network

import com.agroland.core.network.auth.AuthInterceptor
import com.agroland.core.network.error.ApiErrorParser
import com.agroland.core.network.error.Failure
import com.agroland.core.network.error.TariffLimitException
import com.agroland.core.network.interceptors.ApiMonitoringInterceptor
import com.agroland.core.network.interceptors.MonitoringInterceptor
import com.agroland.core.network.interceptors.PlatformInterceptor
import com.agroland.core.network.interceptors.RetryInterceptor
import com.agroland.core.network.interceptors.TariffLimitInterceptor
import com.agroland.core.network.json.JsonParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Сеть қабатының Hilt модулі: OkHttp (интерцептор тізбегі) + Retrofit. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Интерцептор тізбегінің реті КРИТИКАЛЫҚ (spec §4):
     * 1) Auth       — Bearer + single-flight 401 refresh
     * 2) Monitoring — логтар
     * 3) ApiMonitoring — оқиға мониторингі (Фаза 19, Flutter 1:1; retry-ден
     *    ТЫСҚАРЫ тұрғандықтан retry-ден КЕЙІНГІ нәтижені көреді)
     * 4) Retry      — GET-only, max 2, 800мс экспоненциал
     * 5) TariffLimit — 403 TARIFF_LIMIT_* → типтелген ерекшелік
     * 6) Platform   — төлем сұрауларына X-Platform: android (spec §5)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        monitoringInterceptor: MonitoringInterceptor,
        apiMonitoringInterceptor: ApiMonitoringInterceptor,
        retryInterceptor: RetryInterceptor,
        tariffLimitInterceptor: TariffLimitInterceptor,
        platformInterceptor: PlatformInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(20))
        .readTimeout(Duration.ofSeconds(60))
        .writeTimeout(Duration.ofSeconds(60))
        .addInterceptor(authInterceptor)
        .addInterceptor(monitoringInterceptor)
        .addInterceptor(apiMonitoringInterceptor)
        .addInterceptor(retryInterceptor)
        .addInterceptor(tariffLimitInterceptor)
        .addInterceptor(platformInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        config: NetworkConfig,
        client: OkHttpClient,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl.ensureTrailingSlash())
        .client(client)
        .addConverterFactory(
            JsonParser.json.asConverterFactory("application/json".toMediaType()),
        )
        .build()

    private fun String.ensureTrailingSlash(): String =
        if (endsWith("/")) this else "$this/"

    /**
     *suspend функцияның дұрыс түрде қабылдануы: retrofit suspend — HttpException,
     * желі қателері — IOException. Барлық репозиторий осы арқылы қорғалады.
     */
    suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: TariffLimitException) {
        ApiResult.Error(Failure.TariffLimit(e.error))
    } catch (e: HttpException) {
        val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
        val apiError = ApiErrorParser.parse(e.code(), body)
        when {
            e.code() == 401 -> ApiResult.Error(Failure.Unauthorized)
            else -> ApiResult.Error(Failure.Http(apiError))
        }
    } catch (e: SerializationException) {
        ApiResult.Error(Failure.Parsing(e))
    } catch (e: UnknownHostException) {
        ApiResult.Error(Failure.Network(e))
    } catch (e: ConnectException) {
        ApiResult.Error(Failure.Network(e))
    } catch (e: SocketTimeoutException) {
        ApiResult.Error(Failure.Network(e))
    } catch (e: IOException) {
        ApiResult.Error(Failure.Network(e))
    } catch (e: Exception) {
        ApiResult.Error(Failure.Unknown(e))
    }
}