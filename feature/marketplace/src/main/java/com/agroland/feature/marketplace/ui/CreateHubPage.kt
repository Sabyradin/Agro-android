package com.agroland.feature.marketplace.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.agroland.core.l10n.R as L10nR
import com.agroland.feature.location.data.SelectedLocation

/** CreateHubPage қойындылары (iOS реті). */
object CreateHubTab {
    const val AD = 0
    const val OFFER = 1
    const val BULK = 2
}

/**
 * «Қосу» беті — iOS макеті: «Жабу» + атау + «Жарнама жасау | Ұсыныс жасау |
 * Жаппай жүктеу» ауыстырғышы. Әр қойынды — өз ViewModel-і бар бет (навигация
 * жазбасына байланған), сондықтан қойынды ауыстырғанда толтырылған деректер сақталады.
 */
@Composable
fun CreateHubPage(
    onClose: () -> Unit,
    onSubmitted: () -> Unit,
    initialTab: Int = CreateHubTab.AD,
    mapSelection: SelectedLocation? = null,
    onMapSelectionConsumed: () -> Unit = {},
    onOpenMapPicker: (SelectedLocation?) -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        stringResource(L10nR.string.create_tab_ad),
        stringResource(L10nR.string.create_tab_offer),
        stringResource(L10nR.string.create_tab_bulk),
    )
    val closeLabel = stringResource(L10nR.string.common_close)
    val header: @Composable () -> Unit = {
        CreateHeader(
            title = tabs[tab],
            closeLabel = closeLabel,
            onClose = onClose,
            tabs = tabs,
            selectedTab = tab,
            onSelectTab = { tab = it },
        )
    }
    when (tab) {
        CreateHubTab.OFFER -> MakeOfferPage(
            onBack = onClose,
            onSubmitted = onSubmitted,
            header = header,
        )
        CreateHubTab.BULK -> BulkUploadPage(
            onBack = onClose,
            header = header,
        )
        else -> CreateAdPage(
            onBack = onClose,
            onSubmitted = onSubmitted,
            mapSelection = mapSelection,
            onMapSelectionConsumed = onMapSelectionConsumed,
            onOpenMapPicker = onOpenMapPicker,
            header = header,
        )
    }
}
