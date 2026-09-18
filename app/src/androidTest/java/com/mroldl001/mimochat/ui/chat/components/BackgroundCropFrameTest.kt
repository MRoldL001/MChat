package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class BackgroundCropFrameTest {
    @get:Rule val compose = createComposeRule()

    @Test fun phoneHasNoPermanentSidebar() = checkFrame(BackgroundWindow(393, 851, topInset = 30, bottomInset = 24), false)

    @Test fun tabletRestoredFromSmallWindowUsesMessagePane() = checkFrame(BackgroundWindow(1280, 800, topInset = 24, bottomInset = 24), true)

    @Test fun belowTabletBreakpointHasNoSidebar() = checkFrame(BackgroundWindow(599, 1000), false)

    @Test fun tabletBreakpointIncludesSidebar() = checkFrame(BackgroundWindow(600, 1000), true)

    @Test fun sideSystemBarAndLargeFontAreMeasured() = checkFrame(BackgroundWindow(1200, 900, rightInset = 48, topInset = 24), true, 1.7f)

    private fun checkFrame(window: BackgroundWindow, expanded: Boolean, fontScale: Float = 1f) {
        var messageSize = IntSize.Zero
        var largePreview = IntSize.Zero
        var smallPreview = IntSize.Zero
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MaterialTheme {
                    Layout(content = {
                        ReferenceChat(window, expanded) { messageSize = it }
                        BackgroundCropFrame(window = window) {
                            Box(Modifier.fillMaxSize().onSizeChanged { largePreview = it })
                        }
                        BackgroundCropFrame(window = window) {
                            Box(Modifier.fillMaxSize().onSizeChanged { smallPreview = it })
                        }
                    }) { children, constraints ->
                        val reference = children[0].measure(Constraints.fixed(window.width, window.height))
                        val large = children[1].measure(Constraints.fixed(700, 700))
                        val small = children[2].measure(Constraints.fixed(240, 180))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            reference.place(0, 0)
                            large.place(0, 0)
                            small.place(0, 0)
                        }
                    }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(window.width - (if (expanded) 280 else 0) - window.leftInset - window.rightInset, messageSize.width)
            assertTrue(messageSize.height in 1 until window.height)
            for (preview in listOf(largePreview, smallPreview)) {
                assertTrue(preview.width > 0 && preview.height > 0)
                val expectedHeight = preview.width.toFloat() * messageSize.height / messageSize.width
                assertTrue("Crop must match the actual message area: $messageSize, preview: $preview",
                    abs(preview.height - expectedHeight) <= 2f)
            }
        }
    }
}

// Use the actual Scaffold/drawer sizing rather than reproducing crop arithmetic as the oracle.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceChat(window: BackgroundWindow, expanded: Boolean, onMessageSize: (IntSize) -> Unit) {
    val chat: @Composable () -> Unit = {
        Scaffold(
            contentWindowInsets = WindowInsets(window.leftInset, window.topInset, window.rightInset, window.bottomInset),
            topBar = {
                TopAppBar(title = {}, windowInsets = WindowInsets(window.leftInset, window.topInset, window.rightInset, 0))
            },
            bottomBar = {
                Column(Modifier.absolutePadding(left = window.leftInset.dp, right = window.rightInset.dp, bottom = window.bottomInset.dp)) {
                    SkillToggleBar(false, false, null, false, {}, {}, {},
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
                    InputBar(onSendMessage = {}, onStopGenerating = {})
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Box(Modifier.fillMaxSize().onSizeChanged(onMessageSize))
            }
        }
    }
    if (expanded) {
        PermanentNavigationDrawer(drawerContent = {
            PermanentDrawerSheet(Modifier.width(ChatSidebarWidth)) { }
        }, content = chat)
    } else {
        chat()
    }
}
