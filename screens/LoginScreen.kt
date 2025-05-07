package com.mcp.oogabooga.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mcp.oogabooga.R
import com.mcp.oogabooga.supabase.SupabaseStorageHelper
import com.mcp.oogabooga.viewmodel.LoginViewModel

// Define the Sen Font Family
val senFontFamily = FontFamily(
    Font(R.font.sen_regular, FontWeight.Normal),
    Font(R.font.sen_bold, FontWeight.Bold),
    Font(R.font.sen_semibold, FontWeight.SemiBold),
    Font(R.font.sen_medium, FontWeight.Medium),
    Font(R.font.sen_extrabold, FontWeight.ExtraBold)
)

@SuppressLint("ContextCastToActivity")
@Composable
fun LoginScreen(navController: NavController) {
    val activity = LocalContext.current as Activity
    val loginViewModel: LoginViewModel = viewModel()

    val loginState by loginViewModel.loginState.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(loginState) {
        when (loginState) {
            true -> {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
            false -> {
                Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
            }
            null -> Unit
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Title
            Text(
                text = "Clouda",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = senFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                fontSize = 60.sp,
                color = Color.Black,
                modifier = Modifier.padding(top = 128.dp).padding(bottom = 3.dp)
            )

            // Central Cloud Storage Logo
            Image(
                painter = painterResource(id = R.drawable.cloud_storage_logo),
                contentDescription = "Cloud Storage Logo",
                modifier = Modifier
                    .size(250.dp)
                    .padding(bottom = 24.dp),
                contentScale = ContentScale.Fit
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Welcome Text
                Text(
                    text = "Welcome Aboard!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = senFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "Fast, smart and reliable cloud storage! 🚀",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = senFontFamily,
                        fontSize = 14.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )


                Button(
                    onClick = {
                        loginViewModel.signIn(activity) {
                            SupabaseStorageHelper.getUploadedFiles { files -> var fileList = files }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005A45),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.google_logo),
                            contentDescription = "Google logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Continue with Google",
                            modifier = Modifier.padding(start = 12.dp),
                            fontSize = 16.sp,
                            fontFamily = senFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "OR",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = senFontFamily
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Apple Sign-In Button
                Button(
                    onClick = { /* Will implement later */ },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005A45),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.apple_logo),
                            contentDescription = "Apple logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Continue with Apple",
                            modifier = Modifier.padding(start = 12.dp),
                            fontSize = 16.sp,
                            fontFamily = senFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


