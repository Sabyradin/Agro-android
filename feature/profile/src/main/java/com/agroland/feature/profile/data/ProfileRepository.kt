package com.agroland.feature.profile.data

import com.agroland.core.network.ApiResult
import com.agroland.feature.location.data.SelectedLocation
import com.agroland.core.network.NetworkModule
import com.agroland.core.network.error.Failure
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Профиль/компания/KYC репозиторісі — барлық API шақыруы safeCall арқылы.
 * Жауап JsonObject → ProfileParser (кешірімді); парсинг нәтижесі бос болса Failure.Parsing.
 * Қате хабарламасы UI-да локализацияланады (шикі error_code шықпайды).
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val userApi: UserApi,
) {

    suspend fun getProfile(): ApiResult<UserProfile> =
        parseProfileBody { userApi.getProfile() }

    suspend fun updateProfile(name: String?, email: String?): ApiResult<UserProfile> =
        parseProfileBody { userApi.updateProfile(ProfileRequests.updateProfile(name, email)) }

    /** VAT тумблері — Дилер баптаулары (Фаза 16), additive PATCH {is_vat_payer}. */
    suspend fun updateVatPayer(value: Boolean): ApiResult<UserProfile> =
        parseProfileBody { userApi.updateProfile(ProfileRequests.isVatPayer(value)) }

    suspend fun uploadAvatar(part: MultipartBody.Part): ApiResult<String?> =
        safeCall { ProfileParser.parseAvatarUrl(userApi.uploadAvatar(part)) }

    suspend fun createLocation(
        street: String,
        house: String,
        catalog: SelectedLocation?,
        latitude: Double?,
        longitude: Double?,
    ): ApiResult<UserLocation> =
        parseLocationBody {
            userApi.createLocation(
                ProfileRequests.location(street, house, catalog, latitude, longitude),
            )
        }

    /**
     * Мекенжайды «өңдеу» — Flutter refreshStale үлгісі: жаңасын POST етіп,
     * ескісін DELETE жасаймыз (PATCH /user/location/{id} жоқ).
     * Қайта жүктеу кезінде backend district_id-ды координат бойынша өздігінен
     * турындатады.
     */
    suspend fun replaceLocation(
        oldId: Long,
        street: String,
        house: String,
        catalog: SelectedLocation?,
        latitude: Double?,
        longitude: Double?,
    ): ApiResult<UserLocation> {
        val created = createLocation(street, house, catalog, latitude, longitude)
        if (created is ApiResult.Error) return created
        deleteLocation(oldId)
        return created
    }

    suspend fun deleteLocation(id: Long): ApiResult<Unit> =
        safeCall { userApi.deleteLocation(id); Unit }

    suspend fun updateCompanyRepresentative(name: String, position: String): ApiResult<Unit> =
        safeCall { userApi.updateCompanySection("representative", ProfileRequests.companyRepresentative(name, position)); Unit }

    suspend fun updateCompanyAbout(text: String): ApiResult<Unit> =
        safeCall { userApi.updateCompanySection("about", ProfileRequests.companyAbout(text)); Unit }

    suspend fun updateCompanyContacts(
        phone: String?,
        website: String?,
        telegram: String?,
        whatsapp: String?,
    ): ApiResult<Unit> =
        safeCall {
            userApi.updateCompanySection("contacts", ProfileRequests.companyContacts(phone, website, telegram, whatsapp))
            Unit
        }

    suspend fun updateCompanyDecor(logo: MultipartBody.Part?, banner: MultipartBody.Part?): ApiResult<Unit> =
        safeCall { userApi.updateCompanyDecor(logo, banner); Unit }

    suspend fun acceptBusinessTerms(): ApiResult<Unit> =
        safeCall { userApi.acceptBusinessTerms(); Unit }

    suspend fun getVerificationStatus(): ApiResult<VerificationStatus> =
        parseVerificationBody { userApi.getVerificationStatus() }

    suspend fun submitVerification(
        fullName: String?,
        iin: String?,
        documents: List<MultipartBody.Part>,
    ): ApiResult<VerificationStatus> {
        val plain = "text/plain".toMediaType()
        val name = fullName?.trim()?.takeIf { it.isNotEmpty() }?.toRequestBody(plain)
        val iinBody = iin?.trim()?.takeIf { it.isNotEmpty() }?.toRequestBody(plain)
        return parseVerificationBody { userApi.submitVerification(name, iinBody, documents) }
    }

    suspend fun deleteAccount(): ApiResult<Unit> = safeCall { userApi.deleteAccount(); Unit }

    /** NetworkModule.safeCall — қысқа атау үшін жергілікті делдал. */
    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> =
        NetworkModule.safeCall(block)

    private suspend fun parseProfileBody(block: suspend () -> JsonObject): ApiResult<UserProfile> {
        val raw = safeCall(block)
        return when (raw) {
            is ApiResult.Success ->
                ProfileParser.parseProfile(raw.value)?.let { ApiResult.Success(it) }
                    ?: ApiResult.Error(Failure.Parsing(IllegalStateException("Пустой ответ")))
            is ApiResult.Error -> raw
        }
    }

    private suspend fun parseLocationBody(block: suspend () -> JsonObject): ApiResult<UserLocation> {
        val raw = safeCall(block)
        return when (raw) {
            is ApiResult.Success ->
                ProfileParser.parseLocation(raw.value)?.let { ApiResult.Success(it) }
                    ?: ApiResult.Error(Failure.Parsing(IllegalStateException("Пустой ответ")))
            is ApiResult.Error -> raw
        }
    }

    private suspend fun parseVerificationBody(block: suspend () -> JsonObject): ApiResult<VerificationStatus> {
        val raw = safeCall(block)
        return when (raw) {
            is ApiResult.Success ->
                ProfileParser.parseVerificationStatus(raw.value)?.let { ApiResult.Success(it) }
                    ?: ApiResult.Error(Failure.Parsing(IllegalStateException("Пустой ответ")))
            is ApiResult.Error -> raw
        }
    }
}