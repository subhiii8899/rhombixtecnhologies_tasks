package com.example.vivora

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.vivora.ui.theme.MetallicGreen
import com.example.vivora.ui.theme.MidnightBlue
import com.example.vivora.ui.theme.TextPrimary
import com.example.vivora.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistProfileScreen(
    uid: String,
    onArtworkClick: (String) -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: ProfileViewModel = viewModel(),
    artworkViewModel: ArtworkViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val artworks by viewModel.artworks.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val likedIds by artworkViewModel.likedArtworkIds.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    val isOwner = uid == currentUserId
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    var selectedArtworkForOptions by remember { mutableStateOf<Artwork?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    var editId by remember { mutableStateOf("") }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) viewModel.loadProfile(uid)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VivoraBackgroundGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Settings
            Box(modifier = Modifier.fillMaxWidth()) {
                if (isOwner) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MidnightBlue)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Profile", color = TextPrimary) },
                            onClick = {
                                showMenu = false
                                onEditProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About Vivora", color = TextPrimary) },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            }
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        DropdownMenuItem(
                            text = { Text("Logout", color = Color.Red) },
                            onClick = {
                                showMenu = false
                                FirebaseAuth.getInstance().signOut()
                                onLogout()
                            }
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(VivoraAccentGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile?.profileImageUrl.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            tint = TextPrimary.copy(alpha = 0.6f)
                        )
                    } else {
                        AsyncImage(
                            model = profile?.profileImageUrl,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = profile?.username?.ifBlank { "Artist" } ?: "Artist",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = profile?.bio ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    ProfileStat(count = profile?.followersCount ?: 0, label = "Followers")
                    ProfileStat(count = profile?.followingCount ?: 0, label = "Following")
                    ProfileStat(count = artworks.size, label = "Artworks")
                }
            }

            if (isOwner) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MetallicGreen,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Posts") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Favorites") }
                    )
                }
            } else {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Posts",
                        color = MetallicGreen,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                // Force selectedTab to 0 for non-owners
                SideEffect { selectedTab = 0 }
            }

            val currentList = if (selectedTab == 0) artworks else favorites

            if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedTab == 0) "No artworks posted yet." else "No favorites saved yet.",
                        color = TextSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentList) { artwork ->
                        val isLiked = likedIds.contains(artwork.id)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = artwork.imageUrl,
                                contentDescription = artwork.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        onClick = { onArtworkClick(artwork.id) },
                                        onLongClick = {
                                            if (artwork.artistId == currentUserId) {
                                                selectedArtworkForOptions = artwork
                                            }
                                        }
                                    )
                            )
                            
                            if (artwork.likeCount > 0) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = if (isLiked) Color.Red else Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("${artwork.likeCount}", color = Color.White, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Options Selection Dialog
        if (selectedArtworkForOptions != null) {
            AlertDialog(
                onDismissRequest = { selectedArtworkForOptions = null },
                title = { Text("Artwork Options") },
                text = { Text("What would you like to do with '${selectedArtworkForOptions?.title}'?") },
                confirmButton = {
                    TextButton(onClick = { 
                        showDeleteConfirm = true
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        editId = selectedArtworkForOptions?.id ?: ""
                        editTitle = selectedArtworkForOptions?.title ?: ""
                        editDescription = selectedArtworkForOptions?.description ?: ""
                        showEditDialog = true
                        selectedArtworkForOptions = null 
                    }) {
                        Text("Edit", color = MetallicGreen)
                    }
                }
            )
        }

        // Delete Confirmation
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirm = false 
                    selectedArtworkForOptions = null
                },
                title = { Text("Confirm Delete") },
                text = { Text("Are you sure you want to delete this artwork?") },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedArtworkForOptions?.let { 
                                artworkViewModel.deleteArtwork(it.id, onDone = {
                                    showDeleteConfirm = false
                                    selectedArtworkForOptions = null
                                }, onError = {})
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showDeleteConfirm = false 
                        selectedArtworkForOptions = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Artwork") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            artworkViewModel.updateArtwork(editId, editTitle, editDescription, onDone = {
                                showEditDialog = false
                            }, onError = {})
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGreen)
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About Vivora") },
                text = {
                    Text(
                        "Vivora is a premium platform for artists to showcase their creativity. " +
                        "Share your paintings, photography, and digital art with a global community.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Close", color = MetallicGreen)
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileStat(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
