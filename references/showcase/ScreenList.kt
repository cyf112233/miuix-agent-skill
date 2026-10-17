package showcase

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 通用列表：搜索 → 分段筛选 → 分组卡片。
 * 行右侧用 `SwitchPreference`（可切换）或 `ArrowPreference`（可进入），
 * 不要手搓 `Row { Text(); Switch() }`。
 */
@Composable
fun ListScreen(dark: Boolean = false) {
    PhoneScreen(dark = dark) {
        val scrollBehavior = MiuixScrollBehavior()
        var query by remember { mutableStateOf("") }
        var tab by remember { mutableIntStateOf(0) }
        var autoStart by remember { mutableStateOf(true) }
        val colors = MiuixTheme.colorScheme

        Scaffold(
            topBar = { ScreenTopBar(title = "应用", scrollBehavior = scrollBehavior) },
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
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        label = "搜索应用或包名",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 4.dp),
                        leadingIcon = {
                            top.yukonga.miuix.kmp.basic.Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = null,
                                tint = colors.onSurfaceVariantSummary,
                            )
                        },
                    )
                }
                item {
                    TabRow(
                        tabs = listOf("全部", "用户", "系统"),
                        selectedTabIndex = tab,
                        onTabSelected = { tab = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 10.dp),
                    )
                }
                item { SmallTitle("常用应用") }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        ArrowPreference(
                            title = "相册",
                            summary = "2,318 张照片 · 12.4 GB",
                            startAction = { AppIconTile(MiuixIcons.Photos, colors.primary, colors.tertiaryContainer) },
                            onClick = {},
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ArrowPreference(
                            title = "文件管理",
                            summary = "最近打开 3 个文档",
                            startAction = { AppIconTile(MiuixIcons.Folder, colors.primary, colors.tertiaryContainer) },
                            onClick = {},
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ArrowPreference(
                            title = "音乐",
                            summary = "正在播放 · 晴天",
                            startAction = { AppIconTile(MiuixIcons.Music, colors.primary, colors.tertiaryContainer) },
                            onClick = {},
                        )
                    }
                }
                item { SmallTitle("自启动管理") }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        // 开关行必须直接用 SwitchPreference，不能把 SwitchPreference 塞进另一个
                        // Preference 的 endActions —— 嵌套 BasicComponent 会在 maxIntrinsicHeight 崩溃。
                        SwitchPreference(
                            title = "微信",
                            summary = "8.0.42 · 512 MB",
                            checked = autoStart,
                            onCheckedChange = { autoStart = it },
                            startAction = { AppIconTile(MiuixIcons.Store, colors.primary, colors.tertiaryContainer) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchPreference(
                            title = "哔哩哔哩",
                            summary = "7.65.0 · 268 MB",
                            checked = true,
                            onCheckedChange = {},
                            startAction = { AppIconTile(MiuixIcons.Photos, colors.primary, colors.tertiaryContainer) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchPreference(
                            title = "高德地图",
                            summary = "13.20.0 · 186 MB",
                            checked = false,
                            onCheckedChange = {},
                            startAction = { AppIconTile(MiuixIcons.Store, colors.primary, colors.tertiaryContainer) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchPreference(
                            title = "网易云音乐",
                            summary = "9.1.20 · 142 MB",
                            checked = true,
                            onCheckedChange = {},
                            startAction = { AppIconTile(MiuixIcons.Music, colors.primary, colors.tertiaryContainer) },
                        )
                    }
                }
            }
        }
    }
}

/** 一行应用：图标 + 名称/版本 + 右侧箭头。纯跳转行用 ArrowPreference。 */
@Composable
private fun AppRow(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
) {
    ArrowPreference(
        title = title,
        summary = summary,
        startAction = { AppIconTile(icon, tint, container) },
        onClick = {},
    )
}
