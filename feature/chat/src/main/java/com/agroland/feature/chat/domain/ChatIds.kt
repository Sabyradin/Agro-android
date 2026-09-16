package com.agroland.feature.chat.domain

/**
 * «123» / «chat_123» / « 123 » → 123 — Socket push пен REST жауаптарында
 * room id пішімі тұрақсыз (Flutter payload + ISSUES репорттары).
 * Таза функция — бірлік-тестпен қамтылған (ChatSocketService.parseRoomId
 * осыған делегат жасайды).
 */
fun parseRoomIdOrNull(raw: String): Long? {
    val cleaned = raw.removePrefix("chat_").trim()
    return cleaned.toLongOrNull()
}