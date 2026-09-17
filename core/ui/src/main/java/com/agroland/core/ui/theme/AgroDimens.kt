package com.agroland.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Кеңістік/радиус токендері — бүкіл қосымшада бірдей ырғақ.
 * 4dp торы: экран жиегі 16, блоктар арасы 12, топтар арасы 20/24.
 */
object AgroSpacing {
    /** Иконка мен мәтін арасы. */
    val xs = 4.dp

    /** Бір блок ішіндегі элементтер арасы. */
    val sm = 8.dp

    /** Тізім элементтері арасы. */
    val md = 12.dp

    /** Экранның сол/оң жиегі — БАРЛЫҚ экранда бірдей. */
    val screen = 16.dp

    /** Логикалық топтар арасы. */
    val lg = 20.dp

    /** Секциялар арасы. */
    val xl = 24.dp
}

/** Радиус токендері — чип/өріс/карточка/қалқыма панель. */
object AgroRadius {
    val chip = RoundedCornerShape(10.dp)
    val field = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(16.dp)

    /** Қалқымалы панельдер (төменгі навигация, таб-пилл). */
    val floating = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50)
}

/** Биіктік токендері — тию аймағы 48dp-тен кем болмайды. */
object AgroSize {
    /** Негізгі батырма. */
    val button = 52.dp

    /** Кіші батырма / чип қатары. */
    val buttonSmall = 40.dp

    /** Мәтіндік және іздеу өрісі. */
    val field = 48.dp

    /** Иконка-батырманың тию аймағы. */
    val touchTarget = 44.dp

    /** Қалқымалы төменгі навигация. */
    val navBar = 68.dp

    /** Басты беттің таб-пиллі. */
    val tabBar = 48.dp
}
