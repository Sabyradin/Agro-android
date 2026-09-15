package com.agroland.feature.push

import com.agroland.feature.push.domain.DuplicateGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 500 мс қайталау қорғаны (Flutter _isDuplicateNotification). */
class DuplicateGuardTest {

    @Test
    fun `same id within cooldown is duplicate`() {
        var time = 1_000L
        val guard = DuplicateGuard(cooldownMs = 500, now = { time })
        assertFalse(guard.isDuplicate("x"))
        time += 100
        assertTrue(guard.isDuplicate("x"))
    }

    @Test
    fun `same id after cooldown passes again`() {
        var time = 1_000L
        val guard = DuplicateGuard(cooldownMs = 500, now = { time })
        assertFalse(guard.isDuplicate("x"))
        time += 600
        assertFalse(guard.isDuplicate("x"))
    }

    @Test
    fun `different id within cooldown is not duplicate`() {
        var time = 1_000L
        val guard = DuplicateGuard(cooldownMs = 500, now = { time })
        assertFalse(guard.isDuplicate("x"))
        time += 100
        assertFalse(guard.isDuplicate("y"))
    }
}