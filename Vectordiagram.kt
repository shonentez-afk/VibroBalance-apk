package com.vibrobalance.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibrobalance.app.domain.vector_math.Complex
import com.vibrobalance.app.ui.screens.balancing.PhaseDirection
import com.vibrobalance.app.ui.theme.VbColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Фирменная палитра диаграммы.
 * Вынесена сюда, а не разбросана по drawScope-литералам, чтобы любой,
 * кто переоформляет тему приложения, менял её в одном месте.
 * Соответствует токенам VbColors из ui/theme — тёмный фон, один акцент,
 * приглушённая сетка вместо "светофора" из исходной версии.
 */
private object DiagramPalette {
    val background = Color(0xFF15161A)      // surface-1 приложения
    val gridLine = Color(0x14FFFFFF)        // едва заметная сетка, 8% белого
    val axisLine = Color(0x24FFFFFF)        // оси чуть заметнее сетки
    val bladeLine = Color(0x33FFFFFF)       // пунктир лопастей
    val border = Color(0x1FFFFFFF)          // тонкая рамка карточки, не синяя обводка
    val labelBg = Color(0xE6121317)         // подложка подписи — тёмная, не белая
    val labelText = Color(0xFFE8E9EC)
    val centerDot = Color(0xFF5A5A62)
}

data class VectorData(
    val name: String,
    val complex: Complex,
    val color: Color,
    val dashStyle: DashStyle = DashStyle.SOLID
)

enum class DashStyle {
    SOLID, DASH, DOT
}

@Composable
fun VectorDiagram(
    vectors: List<VectorData>,
    numBlades: Int,
    phaseDirection: PhaseDirection,
    showBlades: Boolean,
    modifier: Modifier = Modifier
) {
    val maxAmplitude = vectors.maxOfOrNull { it.complex.amplitude }?.times(1.3) ?: 100.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            // Было: белая подложка + синяя 2dp рамка Material — узнаваемый
            // системный виджет. Теперь — тёмная карточка с волосяной рамкой,
            // как остальные карточки приложения (12dp радиус, 1dp граница).
            .clip(RoundedCornerShape(16.dp))
            .background(DiagramPalette.background)
            .border(1.dp, DiagramPalette.border, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(40.dp)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = min(size.width, size.height) / 2f

            // Сетка: было 4 ярких серых кольца на белом — теперь почти
            // невидимая сетка на тёмном фоне, не конкурирует с векторами.
            for (i in 1..4) {
                drawCircle(
                    color = DiagramPalette.gridLine,
                    radius = radius * i / 4f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1f)
                )
            }

            // Оси
            drawLine(
                color = DiagramPalette.axisLine,
                start = Offset(centerX - radius, centerY),
                end = Offset(centerX + radius, centerY),
                strokeWidth = 1f
            )
            drawLine(
                color = DiagramPalette.axisLine,
                start = Offset(centerX, centerY - radius),
                end = Offset(centerX, centerY + radius),
                strokeWidth = 1f
            )

            // Точка идеального баланса в центре — визуальная цель,
            // к которой стремится вектор после коррекции.
            drawCircle(
                color = DiagramPalette.centerDot,
                radius = 3f,
                center = Offset(centerX, centerY)
            )

            // Лопасти
            if (showBlades && numBlades > 0) {
                for (i in 0 until numBlades) {
                    val angleDeg = i * 360.0 / numBlades
                    val angleRad = Math.toRadians(
                        if (phaseDirection == PhaseDirection.CW) (360 - angleDeg) else angleDeg
                    )
                    val endX = centerX + radius * 0.95f * cos(angleRad).toFloat()
                    val endY = centerY - radius * 0.95f * sin(angleRad).toFloat()

                    drawLine(
                        color = DiagramPalette.bladeLine,
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }
            }

            // Векторы — убрана отрисовка "тени" (полупрозрачный дубль линии
            // со смещением 2px), это был псевдо-drop-shadow и противоречит
            // плоскому стилю остального приложения. Толщина линии чуть
            // уменьшена (5f → 3.5f) под более тонкие обводки фирменного стиля.
            vectors.forEach { vector ->
                val amplitude = vector.complex.amplitude.toFloat()
                var phaseDeg = vector.complex.phaseDeg.toFloat()
                if (phaseDeg < 0) phaseDeg += 360f

                val displayPhase = if (phaseDirection == PhaseDirection.CW) {
                    (360f - phaseDeg) % 360f
                } else {
                    phaseDeg
                }

                val angleRad = Math.toRadians(displayPhase.toDouble())
                val ratio = (amplitude / maxAmplitude.toFloat()).toFloat()
                val endX = centerX + ratio * radius * cos(angleRad).toFloat()
                val endY = centerY - ratio * radius * sin(angleRad).toFloat()

                val pathEffect = when (vector.dashStyle) {
                    DashStyle.SOLID -> null
                    DashStyle.DASH -> PathEffect.dashPathEffect(floatArrayOf(14f, 9f))
                    DashStyle.DOT -> PathEffect.dashPathEffect(floatArrayOf(4f, 5f))
                }

                drawLine(
                    color = vector.color,
                    start = Offset(centerX, centerY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.5f,
                    pathEffect = pathEffect
                )

                // Маркер на конце вектора: было белое кольцо на цветном
                // фоне (типичный Material-стиль) — теперь цветная точка
                // на фоне диаграммы, тоньше и спокойнее.
                drawCircle(
                    color = DiagramPalette.background,
                    radius = 9f,
                    center = Offset(endX, endY)
                )
                drawCircle(
                    color = vector.color,
                    radius = 6f,
                    center = Offset(endX, endY)
                )
            }
        }

        // Подписи векторов — карточка с моноширинными цифрами вместо
        // обычного полужирного текста, чтобы согласоваться с остальным
        // фирменным стилем приложения (см. экраны истории/результатов).
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            vectors.forEach { vector ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .background(DiagramPalette.labelBg, RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(vector.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = vector.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DiagramPalette.labelText.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${"%.1f".format(vector.complex.amplitude)} ∠${"%.0f".format(vector.complex.phaseDeg)}°",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = DiagramPalette.labelText
                    )
                }
            }
        }
    }
}
