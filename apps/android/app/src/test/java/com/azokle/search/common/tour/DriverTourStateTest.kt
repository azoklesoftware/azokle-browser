package com.azokle.search.common.tour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverTourStateTest {

    private val sampleSteps = listOf(
        TourStep(id = "step1", title = "Step 1", description = "First step"),
        TourStep(id = "step2", title = "Step 2", description = "Second step"),
        TourStep(id = "step3", title = "Step 3", description = "Third step")
    )

    private val homeTourSteps = listOf(
        TourStep("home_omnibar_engine", "Change Search Engine (Top Bar)", "Tap icon to change search engine"),
        TourStep("home_omnibar_bar", "Search or Type Web Address", "Type query or URL"),
        TourStep("home_search_engine", "Change Search Engine (Search Box)", "Tap icon to change engine in box"),
        TourStep("home_search_box", "Search the Web", "Search with text or voice"),
        TourStep("home_shortcuts", "Favorite Websites", "Quick shortcuts"),
        TourStep("home_tabs", "See Your Open Tabs", "View tabs"),
        TourStep("home_burn", "Erase Everything", "One-tap history wipe"),
        TourStep("home_menu", "Menu & Settings", "Access bookmarks and settings")
    )

    private val tabsTourSteps = listOf(
        TourStep("tabs_modes", "Switch Browsing Modes", "Standard, Private, and Tor tabs"),
        TourStep("tabs_card", "Your Open Pages", "Tap to open, X to close"),
        TourStep("tabs_actions", "Quick Clean Up", "Close all or group tabs"),
        TourStep("tabs_new_hub", "Open New Tabs (+)", "Create new tabs")
    )

    @Test
    fun testInitialState() {
        val tourState = TourState(steps = sampleSteps)
        assertFalse(tourState.isVisible)
        assertEquals(0, tourState.currentStepIndex)
        assertEquals(3, tourState.totalSteps)
        assertNull(tourState.currentStep)
        assertFalse(tourState.isLastStep)
    }

    @Test
    fun testStartTour() {
        val tourState = TourState(steps = sampleSteps)
        tourState.start()

        assertTrue(tourState.isVisible)
        assertEquals(0, tourState.currentStepIndex)
        assertNotNull(tourState.currentStep)
        assertEquals("step1", tourState.currentStep?.id)
        assertFalse(tourState.isLastStep)
    }

    @Test
    fun testStartWithSpecificIndex() {
        val tourState = TourState(steps = sampleSteps)
        tourState.start(fromIndex = 2)

        assertTrue(tourState.isVisible)
        assertEquals(2, tourState.currentStepIndex)
        assertEquals("step3", tourState.currentStep?.id)
        assertTrue(tourState.isLastStep)
    }

    @Test
    fun testStartClampsOutOfBoundsIndex() {
        val tourState = TourState(steps = sampleSteps)
        tourState.start(fromIndex = 100)

        assertTrue(tourState.isVisible)
        assertEquals(2, tourState.currentStepIndex)
        assertEquals("step3", tourState.currentStep?.id)
        assertTrue(tourState.isLastStep)
    }

    @Test
    fun testNextAndPreviousNavigation() {
        val tourState = TourState(steps = sampleSteps)
        tourState.start()

        // Step 0 -> Step 1
        tourState.next()
        assertEquals(1, tourState.currentStepIndex)
        assertEquals("step2", tourState.currentStep?.id)
        assertFalse(tourState.isLastStep)

        // Step 1 -> Step 2 (Last)
        tourState.next()
        assertEquals(2, tourState.currentStepIndex)
        assertEquals("step3", tourState.currentStep?.id)
        assertTrue(tourState.isLastStep)

        // Step 2 -> Step 1 (Previous)
        tourState.previous()
        assertEquals(1, tourState.currentStepIndex)
        assertEquals("step2", tourState.currentStep?.id)

        // Step 1 -> Step 0 (Previous)
        tourState.previous()
        assertEquals(0, tourState.currentStepIndex)
        assertEquals("step1", tourState.currentStep?.id)

        // Step 0 -> Previous should stay at 0
        tourState.previous()
        assertEquals(0, tourState.currentStepIndex)
    }

    @Test
    fun testFinishCallbackOnLastStepNext() {
        var finished = false
        val tourState = TourState(steps = sampleSteps, onTourFinished = { finished = true })
        tourState.start(fromIndex = 2)

        assertTrue(tourState.isLastStep)
        tourState.next()

        assertFalse(tourState.isVisible)
        assertNull(tourState.currentStep)
        assertTrue(finished)
    }

    @Test
    fun testSkipTour() {
        var finished = false
        val tourState = TourState(steps = sampleSteps, onTourFinished = { finished = true })
        tourState.start(fromIndex = 1)

        tourState.skip()

        assertFalse(tourState.isVisible)
        assertNull(tourState.currentStep)
        assertTrue(finished)
    }

    @Test
    fun testHomeScreenTourStepFlow() {
        var homeTourDone = false
        val homeTour = TourState(steps = homeTourSteps, onTourFinished = { homeTourDone = true })

        assertEquals(8, homeTour.totalSteps)
        homeTour.start()

        assertEquals("home_omnibar_engine", homeTour.currentStep?.id)
        homeTour.next()
        assertEquals("home_omnibar_bar", homeTour.currentStep?.id)
        homeTour.next()
        assertEquals("home_search_engine", homeTour.currentStep?.id)
        homeTour.next()
        assertEquals("home_search_box", homeTour.currentStep?.id)
        homeTour.next()
        assertEquals("home_shortcuts", homeTour.currentStep?.id)
        homeTour.next()
        assertEquals("home_tabs", homeTour.currentStep?.id)
        homeTour.next()
        assertEquals("home_burn", homeTour.currentStep?.id)
        homeTour.next()
        assertEquals("home_menu", homeTour.currentStep?.id)
        assertTrue(homeTour.isLastStep)

        homeTour.next()
        assertFalse(homeTour.isVisible)
        assertTrue(homeTourDone)
    }

    @Test
    fun testTabsScreenTourStepFlow() {
        var tabsTourDone = false
        val tabsTour = TourState(steps = tabsTourSteps, onTourFinished = { tabsTourDone = true })

        assertEquals(4, tabsTour.totalSteps)
        tabsTour.start()

        assertEquals("tabs_modes", tabsTour.currentStep?.id)
        tabsTour.next()
        assertEquals("tabs_card", tabsTour.currentStep?.id)
        tabsTour.next()
        assertEquals("tabs_actions", tabsTour.currentStep?.id)
        tabsTour.next()
        assertEquals("tabs_new_hub", tabsTour.currentStep?.id)
        assertTrue(tabsTour.isLastStep)

        tabsTour.next()
        assertFalse(tabsTour.isVisible)
        assertTrue(tabsTourDone)
    }

    @Test
    fun testIndependentTourInstances() {
        var homeFinished = false
        var tabsFinished = false

        val homeTour = TourState(steps = homeTourSteps, onTourFinished = { homeFinished = true })
        val tabsTour = TourState(steps = tabsTourSteps, onTourFinished = { tabsFinished = true })

        homeTour.start()
        assertTrue(homeTour.isVisible)
        assertFalse(tabsTour.isVisible)

        homeTour.skip()
        assertFalse(homeTour.isVisible)
        assertTrue(homeFinished)
        assertFalse(tabsFinished)

        tabsTour.start()
        assertTrue(tabsTour.isVisible)
        tabsTour.next()
        assertEquals(1, tabsTour.currentStepIndex)
        assertFalse(tabsFinished)
    }
}
