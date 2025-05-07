package com.mcp.oogabooga.viewmodel

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<Boolean?>(null)
    val loginState = _loginState.asStateFlow()
    private val _isSignInSuccessful = MutableStateFlow<Boolean?>(null)
    val isSignInSuccessful = _isSignInSuccessful.asStateFlow()

    fun signIn(activity: Activity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            Log.d("LoginViewModel", "Starting sign-in process...")

            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("626130300458-tvjod32q8ohn9uin2rfaef15ek2qfrb4.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                Log.d("LoginViewModel", "Requesting credentials...")
                val result: GetCredentialResponse =
                    credentialManager.getCredential(activity, request)

                Log.d("LoginViewModel", "Credential result received")

                val credential = result.credential
                val googleCredential =
                    com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)

                Log.d("LoginViewModel", "Google ID Token: ${googleCredential.idToken}")

                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)

                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("LoginViewModel", "Firebase Sign-In SUCCESS")
                            onSuccess()
                        } else {
                            Log.e("LoginViewModel", "Firebase Sign-In FAILED: ${task.exception?.message}")
                        }
                        _loginState.value = task.isSuccessful
                    }

            } catch (e: GetCredentialException) {
                Log.e("LoginViewModel", "GetCredentialException: ${e.message}")
                e.printStackTrace()
                _loginState.value = false
            } catch (e: Exception) {
                Log.e("LoginViewModel", "General Exception: ${e.message}")
                e.printStackTrace()
                _loginState.value = false
            }
        }
    }
}
