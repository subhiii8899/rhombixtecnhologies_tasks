package com.example.vivora

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ArtworkViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _exploreFeed = MutableStateFlow<List<Artwork>>(emptyList())
    val exploreFeed: StateFlow<List<Artwork>> = _exploreFeed.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortByLikes = MutableStateFlow(true) // Default to sorting by likes
    val sortByLikes: StateFlow<Boolean> = _sortByLikes.asStateFlow()

    private val _likedArtworkIds = MutableStateFlow<Set<String>>(emptySet())
    val likedArtworkIds: StateFlow<Set<String>> = _likedArtworkIds.asStateFlow()

    private val _savedArtworkIds = MutableStateFlow<Set<String>>(emptySet())
    val savedArtworkIds: StateFlow<Set<String>> = _savedArtworkIds.asStateFlow()

    private var feedListener: ListenerRegistration? = null
    private var likesListener: ListenerRegistration? = null
    private var savesListener: ListenerRegistration? = null

    val categories = listOf("All", "Painting", "Photography", "Illustration", "Wallpaper", "Oil Painting", "Sketch")

    init {
        loadExploreFeed("All", true)
        
        // Use a listener to detect when a user logs in
        auth.addAuthStateListener { 
            observeUserActions() 
        }
    }

    private fun observeUserActions() {
        val uid = auth.currentUser?.uid
        
        // Clean up old listeners if they exist
        likesListener?.remove()
        savesListener?.remove()
        
        if (uid == null) {
            _likedArtworkIds.value = emptySet()
            _savedArtworkIds.value = emptySet()
            return
        }
        
        // Observe likes
        likesListener = db.collection("users").document(uid).collection("likedArtworks")
            .addSnapshotListener { snapshot, _ ->
                _likedArtworkIds.value = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
            }

        // Observe saves
        savesListener = db.collection("users").document(uid).collection("savedArtworks")
            .addSnapshotListener { snapshot, _ ->
                _savedArtworkIds.value = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
            }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        loadExploreFeed(category, _sortByLikes.value)
    }

    fun toggleSortOrder() {
        val newSort = !_sortByLikes.value
        _sortByLikes.value = newSort
        loadExploreFeed(_selectedCategory.value, newSort)
    }

    private fun loadExploreFeed(category: String, mostPopular: Boolean) {
        feedListener?.remove()
        
        val primaryField = if (mostPopular) "likeCount" else "timestamp"
        
        var query = db.collection("artworks")
            .orderBy(primaryField, Query.Direction.DESCENDING)
            
        // If sorting by likes, add timestamp as secondary sort
        if (mostPopular) {
            query = query.orderBy("timestamp", Query.Direction.DESCENDING)
        }

        if (category != "All") {
            query = query.whereEqualTo("category", category)
        }

        feedListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                _exploreFeed.value = snapshot.toObjects(Artwork::class.java)
            }
        }
    }

    fun toggleLike(artworkId: String) {
        val uid = auth.currentUser?.uid ?: return
        val isLiked = _likedArtworkIds.value.contains(artworkId)
        
        // 1. Optimistically update local state for instant feedback
        val currentFeed = _exploreFeed.value.toMutableList()
        val index = currentFeed.indexOfFirst { it.id == artworkId }
        if (index != -1) {
            val artwork = currentFeed[index]
            val newLikeCount = if (isLiked) (artwork.likeCount - 1).coerceAtLeast(0) else artwork.likeCount + 1
            currentFeed[index] = artwork.copy(likeCount = newLikeCount)
            _exploreFeed.value = currentFeed
        }

        // 2. Perform actual Database update
        val userLikeRef = db.collection("users").document(uid).collection("likedArtworks").document(artworkId)
        val artworkRef = db.collection("artworks").document(artworkId)

        if (isLiked) {
            userLikeRef.delete()
            artworkRef.update("likeCount", FieldValue.increment(-1))
        } else {
            userLikeRef.set(mapOf("timestamp" to FieldValue.serverTimestamp()))
            artworkRef.update("likeCount", FieldValue.increment(1))
        }
    }

    fun toggleSave(artworkId: String) {
        val uid = auth.currentUser?.uid ?: return
        val isSaved = _savedArtworkIds.value.contains(artworkId)
        val userSaveRef = db.collection("users").document(uid).collection("savedArtworks").document(artworkId)

        if (isSaved) {
            userSaveRef.delete()
        } else {
            userSaveRef.set(mapOf("timestamp" to FieldValue.serverTimestamp()))
        }
    }

    fun uploadArtwork(
        context: Context,
        imageUri: Uri,
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        _isUploading.value = true
        
        MediaManager.get().upload(imageUri)
            .option("unsigned", true)
            .option("upload_preset", "Vivora_preset")
            .option("resource_type", "image")
            .option("folder", "artworks")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as String
                    
                    val newArtwork = Artwork(
                        id = requestId,
                        title = title,
                        description = description,
                        imageUrl = imageUrl,
                        artistId = userId,
                        category = category,
                        tags = tags
                    )

                    db.collection("artworks").document(requestId)
                        .set(newArtwork)
                        .addOnSuccessListener {
                            _isUploading.value = false
                            onDone()
                        }
                        .addOnFailureListener {
                            _isUploading.value = false
                            onError(it.localizedMessage ?: "Firestore error")
                        }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    _isUploading.value = false
                    onError(error.description)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    fun deleteArtwork(artworkId: String, onDone: () -> Unit, onError: (String) -> Unit) {
        db.collection("artworks").document(artworkId).delete()
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Delete failed") }
    }

    fun updateArtwork(
        artworkId: String,
        newTitle: String,
        newDescription: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("artworks").document(artworkId)
            .update(mapOf(
                "title" to newTitle,
                "description" to newDescription
            ))
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { onError(it.localizedMessage ?: "Update failed") }
    }

    override fun onCleared() {
        feedListener?.remove()
        likesListener?.remove()
        savesListener?.remove()
    }
}
