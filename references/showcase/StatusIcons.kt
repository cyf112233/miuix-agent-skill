package showcase

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 状态卡的水印图标：圆环 + 勾 / 感叹号，描边 9（viewport 100）。
 *
 * 这两个矢量直接移植自 lyricon 的 `ic_home_status_check.xml` / `ic_home_status_warning.xml`
 * （Apache-2.0），不是随手画的：圆环形图标被卡片右下角裁掉一部分仍然成立，
 * 换成一个「裸勾」裁掉就变成一道斜线了。
 */
object StatusIcons {
    private const val VIEWPORT = 100f
    private const val STROKE = 9f

    // miuix-lint:allow 矢量内部颜色会被 Icon(tint = ) 整体覆盖，这里只需要一个不透明占位色
    private val Ink = SolidColor(Color.Black)

    val Check: ImageVector by lazy {
            ImageVector.Builder(
            name = "StatusCheck",
            defaultWidth = 128.dp,
            defaultHeight = 128.dp,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT,
        ).apply {
            ring()
            path(
                stroke = Ink,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(29f, 50f)
                lineTo(44f, 65f)
                lineTo(73f, 35f)
            }
        }.build()
    }

    val Warning: ImageVector by lazy {
        ImageVector.Builder(
            name = "StatusWarning",
            defaultWidth = 128.dp,
            defaultHeight = 128.dp,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT,
        ).apply {
            ring()
            path(
                stroke = Ink,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(50f, 27f)
                lineTo(50f, 46f)
            }
            path(fill = Ink) {
                moveTo(45.5f, 59f)
                lineTo(54.5f, 59f)
                lineTo(54.5f, 68f)
                lineTo(45.5f, 68f)
                close()
            }
        }.build()
    }

    private fun ImageVector.Builder.ring() {
        path(stroke = Ink, strokeLineWidth = STROKE) {
            moveTo(50f, 5f)
            arcTo(45f, 45f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 50f, y1 = 95f)
            arcTo(45f, 45f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 50f, y1 = 5f)
            close()
        }
    }
}
