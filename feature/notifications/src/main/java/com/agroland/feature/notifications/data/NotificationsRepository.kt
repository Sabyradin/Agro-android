package com.agroland.feature.notifications.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Хабарламалар API (Flutter NotificationsRepository):
 *  - GET /notifications → {service_count, support_count, promotions_count};
 *  - GET /notifications/{type}?page=1&limit=20 → {items: [...]}.
 */
interface NotificationsApi {

    /** Үш бөлімнің оқылмаған санағы. */
    @GET("notifications")
    suspend fun getCounter(): JsonElement

    /** Бөлім бойынша хабарламалар тізімі. */
    @GET("notifications/{type}")
    suspend fun getByType(
        @Path("type") type: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object NotificationsApiModule {

    @Provides
    @Singleton
    fun provideNotificationsApi(retrofit: Retrofit): NotificationsApi =
        retrofit.create(NotificationsApi::class.java)
}

/** Хабарламалар репозиторісі — санақ + бөлім тізімдері. */
@Singleton
class NotificationsRepository @Inject constructor(
    private val api: NotificationsApi,
) {

    suspend fun getCounter(): ApiResult<NotificationCounter> = safeCall {
        NotificationsParser.parseCounter(api.getCounter())
    }

    suspend fun getByType(type: NotificationType): ApiResult<List<NotificationItem>> = safeCall {
        NotificationsParser.parseItems(api.getByType(type.path))
    }
}