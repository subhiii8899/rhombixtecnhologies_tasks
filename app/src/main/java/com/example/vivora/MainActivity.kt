package com.example.vivora


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.vivora.ui.theme.VivoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VivoraTheme {
                VivoraMainApp()
            }
        }
    }
}

@Composable
fun VivoraMainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute !in listOf("splash", "login", "signup", "profile_setup")) {
                VivoraBottomBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen(
                    onFinished = {
                        val nextRoute = if (FirebaseAuth.getInstance().currentUser != null) "explore" else "login"
                        navController.navigate(nextRoute) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    navController.navigate("explore") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("profile_setup") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                navController.navigate("explore") // Fallback
                            }
                    },
                    onNavigateToSignup = { navController.navigate("signup") }
                )
            }
            composable("signup") {
                SignupScreen(
                    onSignupSuccess = {
                        navController.navigate("profile_setup") {
                            popUpTo("signup") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }
            composable("profile_setup") {
                ProfileSetupScreen(
                    onSetupComplete = {
                        navController.navigate("explore") {
                            popUpTo("profile_setup") { inclusive = true }
                        }
                    }
                )
            }
            composable("explore") {
                ExploreScreen(onArtworkClick = { id -> navController.navigate("artwork_detail/$id") })
            }
            composable("upload") {
                UploadArtworkScreen(onUploaded = { navController.navigate("explore") })
            }
            composable("profile") {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                ArtistProfileScreen(
                    uid = uid,
                    onArtworkClick = { id -> navController.navigate("artwork_detail/$id") },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onEditProfile = {
                        navController.navigate("profile_setup")
                    }
                )
            }
            composable("profile/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                ArtistProfileScreen(
                    uid = userId,
                    onArtworkClick = { id -> navController.navigate("artwork_detail/$id") },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onEditProfile = {
                        navController.navigate("profile_setup")
                    }
                )
            }
            composable("artwork_detail/{artworkId}") { backStackEntry ->
                val artworkId = backStackEntry.arguments?.getString("artworkId") ?: ""
                ArtworkDetailScreen(
                    artworkId = artworkId,
                    onBack = { navController.popBackStack() },
                    onArtistClick = { artistId -> 
                        navController.navigate("profile/$artistId")
                    }
                )
            }
        }
    }
}

@Composable
fun VivoraBottomBar(navController: NavHostController) {
    val items = listOf(
        Triple("explore", "Explore", Icons.Default.Home),
        Triple("upload", "Upload", Icons.Default.AddCircle),
        Triple("profile", "Profile", Icons.Default.AccountCircle)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
