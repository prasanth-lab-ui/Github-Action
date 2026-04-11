package com.saithanyam.wallpaper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.saithanyam.wallpaper.data.BirthdayRepository
import com.saithanyam.wallpaper.util.WeekCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Available live wallpaper styles.
 */
enum class WallpaperStyle { GRID, NUMBER }

data class SetupUiState(
    val birthday: LocalDate? = null,
    val weeksLived: Int = 0,
    val weeksRemaining: Int = WeekCalculator.TOTAL_WEEKS,
    val isConfirmed: Boolean = false,
    val selectedStyle: WallpaperStyle = WallpaperStyle.GRID,
    val shareSuccess: Boolean? = null
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BirthdayRepository(application)

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        // Restore previously saved birthday
        repository.getBirthday()?.let { saved ->
            confirmBirthday(saved)
        }
    }

    fun confirmBirthday(date: LocalDate) {
        repository.saveBirthday(date)
        val lived = WeekCalculator.weeksLived(date)
        val remaining = WeekCalculator.weeksRemaining(date)
        _uiState.value = _uiState.value.copy(
            birthday = date,
            weeksLived = lived,
            weeksRemaining = remaining,
            isConfirmed = true
        )
    }

    fun selectStyle(style: WallpaperStyle) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun onShareCard() {
        val state = _uiState.value
        if (!state.isConfirmed) return
        val success = com.saithanyam.wallpaper.util.ShareCardGenerator.generateAndSave(
            getApplication(),
            state.weeksRemaining
        )
        _uiState.value = state.copy(shareSuccess = success)
    }

    fun clearShareResult() {
        _uiState.value = _uiState.value.copy(shareSuccess = null)
    }
}
