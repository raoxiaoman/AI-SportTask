package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import ui.theme.AppDimens
import ui.theme.AppTheme

/**
 * 全站图标注册表 —— 唯一允许出现图标定义的地方。
 *
 * 设计规范（与 UI 评审方案一致）：
 * - 24dp 视口、2dp 描边、圆角端点/拐角，单色线性风格
 * - 语义正确：每个名字对应真实含义，禁止跨语义借用
 * - 颜色由 [AppIcon] 的 tint 统一着色，图标本身不携带颜色
 * - 页面只准引用 AppIcons.X，不允许出现图标字面量
 */
private const val STROKE_WIDTH = 2f

private fun strokeIcon(
    name: String,
    vararg paths: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    for (p in paths) {
        builder.path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = p
        )
    }
    return builder.build()
}

object AppIcons {

    // ---------- 底部导航 ----------
    /** 训练：哑铃 */
    val Training = strokeIcon("dumbbell",
        { moveTo(7f, 12f); lineTo(17f, 12f) },
        { moveTo(5f, 7.5f); lineTo(5f, 16.5f) },
        { moveTo(19f, 7.5f); lineTo(19f, 16.5f) },
        { moveTo(2.7f, 9.6f); lineTo(2.7f, 14.4f) },
        { moveTo(21.3f, 9.6f); lineTo(21.3f, 14.4f) }
    )

    /** 分组：层叠 */
    val Groups = strokeIcon("layers",
        { moveTo(12f, 3f); lineTo(21f, 8f); lineTo(12f, 13f); lineTo(3f, 8f); close() },
        { moveTo(4.2f, 12.6f); lineTo(12f, 17.1f); lineTo(19.8f, 12.6f) },
        { moveTo(4.2f, 16.2f); lineTo(12f, 20.7f); lineTo(19.8f, 16.2f) }
    )

    /** 打卡：日历 + 对勾 */
    val Checkin = strokeIcon("calendar_check",
        { moveTo(4f, 6.5f); lineTo(20f, 6.5f); lineTo(20f, 20.5f); lineTo(4f, 20.5f); close() },
        { moveTo(8f, 3.8f); lineTo(8f, 6.5f) },
        { moveTo(16f, 3.8f); lineTo(16f, 6.5f) },
        { moveTo(4f, 10.5f); lineTo(20f, 10.5f) },
        { moveTo(9.1f, 15.7f); lineTo(11.3f, 17.9f); lineTo(15.1f, 13.5f) }
    )

    /** 统计：柱状图 */
    val Stats = strokeIcon("bar_chart",
        { moveTo(4f, 4f); lineTo(4f, 20f); lineTo(20f, 20f) },
        { moveTo(8.6f, 20f); lineTo(8.6f, 13.2f) },
        { moveTo(12.6f, 20f); lineTo(12.6f, 8.2f) },
        { moveTo(16.6f, 20f); lineTo(16.6f, 16f) }
    )

    /** 设置：齿轮 */
    val Settings = strokeIcon("gear",
        { moveTo(15.2f, 12f); curveTo(15.2f, 13.77f, 13.77f, 15.2f, 12f, 15.2f); curveTo(10.23f, 15.2f, 8.8f, 13.77f, 8.8f, 12f); curveTo(8.8f, 10.23f, 10.23f, 8.8f, 12f, 8.8f); curveTo(13.77f, 8.8f, 15.2f, 10.23f, 15.2f, 12f); close() },
        { moveTo(12f, 3.2f); lineTo(12f, 5.8f) },
        { moveTo(12f, 18.2f); lineTo(12f, 20.8f) },
        { moveTo(5.8f, 12f); lineTo(3.2f, 12f) },
        { moveTo(18.2f, 12f); lineTo(20.8f, 12f) },
        { moveTo(7.62f, 7.62f); lineTo(5.78f, 5.78f) },
        { moveTo(16.38f, 7.62f); lineTo(18.22f, 5.78f) },
        { moveTo(7.62f, 16.38f); lineTo(5.78f, 18.22f) },
        { moveTo(16.38f, 16.38f); lineTo(18.22f, 18.22f) }
    )

    /** 首页：房屋 */
    val Home = strokeIcon("home",
        { moveTo(3.5f, 10.2f); lineTo(12f, 3.5f); lineTo(20.5f, 10.2f) },
        { moveTo(5.7f, 9f); lineTo(5.7f, 20.5f); lineTo(18.3f, 20.5f); lineTo(18.3f, 9f) }
    )

    // ---------- 通用操作 ----------
    val Search = strokeIcon("search",
        { moveTo(15.9f, 9.6f); curveTo(15.9f, 12.53f, 13.53f, 14.9f, 10.6f, 14.9f); curveTo(7.67f, 14.9f, 5.3f, 12.53f, 5.3f, 9.6f); curveTo(5.3f, 6.67f, 7.67f, 4.3f, 10.6f, 4.3f); curveTo(13.53f, 4.3f, 15.9f, 6.67f, 15.9f, 9.6f); close() },
        { moveTo(14.5f, 13.5f); lineTo(20.2f, 19.2f) }
    )

    val Delete = strokeIcon("trash",
        { moveTo(4.5f, 7f); lineTo(19.5f, 7f) },
        { moveTo(9.5f, 7f); lineTo(9.5f, 4f); lineTo(14.5f, 4f); lineTo(14.5f, 7f) },
        { moveTo(6.5f, 7f); lineTo(6.5f, 20f); lineTo(17.5f, 20f); lineTo(17.5f, 7f) },
        { moveTo(10.3f, 10.5f); lineTo(10.3f, 16.5f) },
        { moveTo(13.7f, 10.5f); lineTo(13.7f, 16.5f) }
    )

    /** 编辑/记录：铅笔 */
    val Edit = strokeIcon("pencil",
        { moveTo(17f, 3.5f); lineTo(20.5f, 7f); lineTo(8f, 19.5f); lineTo(4.5f, 19.5f); lineTo(4.5f, 16f); close() },
        { moveTo(14.8f, 5.7f); lineTo(18.3f, 9.2f) }
    )
    val Note = Edit

    val Folder = strokeIcon("folder",
        { moveTo(3.5f, 19.5f); lineTo(3.5f, 6.5f); lineTo(9f, 6.5f); lineTo(11f, 9f); lineTo(20.5f, 9f); lineTo(20.5f, 19.5f); close() }
    )

    /** 收纳：立方体 */
    val Package = strokeIcon("box",
        { moveTo(12f, 3f); lineTo(20f, 7.5f); lineTo(20f, 16.5f); lineTo(12f, 21f); lineTo(4f, 16.5f); lineTo(4f, 7.5f); close() },
        { moveTo(4f, 7.5f); lineTo(12f, 12f); lineTo(20f, 7.5f) },
        { moveTo(12f, 12f); lineTo(12f, 21f) }
    )

    val Info = strokeIcon("info",
        { moveTo(20f, 12f); curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f); curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f); curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f); curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f); close() },
        { moveTo(12f, 11f); lineTo(12f, 16.3f) },
        { moveTo(12f, 7.7f); lineTo(12f, 7.75f) }
    )
    val Book = strokeIcon("book",
        { moveTo(12f, 6.2f); curveTo(10.2f, 4.8f, 7.4f, 4.6f, 4.5f, 5.4f); lineTo(4.5f, 19f); curveTo(7.4f, 18.2f, 10.2f, 18.4f, 12f, 19.8f); curveTo(13.8f, 18.4f, 16.6f, 18.2f, 19.5f, 19f); lineTo(19.5f, 5.4f); curveTo(16.6f, 4.6f, 13.8f, 4.8f, 12f, 6.2f); close() },
        { moveTo(12f, 6.2f); lineTo(12f, 19.8f) }
    )

    // ---------- 状态与激励 ----------
    val Celebrate = strokeIcon("star",
        { moveTo(12f, 3.5f); lineTo(14.5f, 9f); lineTo(20.4f, 9.7f); lineTo(16f, 13.7f); lineTo(17.2f, 19.5f); lineTo(12f, 16.6f); lineTo(6.8f, 19.5f); lineTo(8f, 13.7f); lineTo(3.6f, 9.7f); lineTo(9.5f, 9f); close() }
    )

    /** 力量：心形 */
    val Strength = strokeIcon("heart",
        { moveTo(12f, 20.3f); curveTo(7f, 15.9f, 3.8f, 13f, 3.8f, 9.2f); curveTo(3.8f, 6.5f, 5.9f, 4.4f, 8.6f, 4.4f); curveTo(10f, 4.4f, 11.2f, 5.1f, 12f, 6.2f); curveTo(12.8f, 5.1f, 14f, 4.4f, 15.4f, 4.4f); curveTo(18.1f, 4.4f, 20.2f, 6.5f, 20.2f, 9.2f); curveTo(20.2f, 13f, 17f, 15.9f, 12f, 20.3f); close() }
    )

    /** 连续：火焰 */
    val Streak = strokeIcon("flame",
        { moveTo(12f, 3.2f); curveTo(13.8f, 5.8f, 16.6f, 8.4f, 16.6f, 12.1f); curveTo(16.6f, 15f, 14.5f, 17.2f, 12f, 17.2f); curveTo(9.5f, 17.2f, 7.4f, 15f, 7.4f, 12.1f); curveTo(7.4f, 10.6f, 8.1f, 9.3f, 9f, 8f); curveTo(9.1f, 9.8f, 9.9f, 10.9f, 10.9f, 10.9f); curveTo(11.9f, 10.9f, 12.4f, 9.9f, 12.4f, 8.2f); curveTo(12.4f, 6.4f, 12f, 5f, 12f, 3.2f); close() }
    )

    /** 休息/夜晚：月亮 */
    val Rest = strokeIcon("moon",
        { moveTo(20.3f, 14.7f); curveTo(19.2f, 15.4f, 17.9f, 15.8f, 16.5f, 15.8f); curveTo(12.4f, 15.8f, 9.2f, 12.6f, 9.2f, 8.5f); curveTo(9.2f, 7f, 9.6f, 5.5f, 10.4f, 4.3f); curveTo(6.6f, 5.3f, 3.8f, 8.8f, 3.8f, 12.9f); curveTo(3.8f, 17.6f, 7.7f, 21.5f, 12.4f, 21.5f); curveTo(16.4f, 21.5f, 19.8f, 18.6f, 20.3f, 14.7f); close() }
    )
    val DarkMode = Rest

    // ---------- 数据与同步 ----------
    val Sync = strokeIcon("sync",
        { moveTo(5.3f, 8.8f); curveTo(6.8f, 6f, 9.2f, 4.5f, 12f, 4.5f); curveTo(15.6f, 4.5f, 18.6f, 6.9f, 19.4f, 10.2f) },
        { moveTo(19.4f, 10.2f); lineTo(20.1f, 7.4f) },
        { moveTo(19.4f, 10.2f); lineTo(16.5f, 10.9f) },
        { moveTo(18.7f, 15.2f); curveTo(17.2f, 18f, 14.8f, 19.5f, 12f, 19.5f); curveTo(8.4f, 19.5f, 5.4f, 17.1f, 4.6f, 13.8f) },
        { moveTo(4.6f, 13.8f); lineTo(3.9f, 16.6f) },
        { moveTo(4.6f, 13.8f); lineTo(7.5f, 13.1f) }
    )
    val Refresh = Sync

    val Upload = strokeIcon("upload",
        { moveTo(4f, 14.5f); lineTo(4f, 20f); lineTo(20f, 20f); lineTo(20f, 14.5f) },
        { moveTo(12f, 3.8f); lineTo(12f, 15.5f) },
        { moveTo(8.2f, 7.6f); lineTo(12f, 3.8f); lineTo(15.8f, 7.6f) }
    )

    val Download = strokeIcon("download",
        { moveTo(4f, 14.5f); lineTo(4f, 20f); lineTo(20f, 20f); lineTo(20f, 14.5f) },
        { moveTo(12f, 3.8f); lineTo(12f, 15.5f) },
        { moveTo(8.2f, 11.7f); lineTo(12f, 15.5f); lineTo(15.8f, 11.7f) }
    )

    /** 服务器/网络：地球 */
    val Link = strokeIcon("globe",
        { moveTo(20f, 12f); curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f); curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f); curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f); curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f); close() },
        { moveTo(4f, 12f); lineTo(20f, 12f) },
        { moveTo(12f, 4f); curveTo(14.6f, 7.6f, 14.6f, 16.4f, 12f, 20f); curveTo(9.4f, 16.4f, 9.4f, 7.6f, 12f, 4f); close() }
    )

    val Logout = strokeIcon("logout",
        { moveTo(14.5f, 4.5f); lineTo(18.5f, 4.5f); lineTo(18.5f, 19.5f); lineTo(14.5f, 19.5f) },
        { moveTo(4.5f, 12f); lineTo(14.5f, 12f) },
        { moveTo(11.3f, 8.4f); lineTo(15.1f, 12f); lineTo(11.3f, 15.6f) }
    )

    val Person = strokeIcon("person",
        { moveTo(15.3f, 7f); curveTo(15.3f, 8.87f, 13.87f, 10.4f, 12f, 10.4f); curveTo(10.13f, 10.4f, 8.7f, 8.87f, 8.7f, 7f); curveTo(8.7f, 5.13f, 10.13f, 3.6f, 12f, 3.6f); curveTo(13.87f, 3.6f, 15.3f, 5.13f, 15.3f, 7f); close() },
        { moveTo(4.5f, 20.5f); curveTo(5.5f, 16.3f, 8.4f, 14.3f, 12f, 14.3f); curveTo(15.6f, 14.3f, 18.5f, 16.3f, 19.5f, 20.5f) }
    )

    val Key = strokeIcon("lock",
        { moveTo(6f, 10.5f); lineTo(18f, 10.5f); lineTo(18f, 20.5f); lineTo(6f, 20.5f); close() },
        { moveTo(8.5f, 10.5f); lineTo(8.5f, 8f); curveTo(8.5f, 5.9f, 10f, 4.3f, 12f, 4.3f); curveTo(14f, 4.3f, 15.5f, 5.9f, 15.5f, 8f); lineTo(15.5f, 10.5f) },
        { moveTo(12f, 14f); lineTo(12f, 15.6f) }
    )

    // ---------- 训练反馈设置 ----------
    val Bell = strokeIcon("bell",
        { moveTo(6f, 16.5f); lineTo(6f, 11f); curveTo(6f, 8f, 8.4f, 5.6f, 12f, 5.6f); curveTo(15.6f, 5.6f, 18f, 8f, 18f, 11f); lineTo(18f, 16.5f) },
        { moveTo(4.5f, 17f); lineTo(19.5f, 17f) },
        { moveTo(10f, 20f); curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f); curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f) }
    )

    val Clock = strokeIcon("clock",
        { moveTo(20.5f, 12f); curveTo(20.5f, 16.7f, 16.7f, 20.5f, 12f, 20.5f); curveTo(7.3f, 20.5f, 3.5f, 16.7f, 3.5f, 12f); curveTo(3.5f, 7.3f, 7.3f, 3.5f, 12f, 3.5f); curveTo(16.7f, 3.5f, 20.5f, 7.3f, 20.5f, 12f); close() },
        { moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15.5f, 14.2f) }
    )

    val Calendar = strokeIcon("calendar",
        { moveTo(4f, 6.5f); lineTo(20f, 6.5f); lineTo(20f, 20.5f); lineTo(4f, 20.5f); close() },
        { moveTo(8f, 3.8f); lineTo(8f, 6.5f) },
        { moveTo(16f, 3.8f); lineTo(16f, 6.5f) },
        { moveTo(4f, 10.5f); lineTo(20f, 10.5f) }
    )

    /** 声音：扬声器 */
    val Sound = strokeIcon("speaker",
        { moveTo(4f, 9.5f); lineTo(8f, 9.5f); lineTo(13f, 5.5f); lineTo(13f, 18.5f); lineTo(8f, 14.5f); lineTo(4f, 14.5f); close() },
        { moveTo(16f, 9.8f); curveTo(17.3f, 10.9f, 17.3f, 13.1f, 16f, 14.2f) },
        { moveTo(18.7f, 7.7f); curveTo(20.9f, 10f, 20.9f, 14f, 18.7f, 16.3f) }
    )

    /** 语音：麦克风 */
    val Voice = strokeIcon("mic",
        { moveTo(9f, 6.5f); curveTo(9f, 5f, 10.3f, 3.5f, 12f, 3.5f); curveTo(13.7f, 3.5f, 15f, 5f, 15f, 6.5f); lineTo(15f, 11.5f); curveTo(15f, 13f, 13.7f, 14.5f, 12f, 14.5f); curveTo(10.3f, 14.5f, 9f, 13f, 9f, 11.5f); close() },
        { moveTo(6.5f, 11.5f); curveTo(6.5f, 15.4f, 9.1f, 18f, 12f, 18f); curveTo(14.9f, 18f, 17.5f, 15.4f, 17.5f, 11.5f) },
        { moveTo(12f, 18f); lineTo(12f, 20.5f) },
        { moveTo(8.5f, 20.5f); lineTo(15.5f, 20.5f) }
    )

    /** 震动：手机 + 波纹 */
    val Vibrate = strokeIcon("vibrate",
        { moveTo(8f, 3.5f); lineTo(16f, 3.5f); lineTo(16f, 20.5f); lineTo(8f, 20.5f); close() },
        { moveTo(4.6f, 8.5f); curveTo(3.9f, 10.2f, 3.9f, 13.8f, 4.6f, 15.5f) },
        { moveTo(19.4f, 8.5f); curveTo(20.1f, 10.2f, 20.1f, 13.8f, 19.4f, 15.5f) }
    )

    // ---------- 提示与方向 ----------
    val Warning = strokeIcon("warning",
        { moveTo(12f, 3.8f); lineTo(21.4f, 20f); lineTo(2.6f, 20f); close() },
        { moveTo(12f, 9.5f); lineTo(12f, 14.5f) },
        { moveTo(12f, 17f); lineTo(12f, 17.1f) }
    )
    val Error = Warning

    val Close = strokeIcon("close",
        { moveTo(6f, 6f); lineTo(18f, 18f) },
        { moveTo(18f, 6f); lineTo(6f, 18f) }
    )
    val Clear = Close

    val Check = strokeIcon("check",
        { moveTo(4.5f, 12.5f); lineTo(9.5f, 17.5f); lineTo(19.5f, 6.5f) }
    )

    val Add = strokeIcon("add",
        { moveTo(12f, 5f); lineTo(12f, 19f) },
        { moveTo(5f, 12f); lineTo(19f, 12f) }
    )

    val Chevron = strokeIcon("chevron_right",
        { moveTo(9.5f, 5.5f); lineTo(15.5f, 12f); lineTo(9.5f, 18.5f) }
    )
    val ChevronUp = strokeIcon("chevron_up",
        { moveTo(5.5f, 14.5f); lineTo(12f, 8f); lineTo(18.5f, 14.5f) }
    )
    val ChevronDown = strokeIcon("chevron_down",
        { moveTo(5.5f, 9.5f); lineTo(12f, 16f); lineTo(18.5f, 9.5f) }
    )

    val Back = strokeIcon("arrow_back",
        { moveTo(19f, 12f); lineTo(5f, 12f) },
        { moveTo(10.5f, 6.5f); lineTo(5f, 12f); lineTo(10.5f, 17.5f) }
    )
    val Forward = strokeIcon("arrow_forward",
        { moveTo(5f, 12f); lineTo(19f, 12f) },
        { moveTo(13.5f, 6.5f); lineTo(19f, 12f); lineTo(13.5f, 17.5f) }
    )

    val Play = strokeIcon("play",
        { moveTo(8f, 5.5f); lineTo(18f, 12f); lineTo(8f, 18.5f); close() }
    )

    /** 视频演示：圆角播放框内三角 */
    val Video = strokeIcon("video",
        {
            moveTo(6f, 4.5f); lineTo(18f, 4.5f); arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20.5f, 7f)
            lineTo(20.5f, 17f); arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 18f, 19.5f)
            lineTo(6f, 19.5f); arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.5f, 17f)
            lineTo(3.5f, 7f); arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 4.5f); close()
        },
        { moveTo(10.3f, 8.8f); lineTo(15.2f, 12f); lineTo(10.3f, 15.2f); close() }
    )

    val Pause = strokeIcon("pause",
        { moveTo(9.2f, 5.5f); lineTo(9.2f, 18.5f) },
        { moveTo(14.8f, 5.5f); lineTo(14.8f, 18.5f) }
    )

    /** 拖拽把手：三点 */
    val DragHandle = strokeIcon("drag_handle",
        { moveTo(12f, 6f); lineTo(12f, 6.8f) },
        { moveTo(12f, 11.6f); lineTo(12f, 12.4f) },
        { moveTo(12f, 17.2f); lineTo(12f, 18f) }
    )

    val Menu = strokeIcon("menu",
        { moveTo(4f, 6.5f); lineTo(20f, 6.5f) },
        { moveTo(4f, 12f); lineTo(20f, 12f) },
        { moveTo(4f, 17.5f); lineTo(20f, 17.5f) }
    )

    val Build = strokeIcon("wrench",
        { moveTo(14.7f, 6.3f); curveTo(13.4f, 5f, 11.4f, 4.8f, 9.9f, 5.7f); lineTo(13f, 8.8f); lineTo(10.5f, 11.3f); lineTo(7.4f, 8.2f); curveTo(6.5f, 9.7f, 6.7f, 11.7f, 8f, 13f); curveTo(9.5f, 14.5f, 11.9f, 14.5f, 13.4f, 13.1f); lineTo(18.6f, 18.3f); curveTo(19.1f, 17.8f, 19.1f, 17f, 18.6f, 16.5f); lineTo(13.4f, 11.3f); curveTo(14.8f, 9.9f, 14.8f, 7.5f, 14.7f, 6.3f); close() }
    )
}

/**
 * 统一图标渲染：24dp 默认尺寸、单色 tint、强制 contentDescription。
 * 用 ColorFilter.tint 保证任何图标资源都能被主题色正确着色。
 */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.inkSub,
    size: Dp = AppDimens.icon
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint)
    )
}
