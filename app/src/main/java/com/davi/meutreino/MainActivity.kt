package com.davi.meutreino

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = Store(applicationContext)
        setContent {
            AppTheme {
                App(store)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        store.checkNewDay()
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFFFF8A50))
        else -> lightColorScheme(primary = Color(0xFFD84315))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun App(store: Store) {
    var aba by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = aba == 0,
                    onClick = { aba = 0 },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Treinos") },
                )
                NavigationBarItem(
                    selected = aba == 1,
                    onClick = { aba = 1 },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    label = { Text("Próximo") },
                )
                NavigationBarItem(
                    selected = aba == 2,
                    onClick = { aba = 2 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Corpo") },
                )
                NavigationBarItem(
                    selected = aba == 3,
                    onClick = { aba = 3 },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text("Dieta") },
                )
            }
        }
    ) { padding ->
        when (aba) {
            0 -> TreinosScreen(store, padding)
            1 -> ProximoScreen(store, padding)
            2 -> CorpoScreen(store, padding)
            else -> DietaScreen(store, padding)
        }
    }
}
