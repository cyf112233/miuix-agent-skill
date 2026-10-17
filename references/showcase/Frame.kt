package showcase

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 手机屏幕的 dp 尺寸，渲染脚本按它换算像素。 */
const val PHONE_WIDTH_DP = 412
const val PHONE_HEIGHT_DP = 915

/** 状态栏高度（模拟 Android status bar，桌面端 insets 为 0，这里自己画）。 */
val StatusBarHeight = 44.dp

@Composable
fun ShowcaseTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val controller = remember(dark) {
        ThemeController(if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light)
    }
    MiuixTheme(controller = controller, content = content)
}

/**
 * 把页面装进「手机屏幕」：状态栏 + 页面本体。
 * 页面本体的第一个元素就是它自己的 Scaffold —— 和真机上完全一致。
 */
@Composable
fun PhoneScreen(dark: Boolean = false, content: @Composable () -> Unit) {
    ShowcaseTheme(dark) {
        Box(modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)) {
            content()
        }
    }
}

/**
 * 页面顶栏 = 仿真状态栏 + 真正的 TopAppBar。
 * 状态栏必须放在 Scaffold 的 topBar 槽里，而不是包在 Scaffold 外面——
 * 否则 OverlayDialog 的遮罩盖不到状态栏，弹窗一出来顶部就露一条亮边。
 */
@Composable
fun ScreenTopBar(
    title: String,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
    large: Boolean = true,
    largeTitle: String = title,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Column {
        StatusBar()
        if (large) {
            top.yukonga.miuix.kmp.basic.TopAppBar(
                title = title,
                largeTitle = largeTitle,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                actions = actions,
            )
        } else {
            top.yukonga.miuix.kmp.basic.SmallTopAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                actions = actions,
            )
        }
    }
}

/** 仿真状态栏：左时间、右信号 / Wi-Fi / 电池。纯装饰，不参与布局语义。 */
@Composable
fun StatusBar() {
    val contentColor = MiuixTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(StatusBarHeight)
            .background(MiuixTheme.colorScheme.surface)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "9:41",
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SignalBars(contentColor)
            WifiGlyph(contentColor)
            BatteryGlyph(contentColor)
        }
    }
}

@Composable
private fun SignalBars(color: Color) {
    Canvas(modifier = Modifier.size(width = 18.dp, height = 12.dp)) {
        val barWidth = size.width / 6f
        val gap = barWidth / 2f
        listOf(0.42f, 0.62f, 0.82f, 1f).forEachIndexed { index, ratio ->
            val h = size.height * ratio
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
private fun WifiGlyph(color: Color) {
    Canvas(modifier = Modifier.size(width = 17.dp, height = 12.dp)) {
        val stroke = Stroke(width = size.height * 0.16f, cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height * 0.78f)
        drawArc(
            color = color,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width * 0.06f, size.height * 0.02f),
            size = Size(size.width * 0.88f, size.height * 1.16f),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = 232f,
            sweepAngle = 76f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.3f),
            size = Size(size.width * 0.56f, size.height * 0.74f),
            style = stroke,
        )
        drawCircle(color = color, radius = size.height * 0.09f, center = center)
    }
}

@Composable
private fun BatteryGlyph(color: Color) {
    Canvas(modifier = Modifier.size(width = 26.dp, height = 13.dp)) {
        val strokeWidth = 1.2.dp.toPx()
        val bodyWidth = size.width * 0.86f
        val bodyHeight = size.height * 0.86f
        drawRoundRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(0f, (size.height - bodyHeight) / 2f),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(3.4.dp.toPx(), 3.4.dp.toPx()),
            style = Stroke(width = strokeWidth),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(strokeWidth * 2f, (size.height - bodyHeight) / 2f + strokeWidth * 2f),
            size = Size(bodyWidth * 0.62f, bodyHeight - strokeWidth * 4f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
        drawRoundRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(bodyWidth + 1.4.dp.toPx(), size.height * 0.32f),
            size = Size(size.width * 0.06f, size.height * 0.36f),
            cornerRadius = CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx()),
        )
    }
}

/**
 * 圆形图标底座：品牌 / 功能入口通用。
 * 注意 `squircleSurface` 不提供内容色，图标 tint 必须显式给。
 */
@Composable
fun IconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    container: Color,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .squircleSurface(color = container, cornerRadius = size / 2)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** 列表行左侧的 32dp 应用图标：`squircleSurface` 容器 + 显式 tint。 */
@Composable
fun AppIconTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    container: Color,
    size: Dp = 32.dp,
) {
    Box(
        modifier = Modifier.size(size).squircleSurface(color = container, cornerRadius = size * 0.3f),
        contentAlignment = Alignment.Center,
    ) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.58f),
        )
    }
}
