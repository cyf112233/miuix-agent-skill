package showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 设置页：`SmallTitle` + `Card(horizontal = 12.dp)` + `XxxPreference`，组内行间分割线内缩 16dp。
 * 传 `showDialog = true` 时同时渲染一个 `OverlayDialog`，用来展示弹层叠在页面上的样子。
 */
@Composable
fun SettingsScreen(dark: Boolean = false, showDialog: Boolean = false) {
    PhoneScreen(dark = dark) {
        val scrollBehavior = MiuixScrollBehavior()
        Scaffold(
            topBar = {
                ScreenTopBar(
                    title = "设置",
                    scrollBehavior = scrollBehavior,
                    large = false,
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = "返回",
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 12.dp,
                ),
                overscrollEffect = null,
            ) {
                item { SmallTitle("通用") }
                item { GeneralCard() }
                item { SmallTitle("显示") }
                item { DisplayCard() }
                item { SmallTitle("隐私与安全") }
                item { PrivacyCard() }
                item { SmallTitle("关于") }
                item { AboutCard() }
            }

            // 弹层放在 Scaffold 的内容 lambda 里、LazyColumn 之外（不能放进 item {}）。
            OverlayDialog(
                show = showDialog,
                title = "恢复默认设置？",
                summary = "所有偏好将恢复为出厂值，此操作不可撤销。",
                onDismissRequest = {},
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(text = "取消", onClick = {}, modifier = Modifier.weight(1f))
                        TextButton(text = "恢复", onClick = {}, modifier = Modifier.weight(1f))
                    }
                },
            )
        }
    }
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        content()
    }
}

@Composable
private fun GeneralCard() {
    var autoCheck by remember { mutableStateOf(true) }
    var languageIndex by remember { mutableIntStateOf(0) }
    GroupCard {
        SwitchPreference(
            title = "自动检查更新",
            summary = "仅在 Wi-Fi 下检查",
            checked = autoCheck,
            onCheckedChange = { autoCheck = it },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        OverlayDropdownPreference(
            title = "语言",
            summary = "界面显示语言",
            items = listOf("简体中文", "繁體中文", "English"),
            selectedIndex = languageIndex,
            onSelectedIndexChange = { languageIndex = it },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = "默认存储位置",
            summary = "内部存储",
            onClick = {},
        )
    }
}

@Composable
private fun DisplayCard() {
    var fontScale by remember { mutableFloatStateOf(1f) }
    var showLabels by remember { mutableStateOf(true) }
    GroupCard {
        SliderPreference(
            title = "字体大小",
            summary = "影响所有界面的文字",
            value = fontScale,
            onValueChange = { fontScale = it },
            valueRange = 0.85f..1.3f,
            valueText = "${(fontScale * 100).toInt()}%",
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SwitchPreference(
            title = "底部导航显示文字",
            summary = "关闭后只显示图标",
            checked = showLabels,
            onCheckedChange = { showLabels = it },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SwitchPreference(
            title = "深色模式跟随系统",
            checked = true,
            onCheckedChange = {},
        )
    }
}

@Composable
private fun PrivacyCard() {
    var anonymous by remember { mutableStateOf(false) }
    GroupCard {
        SwitchPreference(
            title = "匿名使用统计",
            summary = "仅上传崩溃与性能数据，不含任何个人信息",
            checked = anonymous,
            onCheckedChange = { anonymous = it },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ArrowPreference(title = "权限管理", summary = "已授予 4 项权限", onClick = {})
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ArrowPreference(title = "清除本地数据", summary = "缓存与搜索历史", onClick = {})
    }
}

@Composable
private fun AboutCard() {
    GroupCard {
        ArrowPreference(title = "版本", summary = "1.2.0 (2316)", onClick = {})
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ArrowPreference(title = "开源许可", onClick = {})
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ArrowPreference(title = "问题反馈", summary = "帮助我们把界面做得更好", onClick = {})
    }
    Spacer(Modifier.height(8.dp))
}
