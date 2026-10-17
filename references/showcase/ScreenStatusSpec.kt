package showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 状态卡三态规格页。
 *
 * 这是把 `StatusPalette` 摆出来的「设计样张」：同一张卡片换 tone，其余结构完全不动。
 * 三态配色在 Miuix 里没有现成 token，出处与取值见 `references/status-colors.md`。
 */
@Composable
fun StatusSpecScreen(dark: Boolean = false) {
    PhoneScreen(dark = dark) {
        val scrollBehavior = MiuixScrollBehavior()
        Scaffold(
            topBar = {
                ScreenTopBar(
                    title = "状态卡",
                    scrollBehavior = scrollBehavior,
                    large = false,
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(MiuixIcons.Back, contentDescription = "返回", tint = MiuixTheme.colorScheme.onSurface)
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
                item {
                    Text(
                        text = "同一张卡片、同一套排版，只换语义色。绿色用于「一切正常」，琥珀色用于「需要注意」，红色只留给「功能已经不可用」。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 28.dp).padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                item { SmallTitle("三种状态") }
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusCard(
                            tone = StatusPalette.success(),
                            title = "运行正常",
                            description = "全部 3 项检查通过 · 2 分钟前",
                            icon = StatusIcons.Check,
                        )
                        StatusCard(
                            tone = StatusPalette.warning(),
                            title = "存储空间不足",
                            description = "仅剩 3.1 GB，建议清理缓存",
                            icon = StatusIcons.Warning,
                        )
                        StatusCard(
                            tone = StatusPalette.danger(),
                            title = "后台服务已停止",
                            description = "点击重新启动，或查看运行日志",
                            icon = StatusIcons.Warning,
                        )
                    }
                }
                item { SmallTitle("色板") }
                item { PaletteCard() }
            }
        }
    }
}

@Composable
private fun PaletteCard() {
    val tones = listOf(
        Triple("正常", StatusPalette.success(), "#36D167"),
        Triple("注意", StatusPalette.warning(), "#F59E0B"),
        Triple("异常", StatusPalette.danger(), "#D13636"),
    )
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            tones.forEach { (name, tone, hex) ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(tone.accent),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = name,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = hex,
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Text(
            text = "容器色与强调色是成对定义的实色（不是算出来的）：浅色 #DFFAE4 / #FFF4E0 / #FDECEE，深色 #102819 / #2E2210 / #402626。",
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(16.dp),
        )
    }
}
