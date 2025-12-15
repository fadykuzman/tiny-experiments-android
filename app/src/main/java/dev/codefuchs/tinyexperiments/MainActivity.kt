package dev.codefuchs.tinyexperiments

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.codefuchs.tinyexperiments.presentation.auth.LoginScreen
import dev.codefuchs.tinyexperiments.presentation.home.HomeScreen
import dev.codefuchs.tinyexperiments.ui.theme.TinyExperimentsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TinyExperimentsTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            onSignInSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") {inclusive = true}
                                }
                            }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            onSignOutSuccess = {
                                navController.navigate("login") {
                                    popUpTo("home") {inclusive = true}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TinyExperimentsTheme {
        Greeting("Android")
    }
}