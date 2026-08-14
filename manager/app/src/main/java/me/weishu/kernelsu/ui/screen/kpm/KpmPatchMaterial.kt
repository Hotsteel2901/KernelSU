package me.weishu.kernelsu.ui.screen.kpm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedListItem
import me.weishu.kernelsu.ui.component.material.TopBarBackButton
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors

@Composable
fun KpmPatchScreenMaterial(
    state: KpmPatchUiState,
    actions: KpmPatchActions,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kpm_patch)) },
                navigationIcon = {
                    TopBarBackButton(onClick = actions.onBack)
                },
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.kpm_patch_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            SegmentedColumn(
                modifier = Modifier.fillMaxWidth(),
                content = listOf(
                    {
                        SegmentedListItem(
                            onClick = actions.onSelectBoot,
                            headlineContent = { Text(stringResource(R.string.kpm_patch_select_boot)) },
                            supportingContent = {
                                Text(state.bootName.ifEmpty { "-" }, textAlign = TextAlign.End)
                            },
                            leadingContent = { Icon(Icons.Filled.Image, null) },
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = actions.onSelectKpimg,
                            headlineContent = { Text(stringResource(R.string.kpm_patch_select_kpimg)) },
                            supportingContent = {
                                Text(state.kpimgName.ifEmpty { "-" }, textAlign = TextAlign.End)
                            },
                            leadingContent = { Icon(Icons.Filled.Build, null) },
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = actions.onSelectKptools,
                            headlineContent = { Text(stringResource(R.string.kpm_patch_select_kptools)) },
                            supportingContent = {
                                Text(state.kptoolsName.ifEmpty { "-" }, textAlign = TextAlign.End)
                            },
                            leadingContent = { Icon(Icons.Filled.Settings, null) },
                        )
                    },
                ),
            )

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
                    color = if (isSuccess) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
