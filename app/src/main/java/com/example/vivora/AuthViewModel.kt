package com.example.vivora

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Please fill in all fields"
            return
        }
        _isLoading.value = true
        _error.value = null
        
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    _error.value = task.exception?.localizedMessage ?: "Login failed"
                }
            }
    }

    fun signup(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Please fill in all fields"
            return
        }
        _isLoading.value = true
        _error.value = null

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    _error.value = task.exception?.localizedMessage ?: "Signup failed"
                }
            }
    }

    fun signupWithGoogle(credential: com.google.firebase.auth.AuthCredential, onResult: (Boolean) -> Unit) {
        _isLoading.value = true
        _error.value = null
        
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                        _isLoading.value = false
                        onResult(!doc.exists()) // True if new user (needs setup)
                    }.addOnFailureListener {
                        _isLoading.value = false
                        onResult(true) // Default to setup if check fails
                    }
                } else {
                    _isLoading.value = false
                    _error.value = task.exception?.localizedMessage ?: "Google Sign-In failed"
                }
            }
    }
    
    fun clearError() {
        _error.value = null
    }
}
