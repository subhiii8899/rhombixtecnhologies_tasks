package com.example.vivora

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.vivora.ui.theme.DeepNavy
import com.example.vivora.ui.theme.MetallicGreen
import com.example.vivora.ui.theme.TextPrimary
import com.example.vivora.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArtworkDetailScreen(
    artworkId: String,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    artworkViewModel: ArtworkViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val artworks by artworkViewModel.exploreFeed.collectAsState()
    val artwork = artworks.find { it.id == artworkId }
    val likedIds by artworkViewModel.likedArtworkIds.collectAsState()
    val savedIds by artworkViewModel.savedArtworkIds.collectAsState()
    
    val artistProfile by profileViewModel.profile.collectAsState()
    val isFollowing by profileViewModel.isFollowing.collectAsState()
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val scrollState = rememberScrollState()

    val isLiked = likedIds.contains(artworkId)
    val isSaved = savedIds.contains(artworkId)

    LaunchedEffect(artwork?.artistId) {
        artwork?.artistId?.let { profileViewModel.loadProfile(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(artwork?.title ?: "Artwork", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { artworkViewModel.toggleSave(artworkId) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) MetallicGreen else TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = DeepNavy
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VivoraBackgroundGradient)
                .padding(padding)
        ) {
            if (artwork == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MetallicGreen)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    AsyncImage(
                        model = artwork.imageUrl,
                        contentDescription = artwork.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp, max = 500.dp)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )

                    // Artist Info & Follow Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { onArtistClick(artwork.artistId) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = artistProfile?.profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = artistProfile?.username ?: "Artist",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${artistProfile?.followersCount ?: 0} Followers",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (artwork.artistId != currentUserId) {
                            Button(
                                onClick = { profileViewModel.toggleFollow(artwork.artistId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color.Transparent else MetallicGreen,
                                    contentColor = if (isFollowing) MetallicGreen else Color.White
                                ),
                                border = if (isFollowing) BorderStroke(1.dp, MetallicGreen) else null,
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = if (isFollowing) "Following" else "Follow",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { artworkViewModel.toggleLike(artworkId) }) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color.Red else TextPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "${artwork.likeCount} Likes",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Text(
                            text = artwork.category,
                            color = MetallicGreen,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = artwork.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = artwork.description,
                            fontSize = 16.sp,
                            color = TextSecondary,
                            lineHeight = 24.sp
                        )

                        if (artwork.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                artwork.tags.forEach { tag ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("#$tag", color = MetallicGreen) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MetallicGreen.copy(alpha = 0.1f)
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = MetallicGreen.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
