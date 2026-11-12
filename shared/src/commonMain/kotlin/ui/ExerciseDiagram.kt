package ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import ui.theme.AppTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 动作示意图（图例）体系 —— 全站统一的动作图示。
 *
 * 设计规范：
 * - 100×100 归一化坐标系，y 向下，地面线 y≈91.5
 * - 小人为「图标人」风格：粗圆头线条躯干四肢 + 实心头部
 * - 远侧肢体用 35% 透明度区分前后；道具（墙/垫/带/台阶）用中性色
 * - 方向箭头用品牌色并带呼吸动画，表达动作方向
 * - 颜色全部取自 AppTheme 语义色，禁止硬编码
 */

/** 归一化坐标点 */
data class Pt(val x: Float, val y: Float) {
    companion object {
        fun of(x: Float, y: Float) = Pt(x, y)
    }
}

/** 矩形道具：台阶 / 椅子 / 垫子 / 平衡垫等 */
data class PoseBox(val x: Float, val y: Float, val w: Float, val h: Float, val filled: Boolean = true)

/** 线状道具：墙 / 弹力带 / 毛巾等 */
data class PoseLine(val from: Pt, val to: Pt)

/** 完整动作姿势 */
data class PoseSpec(
    val head: Pt,
    val neck: Pt,
    val hip: Pt,
    /** 近侧手臂 [肘, 手]，肩点与颈点重合 */
    val armNear: List<Pt>,
    /** 远侧手臂 [肘, 手] */
    val armFar: List<Pt>,
    /** 近侧腿 [膝, 脚] */
    val legNear: List<Pt>,
    /** 远侧腿 [膝, 脚] */
    val legFar: List<Pt>,
    /** 动作方向箭头 */
    val arrows: List<Pair<Pt, Pt>> = emptyList(),
    val props: List<PoseLine> = emptyList(),
    val boxes: List<PoseBox> = emptyList(),
    /** 头部半径，默认 5.5 */
    val headR: Float = 5.5f
)

/** 静止站立（面向右侧的通用回退姿势） */
val genericPose = PoseSpec(
    head = Pt(50f, 30f), neck = Pt(49f, 39f), hip = Pt(48f, 62f),
    armNear = listOf(Pt(54f, 50f), Pt(57f, 62f)),
    armFar = listOf(Pt(45f, 50f), Pt(42f, 62f)),
    legNear = listOf(Pt(50f, 76f), Pt(50f, 89f)),
    legFar = listOf(Pt(46f, 76f), Pt(46f, 89f))
)

@Composable
fun ExerciseDiagram(spec: PoseSpec, modifier: Modifier = Modifier) {
    // 箭头呼吸动画：指引视线跟随动作方向
    val transition = rememberInfiniteTransition(label = "arrow")
    val arrowAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowAlpha"
    )

    val ink = AppTheme.colors.ink
    val inkSub = AppTheme.colors.inkSub
    val outline = AppTheme.colors.outline
    val surfaceAlt = AppTheme.colors.surfaceAlt
    val arrow = AppTheme.colors.brandStrong

    Canvas(modifier = modifier) {
        val scale = size.minDimension / 100f
        val left = (size.width - size.minDimension) / 2f
        val top = (size.height - size.minDimension) / 2f
        fun P(p: Pt) = Offset(left + p.x * scale, top + p.y * scale)
        fun L(v: Float) = v * scale

        // 地面线
        drawLine(
            color = outline,
            start = P(Pt(6f, 91.5f)),
            end = P(Pt(94f, 91.5f)),
            strokeWidth = L(2.5f),
            cap = StrokeCap.Round
        )

        // 道具：块
        spec.boxes.forEach { b ->
            val topLeft = P(Pt(b.x, b.y))
            val boxSize = Size(L(b.w), L(b.h))
            if (b.filled) {
                drawRoundRect(
                    color = surfaceAlt,
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(L(3f))
                )
            }
            drawRoundRect(
                color = inkSub,
                topLeft = topLeft,
                size = boxSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(L(3f)),
                style = Stroke(width = L(2f))
            )
        }

        // 道具：线（墙 / 弹力带）
        spec.props.forEach { l ->
            drawLine(
                color = inkSub,
                start = P(l.from),
                end = P(l.to),
                strokeWidth = L(3.5f),
                cap = StrokeCap.Round
            )
        }

        // 远侧肢体（弱化）
        drawLimb(P(spec.neck), spec.armFar.map { P(it) }, ink.copy(alpha = 0.35f), L(6f))
        drawLimb(P(spec.hip), spec.legFar.map { P(it) }, ink.copy(alpha = 0.35f), L(6f))

        // 躯干与近侧肢体
        drawLimb(P(spec.neck), listOf(P(spec.hip)), ink, L(7f))
        drawLimb(P(spec.neck), spec.armNear.map { P(it) }, ink, L(6f))
        drawLimb(P(spec.hip), spec.legNear.map { P(it) }, ink, L(6.5f))

        // 头部
        drawCircle(color = ink, radius = L(spec.headR), center = P(spec.head))

        // 方向箭头（呼吸动画）
        spec.arrows.forEach { (from, to) ->
            drawArrow(P(from), P(to), arrow.copy(alpha = arrowAlpha), L(3.5f), L(7f))
        }
    }
}

/** 连续折线：肩/髋 → 肘/膝 → 手/脚 */
private fun DrawScope.drawLimb(root: Offset, joints: List<Offset>, color: Color, width: Float) {
    var prev = root
    joints.forEach { j ->
        drawLine(color = color, start = prev, end = j, strokeWidth = width, cap = StrokeCap.Round)
        prev = j
    }
}

/** 带箭头的线段 */
private fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color, width: Float, headLen: Float) {
    drawLine(color = color, start = from, end = to, strokeWidth = width, cap = StrokeCap.Round)
    val angle = atan2(to.y - from.y, to.x - from.x)
    val spread = 150f * PI.toFloat() / 180f
    listOf(angle + spread, angle - spread).forEach { a ->
        drawLine(
            color = color,
            start = to,
            end = Offset(
                to.x + headLen * cos(a),
                to.y + headLen * sin(a)
            ),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
}
