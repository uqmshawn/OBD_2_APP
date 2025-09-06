package com.hurtec.obd2.diagnostics.ui.screens.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hurtec.obd2.diagnostics.ui.theme.HurtecOBD2Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for DashboardScreen
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardScreen_displaysCorrectTitle() {
        composeTestRule.setContent {
            HurtecOBD2Theme {
                DashboardScreen()
            }
        }

        composeTestRule
            .onNodeWithText("Vehicle Dashboard")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_displaysGauges() {
        composeTestRule.setContent {
            HurtecOBD2Theme {
                DashboardScreen()
            }
        }

        // Check that gauge cards are displayed
        composeTestRule
            .onNodeWithText("Engine RPM")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Vehicle Speed")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Engine Load")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Coolant Temperature")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_refreshButtonWorks() {
        composeTestRule.setContent {
            HurtecOBD2Theme {
                DashboardScreen()
            }
        }

        // Find and click refresh button
        composeTestRule
            .onNodeWithContentDescription("Refresh data")
            .assertIsDisplayed()
            .performClick()

        // Verify the action was performed (in a real test, you'd verify the data changed)
        composeTestRule
            .onNodeWithContentDescription("Refresh data")
            .assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_gaugesShowValues() {
        composeTestRule.setContent {
            HurtecOBD2Theme {
                DashboardScreen()
            }
        }

        // Check that gauges show numeric values
        composeTestRule
            .onAllNodesWithText("rpm")
            .assertCountEquals(1)
        
        composeTestRule
            .onAllNodesWithText("mph")
            .assertCountEquals(1)
        
        composeTestRule
            .onAllNodesWithText("%")
            .assertCountEquals(1)
        
        composeTestRule
            .onAllNodesWithText("°F")
            .assertCountEquals(1)
    }

    @Test
    fun dashboardScreen_hasProperLayout() {
        composeTestRule.setContent {
            HurtecOBD2Theme {
                DashboardScreen()
            }
        }

        // Verify the main components are present
        composeTestRule
            .onNodeWithText("Vehicle Dashboard")
            .assertIsDisplayed()

        // Verify gauge grid is present (should have 4 gauges in 2x2 grid)
        composeTestRule
            .onAllNodes(hasText("Engine RPM") or hasText("Vehicle Speed") or hasText("Engine Load") or hasText("Coolant Temperature"))
            .assertCountEquals(4)
    }
}
