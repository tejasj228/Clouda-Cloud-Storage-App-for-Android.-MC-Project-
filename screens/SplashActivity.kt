package com.mcp.oogabooga.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2000L)
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            navController.navigate("main") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF343434), // Dark gray from the gradient stops
                        Color(0xFF282828),
                        Color(0xFF151515)// Darker gray from the gradient stops
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CloudUpload,
                contentDescription = "App Logo",
                modifier = Modifier.size(130.dp),
                tint = Color(0xFF00AB67) // Changed to specific green color
            )
            Text(
                text = "Clouda",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = senFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 60.sp
                ),
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFF00AB67) // Changed to specific green color
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(16.dp)
                    .size(75.dp), // Increased size of loading circle
                color = Color(0xFF00AB67), // Changed to specific green color
                strokeWidth = 7.dp // Optional: slightly increased stroke width for visibility
            )
        }
    }
}