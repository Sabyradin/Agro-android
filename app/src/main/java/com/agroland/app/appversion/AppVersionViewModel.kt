package com.agroland.app.appversion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Force-update күйі (Фаза 20): MainActivity root қабатында диалог көрсетеді.
 * Flutter root_page parity — суық старт + resume кезінде forceCheck=true
 * (міндетті жаңарту 24 сағат throttle-ін өткізіп жібереді).
 */
@HiltViewModel
class AppVersionViewModel @Inject constructor(
    private val checker: AppVersionChecker,
) : ViewModel() {

    private val _dialog = MutableStateFlow<UpdateDecision?>(null)

    /** Null емес болса — жаңарту диалогі көрсетіледі. */
    val dialog: StateFlow<UpdateDecision?> = _dialog

    private var inFlight = false

    fun checkForUpdate(forceCheck: Boolean = true) {
        if (inFlight) return
        inFlight = true
        viewModelScope.launch {
            try {
                val decision = checker.check(forceCheck)
                if (decision != null && _dialog.value == null) {
                    _dialog.value = decision
                }
            } finally {
                inFlight = false
            }
        }
    }

    /** Міндетті ЕМЕС жаңарту диалогін жабу (Cancel). */
    fun dismiss() {
        _dialog.value = null
    }
}