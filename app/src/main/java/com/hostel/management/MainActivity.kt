package com.hostel.management

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.hostel.management.navigation.NavGraph


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HostelTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700), // Vibrant Metallic Gold
    secondary = Color(0xFFE5C158), // Bright Warm Gold
    tertiary = Color(0xFFF3E5AB), // Cream Gold Accent
    background = Color(0xFF12100C), // Deep Obsidian Gold
    surface = Color(0xFF1C1812), // Dark Amber Gold Surface
    onPrimary = Color(0xFF12100C),
    onSecondary = Color(0xFF12100C),
    onBackground = Color(0xFFFAF6F0), // Champagne White
    onSurface = Color(0xFFFAF6F0), // Champagne White
    primaryContainer = Color(0xFF3B300A), // Deep Gold Container
    onPrimaryContainer = Color(0xFFFFF8E0),
    secondaryContainer = Color(0xFF2E250A),
    onSecondaryContainer = Color(0xFFFFF0CB),
    surfaceVariant = Color(0xFF282219),
    onSurfaceVariant = Color(0xFFE8DCC4),
    outline = Color(0xFF8A7333)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD4AF37), // Metallic Gold
    secondary = Color(0xFFB8860B), // Dark Goldenrod
    tertiary = Color(0xFF967117), // Rich Bronze Gold
    background = Color(0xFFFAFAFA), // Porcelain White
    surface = Color(0xFFFFFFFF), // Pristine Pure White
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F1A0E), // Warm Deep Charcoal
    onSurface = Color(0xFF1F1A0E), // Warm Deep Charcoal
    primaryContainer = Color(0xFFFFF7DB), // Warm Ivory Gold Tint
    onPrimaryContainer = Color(0xFF524000), // Deep Golden Text
    secondaryContainer = Color(0xFFFFF0CB), // Light Champagne Gold
    onSecondaryContainer = Color(0xFF423300),
    surfaceVariant = Color(0xFFF7F2E7), // Pearl Champagne White
    onSurfaceVariant = Color(0xFF4A4131),
    outline = Color(0xFFD6BE8A) // Soft Gold Border Accent
)

@Composable
fun HostelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

