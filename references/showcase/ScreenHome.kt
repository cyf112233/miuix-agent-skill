package showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Carrier
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 首页 / 仪表盘。
 *
 * 结构（自上而下）：状态卡（绿/黄/红三态之一）→ 两列数据卡 → 快捷入口行 →
 * 常用设置分组 → 底部导航。对应 `references/scenario-home.md`、
 * 状态色出处见 `references/status-colors.md`。
 */
@Composable
fun HomeScreen(dark: Boolean = false) {
    PhoneScreen(dark = dark) {
        val scrollBehavior = MiuixScrollBehavior()
        Scaffold(
            topBar = { ScreenTopBar(title = "首页", scrollBehavior = scrollBehavior) },
            bottomBar = { MainNavigationBar(selectedIndex = 0) },
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
                item {
                    StatusCard(
                        tone = StatusPalette.success(),
                        title = "运行正常",
                        description = "已开启全部优化项 · 2 分钟前检查",
                        icon = StatusIcons.Check,
                        modifier = Modifier.padding(horizontal = 12.dp).padding(top = 4.dp),
                        onClick = {},
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(label = "已用存储", value = "87.2", hint = "GB", modifier = Modifier.weight(1f))
                        StatCard(label = "可用内存", value = "7.8", hint = "GB", modifier = Modifier.weight(1f))
                    }
                }
                item { QuickActions() }
                item { SmallTitle("最近使用") }
                item { RecentAppsCard() }
                item { SmallTitle("常用设置") }
                item { HomeSettingsCard() }
            }
        }
    }
}

@Composable
private fun QuickActions() {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        QuickAction("清理加速", MiuixIcons.Refresh, colors.onSurface, colors.surfaceContainerHigh)
        QuickAction("流量管理", MiuixIcons.Carrier, colors.onSurface, colors.surfaceContainerHigh)
        QuickAction("扫一扫", MiuixIcons.Scan, colors.onSurface, colors.surfaceContainerHigh)
        QuickAction("应用商店", MiuixIcons.Store, colors.onSurface, colors.surfaceContainerHigh)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    container: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(icon = icon, tint = tint, container = container, size = 50.dp, iconSize = 24.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeSettingsCard() {
    var autoClean by remember { mutableStateOf(true) }
    var batterySaver by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        SwitchPreference(
            title = "自动清理",
            summary = "每天 03:00 清理缓存文件",
            checked = autoClean,
            onCheckedChange = { autoClean = it },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        SwitchPreference(
            title = "省电模式",
            summary = "电量低于 20% 时自动开启",
            checked = batterySaver,
            onCheckedChange = { batterySaver = it },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = "存储管理",
            summary = "查看各类文件的占用情况",
            onClick = {},
        )
    }
}

@Composable
fun MainNavigationBar(selectedIndex: Int) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = {},
            icon = MiuixIcons.Home,
            label = "首页",
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = {},
            icon = MiuixIcons.Store,
            label = "应用",
        )
        NavigationBarItem(
            selected = selectedIndex == 2,
            onClick = {},
            icon = MiuixIcons.Settings,
            label = "设置",
        )
    }
}

@Composable
private fun RecentAppsCard() {
    val colors = MiuixTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        ArrowPreference(
            title = "相机",
            summary = "2 分钟前使用",
            startAction = { AppIconTile(MiuixIcons.Image, colors.primary, colors.tertiaryContainer) },
            onClick = {},
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = "文件管理",
            summary = "1 小时前使用",
            startAction = { AppIconTile(MiuixIcons.Folder, colors.primary, colors.tertiaryContainer) },
            onClick = {},
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = "音乐",
            summary = "昨天使用",
            startAction = { AppIconTile(MiuixIcons.Music, colors.primary, colors.tertiaryContainer) },
            onClick = {},
        )
    }
}
