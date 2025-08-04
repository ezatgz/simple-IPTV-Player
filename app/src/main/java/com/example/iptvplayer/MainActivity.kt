// --- 推荐文件: MainActivity.kt (主入口) ---
package com.example.iptvplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.iptvplayer.data.model.SettingsManager
import com.example.iptvplayer.data.model.ThemeMode
import com.example.iptvplayer.ui.home.HomeScreen
import com.example.iptvplayer.ui.player.PlayerScreen
import com.example.iptvplayer.ui.settings.SettingsScreen
import com.example.iptvplayer.ui.theme.IPTVPlayerTheme
import com.example.iptvplayer.viewmodel.ChannelListViewModel
import com.example.iptvplayer.viewmodel.MainViewModel
import com.example.iptvplayer.ui.channel_list.ChannelListScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import android.app.Application
import com.example.iptvplayer.data.model.Channel
import com.example.iptvplayer.data.model.EpgProgram
import com.example.iptvplayer.data.model.AppStateManager
import java.net.URLDecoder
import com.example.iptvplayer.viewmodel.PlayerViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen



class MainActivity : ComponentActivity() {
    // 创建全局的AppStateManager实例
    private lateinit var appStateManager: AppStateManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 super.onCreate() 之前调用，这是启用新版启动画面的关键
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // 初始化AppStateManager
        val settingsManager = SettingsManager(this)
        appStateManager = AppStateManager(settingsManager)
        
        setContent {
            IPTVPlayerApp(appStateManager)
        }
    }
}

@Composable
fun IPTVPlayerApp(appStateManager: AppStateManager) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val settingsManager = SettingsManager(context)
    val themeMode by settingsManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    IPTVPlayerTheme(themeMode) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    viewModel = mainViewModel,
                    onPlaylistClick = { playlistId, playlistName ->
                        val encodedName = URLEncoder.encode(playlistName, StandardCharsets.UTF_8.toString())
                        navController.navigate("channels/$playlistId/$encodedName")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                "channels/{playlistId}/{playlistName}",
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.IntType },
                    navArgument("playlistName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getInt("playlistId") ?: 0
                val playlistName = backStackEntry.arguments?.getString("playlistName") ?: "频道"
                val context = LocalContext.current // Get the current context

                // Simple ViewModel factory
                //val channelViewModel: ChannelListViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                //    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                //        return ChannelListViewModel(application = getApplication(), playlistId = playlistId) as T
                //    }
                //})

                val channelViewModel: ChannelListViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(ChannelListViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return ChannelListViewModel(
                                    application = context.applicationContext as Application, // Get application context
                                    playlistId = playlistId
                                ) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                )

                ChannelListScreen(
                    playlistName = playlistName,
                    viewModel = channelViewModel,
                    onChannelClick = { channel ->  // 修改为接收Channel对象
                        // 传递频道ID到PlayerScreen
                        navController.navigate("player/${channel.id}")
                    },
                    onBack = { navController.popBackStack() },
                    appStateManager = appStateManager
                )
            }
            
            composable(
                "player/{channelId}",
                arguments = listOf(navArgument("channelId") { type = NavType.IntType })
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getInt("channelId") ?: 0
                val playerViewModel: PlayerViewModel = viewModel()
                
                PlayerScreen(
                    channelId = channelId, 
                    onBack = { navController.popBackStack() },
                    onSwitchChannel = { newChannelId -> 
                        // 直接在ViewModel中切换频道，不重新导航
                        playerViewModel.switchToChannel(newChannelId)
                    },
                    viewModel = playerViewModel
                )
            }
        }
    }
}