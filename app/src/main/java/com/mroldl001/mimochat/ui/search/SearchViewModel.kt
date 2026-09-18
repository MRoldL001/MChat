
package com.mroldl001.mimochat.ui.search

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mroldl001.mimochat.data.repository.ChatRepository
import com.mroldl001.mimochat.domain.model.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    private val _hasSearched = mutableStateOf(false)
    val hasSearched: State<Boolean> = _hasSearched

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        if (_searchQuery.value == query) return
        _searchQuery.value = query
        // Cancel both the pending debounce and the previous database subscription immediately.
        searchJob?.cancel()
        searchJob = null
        _searchResults.value = emptyList()
        _hasSearched.value = false
        _isSearching.value = false
        if (query.isBlank()) return

        searchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true

            chatRepository.searchMessages(query).collect { results ->
                currentCoroutineContext().ensureActive()
                _searchResults.value = results
                _hasSearched.value = true
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        updateQuery("")
    }
}
