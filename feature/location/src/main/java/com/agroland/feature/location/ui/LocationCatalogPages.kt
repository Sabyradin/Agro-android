package com.agroland.feature.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.AgroListTile
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.location.data.CatalogLocation
import com.agroland.feature.location.data.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Қондырғы деңгейіндегі өңір таңдау (AppRegionSetup, spec: CountrySelectorPage /
 * RegionSelectorPage — AppRegionNotifier SharedPreferences → DataStore).
 *
 * Splash → CountryListPage → RegionListPage → MainShell (жаңа стек).
 * Формалар ішіндегі пикерлер бұл беттерді қолданбайды — олар CatalogPickerDialog.
 */
@HiltViewModel
class CatalogPageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LocationRepository,
) : ViewModel() {

    data class State(
        val loading: Boolean = false,
        val error: Boolean = false,
        val items: List<CatalogLocation> = emptyList(),
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    /**
     * RegionListRoute аргументі. Type-safe навигацияда кілт — Kotlin қасиетінің
     * АТЫ (`countryId`); бұрын мұнда snake_case («country_id») тұрған да, мән
     * әрқашан null болып, өңір тізімінің орнына ЕЛДЕР тізімі қайта ашылатын.
     * Екі жазылу да оқылады — маршрут аты өзгерсе де сынбайды.
     */
    private val countryId: Int? = savedStateHandle.get<Int>("countryId")
        ?: savedStateHandle.get<Int>("country_id")

    init {
        // RegionListRoute country_id жібереді; болмаса — елдер тізімі.
        load(countryId)
    }

    private fun load(countryId: Int?) {
        _state.value = State(loading = true)
        viewModelScope.launch {
            val result = if (countryId != null) {
                repository.getRegions(countryId)
            } else {
                repository.getCountries()
            }
            _state.value = when (result) {
                is com.agroland.core.network.ApiResult.Success -> State(items = result.value)
                else -> State(error = true)
            }
        }
    }

    fun retry() {
        load(countryId)
    }
}

/** Елдер тізімі — өңір орнату қадамы 1. */
@Composable
fun CountryListPage(
    onPickCountry: (CatalogLocation) -> Unit,
) {
    val viewModel: CatalogPageViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.app_region_title),
                onBack = null,
            )
        },
    ) { inner ->
        Column(modifier = inner.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(L10nR.string.app_region_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = extendedColors().secondaryText,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            when {
                state.loading -> LoadingWidget(Modifier.fillMaxSize())
                state.error -> ErrorWithRetry(
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.items, key = { it.id }) { country ->
                        AgroListTile(
                            containerColor = extendedColors().card,
                            title = country.localizedName(localeTag),
                            onClick = { onPickCountry(country) },
                            leading = {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = extendedColors().secondaryText,
                                )
                            },
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = extendedColors().secondaryText,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Облыстар/қалалар тізімі — өңір орнату қадамы 2 (country_id маршрут параметрі). */
@Composable
fun RegionListPage(
    countryName: String?,
    onPickRegion: (CatalogLocation) -> Unit,
) {
    val viewModel: CatalogPageViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = countryName ?: stringResource(L10nR.string.location_choose_region),
                onBack = null,
            )
        },
    ) { inner ->
        Column(modifier = inner.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(L10nR.string.app_region_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = extendedColors().secondaryText,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            when {
                state.loading -> LoadingWidget(Modifier.fillMaxSize())
                state.error -> ErrorWithRetry(
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.items, key = { it.id }) { region ->
                        AgroListTile(
                            containerColor = extendedColors().card,
                            title = region.localizedName(localeTag),
                            onClick = { onPickRegion(region) },
                            leading = {
                                // Облыстар үшін — орын белгісі (глобус бір елдің
                                // ішінде әр жолда қайталанып, шуыл жасайтын).
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}