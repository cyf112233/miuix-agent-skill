package showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 引导页：插画 → 标题/说明 → 能力列表 → 页点 → 主/次按钮。
 * 引导页没有 TopAppBar（`StatusBar` 直接作为 topBar 槽），
 * 按钮区固定在底部，不要跟着内容滚动。
 */
@Composable
fun OnboardingScreen(dark: Boolean = false) {
    PhoneScreen(dark = dark) {
        val colors = MiuixTheme.colorScheme
        Scaffold(topBar = { StatusBar() }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(20.dp))
                Illustration()
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "欢迎使用 Miuix",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "用 HyperOS 的设计语言，构建你自己的 Compose Multiplatform 应用。",
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.onBackgroundVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                FeatureRow(
                    icon = MiuixIcons.Store,
                    title = "开箱即用的 HyperOS 组件",
                    description = "设置项、弹层、顶栏、导航全部按小米规范实现",
                    tint = colors.primary,
                    container = colors.tertiaryContainer,
                )
                Spacer(Modifier.height(18.dp))
                FeatureRow(
                    icon = MiuixIcons.Tune,
                    title = "跟随系统的动态取色",
                    description = "一套代码适配浅色、深色与壁纸取色",
                    tint = colors.primary,
                    container = colors.tertiaryContainer,
                )
                Spacer(Modifier.height(18.dp))
                FeatureRow(
                    icon = MiuixIcons.Lock,
                    title = "只申请必要的权限",
                    description = "随时可以在系统设置中撤回",
                    tint = colors.primary,
                    container = colors.tertiaryContainer,
                )
                Spacer(Modifier.weight(1f))
                PageDots()
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        color = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                ) {
                    Text(
                        text = "开始使用",
                        style = MiuixTheme.textStyles.button,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(text = "以后再说", onClick = {}, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun Illustration() {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier.size(168.dp).clip(CircleShape).background(colors.tertiaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.Store,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(72.dp),
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIconTile(icon = icon, tint = tint, container = container, size = 38.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
    }
}

@Composable
private fun PageDots() {
    val colors = MiuixTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(width = 20.dp, height = 6.dp).clip(CircleShape).background(colors.primary))
        repeat(2) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.outline))
        }
    }
}
