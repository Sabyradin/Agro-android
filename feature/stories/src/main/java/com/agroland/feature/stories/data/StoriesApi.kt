package com.agroland.feature.stories.data

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
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Stories/баннер API (Flutter stories_repository.dart + promo_v2_repository
 * getMainBanners, 1:1):
 *  - GET /banners/main          → {items: [...]} — басты беттің төлемді баннерлері
 *  - GET /stories               → {stories: [...]} — admin сторилер
 *  - POST /stories/{id}/view    → көрілген деп белгілеу (auth, идемпотентті)
 *  - POST /marketing/posts/{id}/click → CTA түртуді тіркеу (анонимді де болады)
 */
interface StoriesApi {

    @GET("banners/main")
    suspend fun mainBanners(): JsonElement

    @GET("stories")
    suspend fun stories(): JsonElement

    @POST("stories/{id}/view")
    suspend fun markStoryViewed(@Path("id") storyId: String): JsonElement

    @POST("marketing/posts/{id}/click")
    suspend fun recordMarketingClick(@Path("id") postId: String): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object StoriesApiModule {

    @Provides
    @Singleton
    fun provideStoriesApi(retrofit: Retrofit): StoriesApi =
        retrofit.create(StoriesApi::class.java)
}

@Singleton
class StoriesRepository @Inject constructor(
    private val api: StoriesApi,
) {

    /** GET /banners/main — backend жауабы толерантты оқылады ({items: []} дев-те). */
    suspend fun getMainBanners(): ApiResult<List<MainBanner>> =
        safeCall { MainBanner.listFromResponse(api.mainBanners()) }

    /** GET /stories — admin сторилер (болмаса бос тізім, қате — Error). */
    suspend fun getStories(): ApiResult<List<StoryItem>> =
        safeCall { StoryItem.listFromJson(api.stories()) }

    /**
     * POST /stories/{id}/view — тек авторизацияланған қолданушы шақырады
     * (Flutter StoriesNotifier.markStoryAsViewed auth-gate). Қате — silent:
     * сториді көруге рұқсат ешқашан шектелмейді.
     */
    suspend fun markStoryViewed(storyId: String): Boolean {
        val result = safeCall { api.markStoryViewed(storyId) }
        return result is ApiResult.Success
    }

    /**
     * POST /marketing/posts/{id}/click — CTA түртуді тіркеу (анонимді де
     * болады). Fire-and-forget: қате UI-ға шықпайды.
     */
    suspend fun recordCtaClick(postId: String) {
        safeCall { api.recordMarketingClick(postId) }
    }
}