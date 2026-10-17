package showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 状态页（空态）：整页居中插画 + 标题 + 说明 + 主操作。
 * 空态不要用 `LazyColumn`，直接 `Box(contentAlignment = Center)`；
 * 插画用「底色圆 + 单个图标」，比引入图片资源更适合纯代码项目。
 */
@Composable
fun StateScreen(dark: Boolean = false) {
    PhoneScreen(dark = dark) {
        val scrollBehavior = MiuixScrollBehavior()
        val colors = MiuixTheme.colorScheme
        Scaffold(
            topBar = { ScreenTopBar(title = "收藏", scrollBehavior = scrollBehavior, large = false) },
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(colors.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Favorites,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "还没有收藏",
                        style = MiuixTheme.textStyles.title3,
                        color = colors.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "在任意页面长按内容即可收藏，\n收藏会同步到你的所有设备。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = colors.onBackgroundVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {},
                        minWidth = 160.dp,
                        colors = ButtonDefaults.buttonColors(
                            color = colors.primary,
                            contentColor = colors.onPrimary,
                        ),
                    ) {
                        Text(
                            text = "去浏览",
                            style = MiuixTheme.textStyles.button,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
