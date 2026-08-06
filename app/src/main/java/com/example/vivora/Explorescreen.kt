package com.example.vivora

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.NewReleases
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
import com.example.vivora.ui.theme.TextPrimary
import com.example.vivora.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    onArtworkClick: (String) -> Unit,
    viewModel: ArtworkViewModel = viewModel()
) {
    val artworks by viewModel.exploreFeed.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val likedIds by viewModel.likedArtworkIds.collectAsState()
    val savedIds by viewModel.savedArtworkIds.collectAsState()
    val isSortedByLikes by viewModel.sortByLikes.collectAsState()
    
    var selectedArtworkForOptions by remember { mutableStateOf<Artwork?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    var editId by remember { mutableStateOf("") }
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VivoraBackgroundGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                
                IconButton(
                    onClick = { viewModel.toggleSortOrder() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isSortedByLikes) MetallicGreen.copy(alpha = 0.1f) else Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = if (isSortedByLikes) Icons.Default.Whatshot else Icons.Default.NewReleases,
                        contentDescription = "Toggle Sort",
                        tint = if (isSortedByLikes) MetallicGreen else TextSecondary
                    )
                }
            }

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = TextSecondary,
                            selectedContainerColor = MetallicGreen.copy(alpha = 0.2f),
                            selectedLabelColor = MetallicGreen
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = Color.White.copy(alpha = 0.2f),
                            selectedBorderColor = MetallicGreen
                        )
                    )
                }
            }

            if (artworks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No artwork found.", color = TextPrimary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(artworks) { artwork ->
                        val isLiked = likedIds.contains(artwork.id)
                        val isSaved = savedIds.contains(artwork.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = artwork.imageUrl,
                                    contentDescription = artwork.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            onClick = { onArtworkClick(artwork.id) },
                                            onLongClick = {
                                                if (artwork.artistId == currentUserId) {
                                                    selectedArtworkForOptions = artwork
                                                }
                                            }
                                        )
                                )
                                
                                // Overlay Actions
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.4f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleLike(artwork.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (isLiked) Color.Red else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (artwork.likeCount > 0) {
                                        Text(
                                            "${artwork.likeCount}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleSave(artwork.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Save",
                                            tint = if (isSaved) MetallicGreen else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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
                                viewModel.deleteArtwork(it.id, onDone = {
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

        // Edit Dialog
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
                            viewModel.updateArtwork(editId, editTitle, editDescription, onDone = {
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
    }
}
