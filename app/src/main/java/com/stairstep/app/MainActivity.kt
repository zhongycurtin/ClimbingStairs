package com.stairstep.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stairstep.app.ui.screens.HomeScreen
import com.stairstep.app.ui.screens.StatsScreen
import com.stairstep.app.ui.theme.MintEmerald
import com.stairstep.app.ui.theme.StairStepTheme
import com.stairstep.app.ui.viewmodel.ClimbViewModel
import com.stairstep.app.ui.viewmodel.StatsViewModel

class MainActivity : ComponentActivity() {

    private val climbViewModel: ClimbViewModel by viewModels()
    private val statsViewModel: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StairStepTheme {
                MainAppContainer(
                    climbViewModel = climbViewModel,
                    statsViewModel = statsViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContainer(
    climbViewModel: ClimbViewModel,
    statsViewModel: StatsViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .height(64.dp)
                        .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.08f))
                        .clip(RoundedCornerShape(32.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsRun,
                                contentDescription = "今日打卡",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "今日打卡",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MintEmerald,
                            selectedTextColor = MintEmerald,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            indicatorColor = MintEmerald.copy(alpha = 0.12f)
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = "趋势成就",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "趋势成就",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MintEmerald,
                            selectedTextColor = MintEmerald,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            indicatorColor = MintEmerald.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel = climbViewModel)
                1 -> StatsScreen(viewModel = statsViewModel)
            }
        }
    }
}
