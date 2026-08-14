package me.weishu.kernelsu.ui.screen.kpm

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.getFileName
import me.weishu.kernelsu.ui.util.patchKpmBoot

@Composable
fun KpmPatchScreen() {
    val navigator = LocalNavigator.current
    val uiMode = LocalUiMode.current
    val context = LocalContext.current

    var bootUri by remember { mutableStateOf<Uri?>(null) }
    var kpimgUri by remember { mutableStateOf<Uri?>(null) }
    var kptoolsUri by remember { mutableStateOf<Uri?>(null) }
    var isPatching by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingAssign by remember { mutableStateOf<(Uri) -> Unit>({ }) }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri -> pendingAssign(uri) }
        }
    }

    val launchPicker: ((Uri) -> Unit) -> Unit = { assign ->
        pendingAssign = assign
        fileLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/octet-stream" })
    }

    val actions = KpmPatchActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSelectBoot = { launchPicker { bootUri = it } },
        onSelectKpimg = { launchPicker { kpimgUri = it } },
        onSelectKptools = { launchPicker { kptoolsUri = it } },
        onPatch = {
            val boot = bootUri
            val kpimg = kpimgUri
            val kptools = kptoolsUri
            if (boot != null && kpimg != null && kptools != null && !isPatching) {
                isPatching = true
                message = null
                patchKpmBoot(
                    bootUri = boot,
                    kpimgUri = kpimg,
                    kptoolsUri = kptools,
                    onStdout = { },
                    onStderr = { },
                ).let { result ->
                    isPatching = false
                    message = if (result.code == 0) "kpm_patch_success" else "kpm_patch_failed"
                }
            }
        },
    )

    val state = KpmPatchUiState(
        bootName = bootUri?.let { it.getFileName(context) }.orEmpty(),
        kpimgName = kpimgUri?.let { it.getFileName(context) }.orEmpty(),
        kptoolsName = kptoolsUri?.let { it.getFileName(context) }.orEmpty(),
        isPatching = isPatching,
        message = message,
    )

    when (uiMode) {
        UiMode.Miuix -> KpmPatchScreenMiuix(state, actions)
        UiMode.Material -> KpmPatchScreenMaterial(state, actions)
    }
}

data class KpmPatchActions(
    val onBack: () -> Unit,
    val onSelectBoot: () -> Unit,
    val onSelectKpimg: () -> Unit,
    val onSelectKptools: () -> Unit,
    val onPatch: () -> Unit,
)

data class KpmPatchUiState(
    val bootName: String,
    val kpimgName: String,
    val kptoolsName: String,
    val isPatching: Boolean,
    val message: String?,
)
