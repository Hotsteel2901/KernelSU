package me.weishu.kernelsu.ui.screen.kpm

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.util.getFileName
import me.weishu.kernelsu.ui.viewmodel.KpmViewModel
import java.io.File

@Composable
fun KpmScreen() {
    val navigator = LocalNavigator.current
    val uiMode = LocalUiMode.current
    val viewModel = viewModel<KpmViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val selectKpmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    copyKpmToCache(uri)?.let { file ->
                        viewModel.load(file.absolutePath)
                        file.delete()
                    }
                }
            }
        }
    }

    val actions = KpmScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onRefresh = viewModel::refresh,
        onLoad = {
            selectKpmLauncher.launch(
                Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/octet-stream" }
            )
        },
        onUnload = viewModel::unload,
        onPatchBoot = { navigator.push(Route.KpmPatch) },
    )

    when (uiMode) {
        UiMode.Miuix -> KpmScreenMiuix(
            state = uiState,
            onBack = actions.onBack,
            onRefresh = actions.onRefresh,
            onLoad = actions.onLoad,
            onUnload = actions.onUnload,
            onPatchBoot = actions.onPatchBoot,
        )
        UiMode.Material -> KpmScreenMaterial(
            state = uiState,
            onBack = actions.onBack,
            onRefresh = actions.onRefresh,
            onLoad = actions.onLoad,
            onUnload = actions.onUnload,
            onPatchBoot = actions.onPatchBoot,
        )
    }
}

data class KpmScreenActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onLoad: () -> Unit,
    val onUnload: (String) -> Unit,
    val onPatchBoot: () -> Unit,
)

private fun copyKpmToCache(uri: Uri): File? {
    return try {
        val resolver = ksuApp.contentResolver
        val name = uri.getFileName(ksuApp) ?: "module.kpm"
        val file = File(ksuApp.cacheDir, name)
        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        null
    }
}
