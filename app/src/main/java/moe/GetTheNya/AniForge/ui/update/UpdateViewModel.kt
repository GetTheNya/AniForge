package moe.GetTheNya.AniForge.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateManager: UpdateManager
) : ViewModel() {

    val updateState: StateFlow<UpdateManager.UpdateState> = updateManager.updateState

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates(silent = false)
        }
    }

    fun downloadUpdate(downloadUrl: String) {
        updateManager.startDownload(downloadUrl)
    }

    fun installUpdate(apkFile: File) {
        updateManager.installUpdate(apkFile)
    }

    fun reset() {
        updateManager.reset()
    }
}
