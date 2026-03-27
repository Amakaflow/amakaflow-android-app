package com.amakaflow.companion.ui.screens.social

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FeedViewModel"

data class FeedUiState(
    val posts: List<SocialFeedPost> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val error: String? = null,
    // Comment sheet state
    val selectedPostId: String? = null,
    val comments: List<SocialComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isPostingComment: Boolean = false
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val api: AmakaflowApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getSocialFeed(cursor = null, limit = 20)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            posts = body.posts,
                            isLoading = false,
                            hasMore = body.hasMore,
                            nextCursor = body.nextCursor
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load feed (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadFeed error", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Could not load feed: ${e.message}")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val response = api.getSocialFeed(cursor = null, limit = 20)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            posts = body.posts,
                            isRefreshing = false,
                            hasMore = body.hasMore,
                            nextCursor = body.nextCursor
                        )
                    }
                } else {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "refresh error", e)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val response = api.getSocialFeed(cursor = cursor, limit = 20)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            posts = it.posts + body.posts,
                            isLoadingMore = false,
                            hasMore = body.hasMore,
                            nextCursor = body.nextCursor
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMore error", e)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun toggleReaction(postId: String, emoji: String) {
        val index = _uiState.value.posts.indexOfFirst { it.id == postId }
        if (index == -1) return

        val post = _uiState.value.posts[index]
        val hasReacted = post.userReactions.contains(emoji)

        // Optimistic update
        val updatedReactions = if (hasReacted) {
            post.reactions.map { r ->
                if (r.emoji == emoji) r.copy(count = maxOf(0, r.count - 1)) else r
            }.filter { it.count > 0 }
        } else {
            val existing = post.reactions.find { it.emoji == emoji }
            if (existing != null) {
                post.reactions.map { r ->
                    if (r.emoji == emoji) r.copy(count = r.count + 1) else r
                }
            } else {
                post.reactions + SocialFeedReaction(emoji = emoji, count = 1)
            }
        }

        val updatedUserReactions = if (hasReacted) {
            post.userReactions.filter { it != emoji }
        } else {
            post.userReactions + emoji
        }

        val updatedPost = post.copy(
            reactions = updatedReactions,
            userReactions = updatedUserReactions
        )

        _uiState.update {
            val newPosts = it.posts.toMutableList()
            newPosts[index] = updatedPost
            it.copy(posts = newPosts)
        }

        viewModelScope.launch {
            try {
                if (hasReacted) {
                    api.removeSocialReaction(postId, emoji)
                } else {
                    api.addSocialReaction(postId, ReactRequest(emoji))
                }
            } catch (e: Exception) {
                Log.e(TAG, "toggleReaction error", e)
                // Revert on failure
                loadFeed()
            }
        }
    }

    fun loadComments(postId: String) {
        _uiState.update { it.copy(selectedPostId = postId, isLoadingComments = true, comments = emptyList()) }
        viewModelScope.launch {
            try {
                val response = api.getSocialComments(postId)
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(comments = response.body()?.comments ?: emptyList(), isLoadingComments = false)
                    }
                } else {
                    _uiState.update { it.copy(isLoadingComments = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadComments error", e)
                _uiState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    fun postComment(text: String) {
        val postId = _uiState.value.selectedPostId ?: return
        if (text.isBlank()) return

        _uiState.update { it.copy(isPostingComment = true) }
        viewModelScope.launch {
            try {
                api.postSocialComment(postId, CommentRequest(text.trim()))
                // Refresh comments and increment count
                loadComments(postId)

                val idx = _uiState.value.posts.indexOfFirst { it.id == postId }
                if (idx >= 0) {
                    _uiState.update {
                        val newPosts = it.posts.toMutableList()
                        newPosts[idx] = newPosts[idx].copy(commentCount = newPosts[idx].commentCount + 1)
                        it.copy(posts = newPosts, isPostingComment = false)
                    }
                } else {
                    _uiState.update { it.copy(isPostingComment = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "postComment error", e)
                _uiState.update { it.copy(isPostingComment = false) }
            }
        }
    }

    fun dismissComments() {
        _uiState.update { it.copy(selectedPostId = null, comments = emptyList()) }
    }
}
