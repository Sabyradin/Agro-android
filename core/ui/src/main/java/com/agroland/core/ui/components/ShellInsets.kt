package com.agroland.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Қалқымалы төменгі навигация жапқан биіктік.
 *
 * Навигация панелі контенттің ҮСТІНДЕ қалқып тұрады (Flutter `extendBody`) —
 * тізімдер оның астынан өтіп көрінеді. Соңғы элемент панельдің артында
 * жасырынып қалмауы үшін әр қойынды тізімі осы мәнді `contentPadding`-тің
 * төменгі жағына қосады.
 *
 * Shell экраны нақты мәнді ұсынады; shell-ден тыс ашылған беттерде 0.
 */
val LocalShellBottomPadding = compositionLocalOf { 0.dp }

/** Тізімнің төменгі соқпасы: навигация биіктігі + қосымша бос орын. */
@Composable
fun shellBottomPadding(extra: Dp = 0.dp): Dp = LocalShellBottomPadding.current + extra
