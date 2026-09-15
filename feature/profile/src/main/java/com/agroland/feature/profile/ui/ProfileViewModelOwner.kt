package com.agroland.feature.profile.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Профиль экрандары ортақ (activity scope) ProfileViewModel-ді пайдаланады:
 * хаб + барлық саб-беттер бір профиль дерегін көреді, өңдеу бірден көрінеді.
 */
@Composable
fun rememberProfileViewModel(): ProfileViewModel {
    var context: Context? = LocalContext.current
    var owner: ViewModelStoreOwner? = null
    while (context is ContextWrapper) {
        if (context is ViewModelStoreOwner) {
            owner = context
            break
        }
        context = context.baseContext
    }
    val storeOwner = owner ?: error("ViewModelStoreOwner табылмады")
    return viewModel(storeOwner)
}