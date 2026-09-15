package com.agroland.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.extendedColors
import com.agroland.core.common.validators.Validators
import com.agroland.feature.profile.data.MultipartHelper
import kotlinx.coroutines.launch

/**
 * Профильді өңдеу (EditProfilePage):
 *  - Avatar: фото таңдау → POST /user/avatar (multipart 'file') → profile refresh
 *  - Аты + email: additive PATCH /user/profile — тек өзгерген өрістер жіберіледі
 *  - Email "" → backend NULL (тазартылады)
 */
@Composable
fun EditProfilePage(onBack: () -> Unit) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var name by remember(profile?.id) { mutableStateOf(profile?.name ?: "") }
    var email by remember(profile?.id) { mutableStateOf(profile?.email ?: "") }
    var emailError by remember { mutableStateOf(false) }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)
    val savedToast = stringResource(L10nR.string.profile_saved_toast)
    val avatarToast = stringResource(L10nR.string.profile_avatar_toast)
    val emailInvalid = stringResource(L10nR.string.profile_email_invalid)
    val nameEmpty = stringResource(L10nR.string.auth_name_error)

    val currentAvatar = profile?.avatarUrl

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val part = MultipartHelper.toPart(context, uri, "file")
                if (part != null) {
                    viewModel.uploadAvatar(part)
                } else {
                    snackbar.showSnackbar(avatarToast)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (profile == null) viewModel.refresh()
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowError -> {
                    val text = event.error.backendMessage
                        ?: if (event.error.isNetwork) networkError else genericError
                    snackbar.showSnackbar(text)
                }
                ProfileEvent.Saved -> snackbar.showSnackbar(savedToast)
                ProfileEvent.AvatarUploaded -> snackbar.showSnackbar(avatarToast)
                else -> Unit
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.profile_edit),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.height(24.dp))
                // Avatar.
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(extendedColors().grey)
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    CachedImage(
                        url = currentAvatar,
                        contentDescription = null,
                        modifier = Modifier.size(112.dp),
                    )
                    if (currentAvatar == null) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = extendedColors().secondaryText,
                            modifier = Modifier.size(36.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = extendedColors().white,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(L10nR.string.profile_change_photo),
                    style = MaterialTheme.typography.labelMedium,
                    color = extendedColors().secondaryText,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(extendedColors().card)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AgroTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) emailError = false
                        },
                        label = stringResource(L10nR.string.auth_name_hint),
                        isError = name.isBlank(),
                        errorText = nameEmpty,
                    )
                    AgroTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = false
                        },
                        label = stringResource(L10nR.string.auth_email_hint),
                        isError = emailError,
                        errorText = emailInvalid,
                        supportingText = stringResource(L10nR.string.profile_email_support),
                    )
                }
                Box(modifier = Modifier.height(120.dp))
            }

            BottomActionContainer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                AgroButton(
                    text = stringResource(L10nR.string.common_save),
                    onClick = {
                        val trimmedEmail = email.trim()
                        if (trimmedEmail.isNotEmpty() &&
                            (!Validators.isValidEmailFormat(trimmedEmail) || !Validators.isEmailDomainAllowed(trimmedEmail))
                        ) {
                            emailError = true
                        } else {
                            viewModel.updateProfileFields(
                                originalName = profile?.name,
                                originalEmail = profile?.email,
                                newName = name,
                                newEmail = email,
                            )
                        }
                    },
                    enabled = !saving,
                    loading = saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}