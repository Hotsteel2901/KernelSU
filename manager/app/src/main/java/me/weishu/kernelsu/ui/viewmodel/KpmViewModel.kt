package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.util.kpmList
import me.weishu.kernelsu.ui.util.kpmNum
import me.weishu.kernelsu.ui.util.loadKpm
import me.weishu.kernelsu.ui.util.unloadKpm

data class KpmUiState(
    val isLoading: Boolean = false,
    val available: Boolean = true,
    val modules: List<KpmModule> = emptyList(),
    val count: Int = 0,
    val errorMessage: String? = null,
)

data class KpmModule(
    val name: String,
    val info: String = "",
)

class KpmViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(KpmUiState())
    val uiState: StateFlow<KpmUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val count = kpmNum()
            val raw = kpmList()
            val modules = raw.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { KpmModule(name = it) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    available = true,
                    count = count,
                    modules = modules,
                )
            }
        }
    }

    fun load(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = loadKpm(path)
            _uiState.update {
                it.copy(
                    errorMessage = if (ok) null else "kpm load failed",
                )
            }
            if (ok) refresh()
        }
    }

    fun unload(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = unloadKpm(name)
            _uiState.update {
                it.copy(
                    errorMessage = if (ok) null else "kpm unload failed",
                )
            }
            if (ok) refresh()
        }
    }
}
