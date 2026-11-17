package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SearchFilter
import com.example.model.ShortItem
import com.example.model.VideoItem
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CompactVideoRowCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VideoThumbnailSurface
import com.example.ui.viewmodel.SearchUiState
import com.example.util.FormatUtils

@Composable
fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onSelectFilter: (SearchFilter) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onShortClick: (ShortItem) -> Unit,
    onToggleSubscribe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Search Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }

                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.testTag("search_clear_input")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                onSubmitSearch(state.query)
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input")
                    )

                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onSubmitSearch(state.query)
                        },
                        modifier = Modifier.testTag("search_submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (state.query.isNotBlank()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SearchFilter.entries) { filter ->
                            FilterChip(
                                selected = state.filter == filter,
                                onClick = { onSelectFilter(filter) },
                                label = { Text(filter.label) },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null,
                                modifier = Modifier.testTag("search_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }
        }

        val showResults = state.query.isNotBlank() && state.hasSubmittedQuery
        val showLivePreview = state.query.isNotBlank() && !state.hasSubmittedQuery

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("search_list"),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            // Recent Searches (when query is blank)
            if (state.query.isBlank() && state.recentSearches.isNotEmpty()) {
                item(key = "recent_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.recent_searches),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = onClearRecentSearches,
                            modifier = Modifier.testTag("clear_recent_searches")
                        ) {
                            Text(stringResource(R.string.clear_all))
                        }
                    }
                }

                items(state.recentSearches, key = { "recent_$it" }) { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                keyboardController?.hide()
                                onSubmitSearch(recent)
                            }
                            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                            .testTag("recent_search_item"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = recent,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveRecentSearch(recent) }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove recent search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Search Suggestions (when typing or empty)
            if (!showResults && state.suggestions.isNotEmpty()) {
                item(key = "suggestions_header") {
                    Text(
                        text = stringResource(R.string.suggestions),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(state.suggestions, key = { "sug_$it" }) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                keyboardController?.hide()
                                onSubmitSearch(suggestion)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("search_suggestion_item"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.NorthWest,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Results (both when submitted and live preview below suggestions)
            if (showResults || showLivePreview) {
                val showChannels =
                    (state.filter == SearchFilter.ALL || state.filter == SearchFilter.CHANNELS) &&
                        state.matchingChannels.isNotEmpty()
                val showShorts =
                    (state.filter == SearchFilter.ALL || state.filter == SearchFilter.SHORTS) &&
                        state.matchingShorts.isNotEmpty()
                val showVideos =
                    (state.filter == SearchFilter.ALL || state.filter == SearchFilter.VIDEOS) &&
                        state.matchingVideos.isNotEmpty()

                if (showResults && !showChannels && !showShorts && !showVideos) {
                    item(key = "no_results") {
                        EmptyStateView(
                            icon = Icons.Outlined.Search,
                            title = "No results for \"${state.query}\"",
                            subtitle = "Try searching by video title, song name, artist, or quality (e.g., 480p SD).",
                            actionLabel = "Clear Search",
                            onAction = { onQueryChange("") }
                        )
                    }
                } else {
                    if (showChannels || showShorts || showVideos) {
                        item(key = "results_header") {
                            Text(
                                text = stringResource(R.string.search_results),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    if (showChannels) {
                        items(state.matchingChannels, key = { "ch_${it.id}" }) { ch ->
                            val isSub = ch.id in state.subscribedChannelIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ChannelAvatar(channel = ch, size = 48.dp, showVerifiedBadge = true)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${ch.handle} • ${FormatUtils.formatCompactCount(ch.baseSubscriberCount)} subscribers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { onToggleSubscribe(ch.id) },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSub) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        contentColor = if (isSub) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        }
                                    )
                                ) {
                                    Text(if (isSub) "Subscribed" else "Subscribe")
                                }
                            }
                        }
                    }

                    if (showVideos) {
                        items(state.matchingVideos, key = { "res_v_${it.id}" }) { video ->
                            CompactVideoRowCard(
                                video = video,
                                channel = state.channelsById[video.channelId],
                                onClick = {
                                    onSubmitSearch(state.query)
                                    onVideoClick(video)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (showShorts) {
                        item(key = "shorts_results_row") {
                            Text(
                                text = "Shorts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.matchingShorts, key = { "res_s_${it.id}" }) { short ->
                                    Column(
                                        modifier = Modifier
                                            .width(132.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onShortClick(short) }
                                    ) {
                                        VideoThumbnailSurface(
                                            artwork = short.artwork,
                                            contentDescription = short.title,
                                            aspectRatio = 9f / 14f,
                                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayCircleOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = short.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
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
    }
}
