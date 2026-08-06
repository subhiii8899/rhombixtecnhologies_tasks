package com.example.vivora

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.vivora.ui.theme.MetallicGreen
import com.example.vivora.ui.theme.TextPrimary
import com.example.vivora.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scrollState = rememberScrollState()

    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageUrl by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch current data to prefill
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    username = doc.getString("username") ?: ""
                    bio = doc.getString("bio") ?: ""
                    currentImageUrl = doc.getString("profileImageUrl") ?: ""
                } else {
                    // Default for first time signup
                    username = auth.currentUser?.displayName ?: ""
                    currentImageUrl = auth.currentUser?.photoUrl?.toString() ?: ""
                }
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VivoraBackgroundGradient)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "Complete Your Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            "Tell the community who you are",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        // Profile Image Picker
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable {
                    pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val imageSource = when {
                selectedImageUri != null -> selectedImageUri
                currentImageUrl.isNotBlank() -> currentImageUrl
                else -> null
            }

            if (imageSource != null) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(), // Removed padding so it covers full circle
                    tint = TextSecondary.copy(alpha = 0.5f)
                )
            }
            
            // Camera Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { if (it.length <= 20) username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .glow(borderRadius = 12.dp, blurRadius = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MetallicGreen,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 100) bio = it },
            label = { Text("Bio (max 100 characters)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .glow(borderRadius = 12.dp, blurRadius = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MetallicGreen,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            supportingText = {
                Text(
                    "${bio.length}/100",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    color = TextSecondary
                )
            }
        )

        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (username.isBlank()) {
                    errorMessage = "Username is required"
                    return@Button
                }
                isSaving = true
                errorMessage = null

                val uid = auth.currentUser?.uid ?: return@Button

                fun performSave(imageUrl: String) {
                    val updates = mapOf(
                        "uid" to uid,
                        "username" to username,
                        "bio" to bio,
                        "profileImageUrl" to imageUrl
                    )
                    db.collection("users").document(uid).update(updates)
                        .addOnSuccessListener {
                            isSaving = false
                            onSetupComplete()
                        }
                        .addOnFailureListener { e ->
                            // If document doesn't exist (first time), use set instead
                            db.collection("users").document(uid).set(updates)
                                .addOnSuccessListener {
                                    isSaving = false
                                    onSetupComplete()
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                    errorMessage = it.localizedMessage
                                }
                        }
                }

                if (selectedImageUri != null) {
                    MediaManager.get().upload(selectedImageUri!!)
                        .option("unsigned", true)
                        .option("upload_preset", "Vivora_preset")
                        .option("folder", "profiles")
                        .callback(object : UploadCallback {
                            override fun onStart(requestId: String) {}
                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                performSave(resultData["secure_url"] as String)
                            }
                            override fun onError(requestId: String, error: ErrorInfo) {
                                isSaving = false
                                errorMessage = "Image Upload Failed: ${error.description}"
                            }
                            override fun onReschedule(requestId: String, error: ErrorInfo) {}
                        }).dispatch()
                } else {
                    performSave(currentImageUrl)
                }
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glow(color = MetallicGreen, blurRadius = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MetallicGreen)
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Complete Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
