package me.weishu.kernelsu.ui.screen.kpm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.LocalEnableBlur
import me.weishu.kernelsu.ui.util.BlurredBar
import me.weishu.kernelsu.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun KpmPatchScreenMiuix(
    state: KpmPatchUiState,
    actions: KpmPatchActions,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                SmallTopAppBar(
                    title = stringResource(R.string.kpm_patch),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = actions.onBack) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = null)
                        }
                    },
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                Text(
                    text = stringResource(R.string.kpm_patch_note),
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                Card(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.kpm_patch_select_boot),
                        summary = state.bootName.ifEmpty { "-" },
                        startAction = {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                            )
                        },
                        onClick = actions.onSelectBoot,
                    )
                    ArrowPreference(
                        title = stringResource(R.string.kpm_patch_select_kpimg),
                        summary = state.kpimgName.ifEmpty { "-" },
                        startAction = {
                            Icon(
                                imageVector = Icons.Rounded.Build,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                            )
                        },
                        onClick = actions.onSelectKpimg,
                    )
                    ArrowPreference(
                        title = stringResource(R.string.kpm_patch_select_kptools),
                        summary = state.kptoolsName.ifEmpty { "-" },
                        startAction = {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = colorScheme.onBackground,
                            )
                        },
                        onClick = actions.onSelectKptools,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = actions.onPatch,
                    enabled = state.bootName.isNotEmpty() &&
                        state.kpimgName.isNotEmpty() &&
                        state.kptoolsName.isNotEmpty() &&
                        !state.isPatching,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.isPatching) stringResource(R.string.kpm_patch_running)
                        else stringResource(R.string.kpm_patch_start)
                    )
                }

                state.message?.let { msg ->
                    val isSuccess = msg == "kpm_patch_success"
                    Text(
                        text = stringResource(
                            if (isSuccess) R.string.kpm_patch_success else R.string.kpm_patch_failed
                        ),
                        color = if (isSuccess) colorScheme.primary else colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
