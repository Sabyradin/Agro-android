package com.agroland.feature.marketplace.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Marketplace ортақ ViewModel-дері activity scope-та жасалады:
 * категориялар кеші CategoriesPage → SubcategoriesPage арасында қайта сұралмайды.
 */
@Composable
fun rememberCategoriesViewModel(): CategoriesViewModel {
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