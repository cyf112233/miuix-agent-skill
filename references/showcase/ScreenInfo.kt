package showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 信息展示 / 关于页：Hero 区 → 只读信息卡 → 动作卡 → 页脚。
 * 只读信息用 `ArrowPreference` 的「标题 + summary」两段式，不要自己拼 Row。
 */
@Composable
fun InfoScreen(dark: Boolean = false) {
    PhoneScreen(dark = dark) {
        val scrollBehavior = MiuixScrollBehavior()
        val colors = MiuixTheme.colorScheme
        Scaffold(
            topBar = {
                ScreenTopBar(
                    title = "关于",
                    scrollBehavior = scrollBehavior,
                    large = false,
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(MiuixIcons.Back, contentDescription = "返回", tint = colors.onSurface)
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
                item { Hero() }
                item { SmallTitle("应用信息") }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        ArrowPreference(title = "版本号", summary = "1.2.0 (2316)", onClick = {})
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ArrowPreference(title = "开发者", summary = "Miuix Team", onClick = {})
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ArrowPreference(title = "开源许可", summary = "Apache License 2.0", onClick = {})
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ArrowPreference(title = "隐私政策", onClick = {})
                    }
                }
                item { SmallTitle("支持") }
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        ArrowPreference(
                            title = "检查更新",
                            summary = "当前已是最新版本",
                            startAction = { AppIconTile(MiuixIcons.Update, colors.primary, colors.tertiaryContainer) },
                            onClick = {},
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ArrowPreference(
                            title = "问题反馈",
                            summary = "帮助我们把界面做得更好",
                            startAction = {
                                AppIconTile(MiuixIcons.Community, colors.primary, colors.tertiaryContainer)
                            },
                            onClick = {},
                        )
                    }
                }
                item {
                    Text(
                        text = "© 2026 Miuix Demo\n用 Miuix 构建 · Compose Multiplatform",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colors.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Hero() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIconTile(
            icon = MiuixIcons.Store,
            tint = MiuixTheme.colorScheme.primary,
            container = MiuixTheme.colorScheme.tertiaryContainer,
            size = 84.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Miuix 示例",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "HyperOS 设计规范的界面样本",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
}
