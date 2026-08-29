package com.raku.smsrelay.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationStateTest {
    @Test
    fun notificationForAnotherThreadReplacesTheCurrentConversation() {
        assertTrue(
            shouldNavigateToConversation(
                currentRoute = AppRoute.Conversation.route,
                activeThreadId = 10L,
                targetThreadId = 20L,
                onboardingVisible = false,
            ),
        )
    }

    @Test
    fun restoredMatchingConversationDoesNotCreateADuplicateEntry() {
        assertFalse(
            shouldNavigateToConversation(
                currentRoute = AppRoute.Conversation.route,
                activeThreadId = 20L,
                targetThreadId = 20L,
                onboardingVisible = false,
            ),
        )
    }

    @Test
    fun onboardingDefersNotificationNavigationUntilTheTourFinishes() {
        assertFalse(
            shouldNavigateToConversation(
                currentRoute = AppRoute.Status.route,
                activeThreadId = null,
                targetThreadId = 20L,
                onboardingVisible = true,
            ),
        )
    }
}
