package com.example.vivora

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _artworks = MutableStateFlow<List<Artwork>>(emptyList())
    val artworks: StateFlow<List<Artwork>> = _artworks.asStateFlow()

    private val _favorites = MutableStateFlow<List<Artwork>>(emptyList())
    val favorites: StateFlow<List<Artwork>> = _favorites.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private var profileListener: ListenerRegistration? = null
    private var followListener: ListenerRegistration? = null

    fun loadProfile(uid: String) {
        profileListener?.remove()
        followListener?.remove()

        // Real-time Profile Info (to see followersCount change)
        profileListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                _profile.value = snapshot?.toObject(UserProfile::class.java)
            }
        
        // Check if current user is following this artist
        val currentUid = auth.currentUser?.uid
        if (currentUid != null && currentUid != uid) {
            followListener = db.collection("users").document(currentUid)
                .collection("following").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    _isFollowing.value = snapshot?.exists() == true
                }
        }

        // Load User's Artworks
        db.collection("artworks")
            .whereEqualTo("artistId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _artworks.value = snapshot.toObjects(Artwork::class.java)
                }
            }

        // Load User's Favorites
        db.collection("users").document(uid).collection("savedArtworks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val favIds = snapshot.documents.map { it.id }
                    if (favIds.isEmpty()) {
                        _favorites.value = emptyList()
                    } else {
                        db.collection("artworks")
                            .whereIn("id", favIds)
                            .get()
                            .addOnSuccessListener { artSnapshot ->
                                _favorites.value = artSnapshot.toObjects(Artwork::class.java)
                            }
                    }
                }
            }
    }

    fun toggleFollow(targetUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid == targetUid) return

        val followingRef = db.collection("users").document(currentUid).collection("following").document(targetUid)
        val followersRef = db.collection("users").document(targetUid).collection("followers").document(currentUid)
        val targetUserDoc = db.collection("users").document(targetUid)
        val currentUserDoc = db.collection("users").document(currentUid)

        if (_isFollowing.value) {
            // Unfollow
            followingRef.delete()
            followersRef.delete()
            targetUserDoc.update("followersCount", FieldValue.increment(-1))
            currentUserDoc.update("followingCount", FieldValue.increment(-1))
        } else {
            // Follow
            val data = mapOf("timestamp" to FieldValue.serverTimestamp())
            followingRef.set(data)
            followersRef.set(data)
            targetUserDoc.update("followersCount", FieldValue.increment(1))
            currentUserDoc.update("followingCount", FieldValue.increment(1))
        }
    }

    override fun onCleared() {
        profileListener?.remove()
        followListener?.remove()
    }
}
