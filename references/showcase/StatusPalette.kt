package showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * Miuix 的默认调色板只有两类强语义色：`primary`（蓝，主操作/选中）和 `error`（红，错误）。
 * **没有 success / warning token** —— 这是库的真实边界，不是文档缺失。
 *
 * 真实项目的做法一致：自己补一组三态色（出处见 `references/status-colors.md`）：
 * 绿 = 正常，琥珀 = 注意，红 = 异常。容器是「同色低饱和底」，文字仍用 `onSurface`，
 * 浅色/深色都不用改文字色，也不会出现浅底浅字。
 */
@Immutable
data class StatusTone(
    val container: Color,
    val accent: Color,
)

/** 主题可能被强制为 Light/Dark/Monet，不能用 isSystemInDarkTheme() 猜，读主题自己的背景亮度。 */
@Composable
fun isMiuixDark(): Boolean = MiuixTheme.colorScheme.background.luminance() < 0.5f

object StatusPalette {
    // miuix-lint:allow 三态色是 Miuix 缺失的 token，成对定义，出处见 references/status-colors.md
    private val SuccessLight = StatusTone(container = Color(0xFFDFFAE4), accent = Color(0xFF36D167))
    // miuix-lint:allow
    private val SuccessDark = StatusTone(container = Color(0xFF102819), accent = Color(0xFF36D167))
    // miuix-lint:allow 琥珀 = 注意，accent 取自 OShin
    private val WarningLight = StatusTone(container = Color(0xFFFFF4E0), accent = Color(0xFFF59E0B))
    // miuix-lint:allow
    private val WarningDark = StatusTone(container = Color(0xFF2E2210), accent = Color(0xFFF59E0B))
    // miuix-lint:allow
    private val DangerLight = StatusTone(container = Color(0xFFFDECEE), accent = Color(0xFFD13636))
    // miuix-lint:allow
    private val DangerDark = StatusTone(container = Color(0xFF402626), accent = Color(0xFFD84242))

    @Composable
    fun success(): StatusTone = if (isMiuixDark()) SuccessDark else SuccessLight

    @Composable
    fun warning(): StatusTone = if (isMiuixDark()) WarningDark else WarningLight

    @Composable
    fun danger(): StatusTone = if (isMiuixDark()) DangerDark else DangerLight
}

/**
 * 状态卡：整卡换底色 + 右下角出血的 128dp 水印图标 + 标题/说明。
 *
 * 结构逐条对齐 lyricon `HomeTab.kt:126-190` 与 InstallerX `MiuixHomePage.kt:286-360`：
 * 卡片高 140dp、内容 padding 16dp、图标 128dp 且 `offset(28, 34)`、标题 20sp SemiBold、
 * 说明 14sp Medium、`PressFeedbackType.Tilt`。两个项目在这里是完全一致的。
 */
@Composable
fun StatusCard(
    tone: StatusTone,
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().height(140.dp),
        colors = CardDefaults.defaultColors(
            color = tone.container,
            contentColor = MiuixTheme.colorScheme.onSurface,
        ),
        onClick = onClick,
        showIndication = onClick != null,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone.accent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 28.dp, y = 34.dp)
                    .size(128.dp),
            )
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                sp2()
                Text(
                    text = description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun sp2() = androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))

/** 两列小数据卡，对齐 InstallerX `MiuixStatCard`：标题 15sp → 数值 26sp。 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.height(86.dp),
        insideMargin = PaddingValues(16.dp),
        onClick = onClick,
        showIndication = onClick != null,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                if (hint != null) {
                    Text(
                        text = hint,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
            }
        }
    }
}

/** 状态点：卡片内联、列表行尾都能用；颜色只由语义决定。 */
@Composable
fun StatusDot(color: Color, size: androidx.compose.ui.unit.Dp = 8.dp) {
    Box(modifier = Modifier.size(size).clip(CircleShape).background(color))
}
