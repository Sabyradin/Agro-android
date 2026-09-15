package com.agroland.core.network

import com.agroland.core.network.error.Failure

/**
 * Репозиторийден UI-ға берілетін нәтиже орамы.
 * Success — T; Failure — адам тіліндегі хабарлама жасауға қажет мәліметтермен.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Error(val failure: Failure) : ApiResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Error -> this
    }

    fun getOrNull(): T? = (this as? Success)?.value

    companion object {
        fun <T> success(value: T): ApiResult<T> = Success(value)
        fun <T> error(failure: Failure): ApiResult<T> = Error(failure)
    }
}