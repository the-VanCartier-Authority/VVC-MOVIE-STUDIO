package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberBackground
import com.example.ui.components.CyberHeader
import com.example.ui.components.VideoPlayerModal
import com.example.ui.screens.DiagnosticScreen
import com.example.ui.screens.SequenceScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.TestLabScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.RetroConsoleFontFamily
import com.example.ui.theme.VvcMovieStudioTheme
import com.example.viewmodel.StudioTab
import com.example.viewmodel.VvcStudioViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VvcStudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VvcMovieStudioTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: VvcStudioViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle back button when on sub-tabs
    BackHandler(enabled = uiState.activeTab != StudioTab.STUDIO) {
        viewModel.setTab(StudioTab.STUDIO)
    }

    CyberBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CyberHeader(
                    title = "VVC MOVIE STUDIO",
                    subtitle = "MOTOR VANGUARDISTA DE REPARACIÓN Y CONVERSIÓN IPC 360 HOME"
                )
            },
            bottomBar = {
                VvcBottomNavBar(
                    currentTab = uiState.activeTab,
                    onSelectTab = { viewModel.setTab(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = uiState.activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_navigation"
                ) { tab ->
                    when (tab) {
                        StudioTab.STUDIO -> StudioScreen(viewModel = viewModel)
                        StudioTab.DIAGNOSTIC -> DiagnosticScreen(viewModel = viewModel)
                        StudioTab.SEQUENCE -> SequenceScreen(viewModel = viewModel)
                        StudioTab.VAULT -> VaultScreen(viewModel = viewModel)
                        StudioTab.TEST_LAB -> TestLabScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Modal Video Player
        if (uiState.activePlayerVideo != null) {
            VideoPlayerModal(
                record = uiState.activePlayerVideo!!,
                onDismiss = { viewModel.setActivePlayerVideo(null) },
                onShare = { viewModel.shareVideo(context, uiState.activePlayerVideo!!) }
            )
        }
    }
}

@Composable
fun VvcBottomNavBar(
    currentTab: StudioTab,
    onSelectTab: (StudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavigationItem(StudioTab.STUDIO, "REPARAR", Icons.Default.Build),
        NavigationItem(StudioTab.DIAGNOSTIC, "DIAGNÓSTICO", Icons.Default.Analytics),
        NavigationItem(StudioTab.SEQUENCE, "SECUENCIA", Icons.Default.Merge),
        NavigationItem(StudioTab.VAULT, "BÓVEDA", Icons.Default.Folder),
        NavigationItem(StudioTab.TEST_LAB, "LAB TEST", Icons.Default.Science)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(ObsidianBlack)
            .border(1.dp, ObsidianBorder)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { item ->
                val isSelected = currentTab == item.tab
                val activeColor = if (item.tab == StudioTab.STUDIO || item.tab == StudioTab.SEQUENCE) NeonMagenta else NeonEmerald

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectTab(item.tab) }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("nav_tab_${item.tab.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) activeColor else Color(0xFF6B7280),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.label,
                        fontFamily = RetroConsoleFontFamily,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) activeColor else Color(0xFF6B7280),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private data class NavigationItem(
    val tab: StudioTab,
    val label: String,
    val icon: ImageVector
)
