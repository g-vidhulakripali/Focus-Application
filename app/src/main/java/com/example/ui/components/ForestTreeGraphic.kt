package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.TreeSpecies
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ForestTreeGraphic(
    progress: Float,
    species: TreeSpecies,
    isWithered: Boolean = false,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 260.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "tree_growth"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "tree_ambient")
    val swayOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val groundY = height * 0.85f

            // 1. Draw Mystic Island / Earth Soil base
            drawGround(centerX, groundY, width, height, species, isWithered, pulseGlow)

            if (isWithered) {
                // Withered Tree Rendering
                drawWitheredTree(centerX, groundY, width, height, swayOffset)
            } else {
                // 2. Draw Tree based on growth progress
                drawGrowingTree(
                    progress = animatedProgress,
                    species = species,
                    centerX = centerX,
                    groundY = groundY,
                    width = width,
                    height = height,
                    swayOffset = swayOffset,
                    pulseGlow = pulseGlow,
                    particlePhase = particlePhase
                )
            }
        }
    }
}

private fun DrawScope.drawGround(
    centerX: Float,
    groundY: Float,
    width: Float,
    height: Float,
    species: TreeSpecies,
    isWithered: Boolean,
    pulseGlow: Float
) {
    // Glowing Arcane Base Circle / Island
    val baseRadiusX = width * 0.42f
    val baseRadiusY = height * 0.11f

    val baseColor1 = if (isWithered) Color(0xFF2C2D2E) else Color(species.primaryColor).copy(alpha = 0.25f * pulseGlow)
    val baseColor2 = if (isWithered) Color(0xFF1E1F20) else Color(species.secondaryColor).copy(alpha = 0.05f)

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(baseColor1, baseColor2, Color.Transparent),
            center = Offset(centerX, groundY),
            radius = baseRadiusX * 1.3f
        ),
        topLeft = Offset(centerX - baseRadiusX * 1.3f, groundY - baseRadiusY * 1.3f),
        size = Size(baseRadiusX * 2.6f, baseRadiusY * 2.6f)
    )

    // Soil Mound
    val soilColor = if (isWithered) Color(0xFF3F3B36) else Color(0xFF1B382B)
    drawOval(
        color = soilColor,
        topLeft = Offset(centerX - baseRadiusX, groundY - baseRadiusY),
        size = Size(baseRadiusX * 2f, baseRadiusY * 2f)
    )

    // Runes / Runic perimeter ring
    val ringColor = if (isWithered) Color(0xFF555555) else Color(species.foliageColor).copy(alpha = 0.6f * pulseGlow)
    drawOval(
        color = ringColor,
        topLeft = Offset(centerX - baseRadiusX * 0.88f, groundY - baseRadiusY * 0.88f),
        size = Size(baseRadiusX * 1.76f, baseRadiusY * 1.76f),
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawGrowingTree(
    progress: Float,
    species: TreeSpecies,
    centerX: Float,
    groundY: Float,
    width: Float,
    height: Float,
    swayOffset: Float,
    pulseGlow: Float,
    particlePhase: Float
) {
    val trunkColor = Color(0xFF4A3525)
    val foliagePrimary = Color(species.foliageColor)
    val foliageSecondary = Color(species.primaryColor)

    if (progress < 0.20f) {
        // STAGE 1: Seedling of Knowledge (Seed + tiny runic sprout)
        val seedRadius = 12f + progress * 20f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(foliagePrimary, foliageSecondary),
                center = Offset(centerX, groundY - 14f),
                radius = seedRadius
            ),
            radius = seedRadius,
            center = Offset(centerX, groundY - 14f)
        )
        // Little sprout leaves
        val sproutProgress = (progress / 0.20f)
        if (sproutProgress > 0.3f) {
            val leafLength = 18f * sproutProgress
            val path = Path().apply {
                moveTo(centerX, groundY - 20f)
                cubicTo(
                    centerX - leafLength, groundY - 35f,
                    centerX - leafLength * 0.5f, groundY - 45f,
                    centerX, groundY - 40f
                )
                close()
            }
            drawPath(path, color = foliagePrimary)
        }
    } else if (progress < 0.50f) {
        // STAGE 2: Sprouting Sapling
        val stageProgress = (progress - 0.20f) / 0.30f
        val trunkHeight = (height * 0.35f) * stageProgress
        val topY = groundY - trunkHeight

        // Trunk
        val trunkPath = Path().apply {
            moveTo(centerX - 10f, groundY)
            lineTo(centerX + 10f, groundY)
            lineTo(centerX + 4f + swayOffset * 0.5f, topY)
            lineTo(centerX - 4f + swayOffset * 0.5f, topY)
            close()
        }
        drawPath(trunkPath, color = trunkColor)

        // Growing Foliage clusters
        val foliageRadius = (width * 0.16f) * (0.5f + stageProgress * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(foliagePrimary, foliageSecondary),
                center = Offset(centerX + swayOffset, topY),
                radius = foliageRadius
            ),
            radius = foliageRadius,
            center = Offset(centerX + swayOffset, topY)
        )
    } else {
        // STAGE 3 & 4: Mature & Ancient Tree
        val stageProgress = (progress - 0.50f) / 0.50f
        val maxTrunkHeight = height * 0.52f
        val trunkHeight = maxTrunkHeight * (0.75f + stageProgress * 0.25f)
        val treeTopY = groundY - trunkHeight

        // Roots
        val rootsPath = Path().apply {
            moveTo(centerX - 24f, groundY)
            lineTo(centerX + 24f, groundY)
            lineTo(centerX + 14f, groundY - 20f)
            lineTo(centerX - 14f, groundY - 20f)
            close()
        }
        drawPath(rootsPath, color = trunkColor)

        // Main Trunk
        val trunkWidthBase = 18f + stageProgress * 6f
        val trunkWidthTop = 8f + stageProgress * 4f
        val trunkPath = Path().apply {
            moveTo(centerX - trunkWidthBase, groundY)
            cubicTo(
                centerX - trunkWidthBase * 0.8f, groundY - trunkHeight * 0.5f,
                centerX - trunkWidthTop + swayOffset * 0.4f, groundY - trunkHeight * 0.8f,
                centerX - trunkWidthTop + swayOffset, treeTopY
            )
            lineTo(centerX + trunkWidthTop + swayOffset, treeTopY)
            cubicTo(
                centerX + trunkWidthTop + swayOffset * 0.4f, groundY - trunkHeight * 0.8f,
                centerX + trunkWidthBase * 0.8f, groundY - trunkHeight * 0.5f,
                centerX + trunkWidthBase, groundY
            )
            close()
        }
        drawPath(trunkPath, color = trunkColor)

        // Branches
        val branchY = groundY - trunkHeight * 0.65f
        val branchPath = Path().apply {
            // Left branch
            moveTo(centerX - 6f, branchY)
            cubicTo(
                centerX - width * 0.15f, branchY - 10f,
                centerX - width * 0.22f, branchY - 30f,
                centerX - width * 0.25f + swayOffset, branchY - 45f
            )
            // Right branch
            moveTo(centerX + 6f, branchY + 10f)
            cubicTo(
                centerX + width * 0.15f, branchY,
                centerX + width * 0.22f, branchY - 20f,
                centerX + width * 0.25f + swayOffset, branchY - 40f
            )
        }
        drawPath(branchPath, color = trunkColor, style = Stroke(width = 7f + stageProgress * 3f))

        // Canopy Foliage Clusters (Multi-layered cloud canopy)
        val canopyScale = 0.7f + stageProgress * 0.35f
        val mainRadius = (width * 0.28f) * canopyScale

        // Center Main Canopy
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(foliagePrimary, foliageSecondary),
                center = Offset(centerX + swayOffset, treeTopY - 15f),
                radius = mainRadius
            ),
            radius = mainRadius,
            center = Offset(centerX + swayOffset, treeTopY - 15f)
        )

        // Left Foliage Cloud
        val leftRadius = (width * 0.20f) * canopyScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(foliagePrimary.copy(alpha = 0.95f), foliageSecondary),
                center = Offset(centerX - width * 0.20f + swayOffset * 0.8f, branchY - 35f),
                radius = leftRadius
            ),
            radius = leftRadius,
            center = Offset(centerX - width * 0.20f + swayOffset * 0.8f, branchY - 35f)
        )

        // Right Foliage Cloud
        val rightRadius = (width * 0.21f) * canopyScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(foliagePrimary.copy(alpha = 0.95f), foliageSecondary),
                center = Offset(centerX + width * 0.20f + swayOffset * 0.8f, branchY - 30f),
                radius = rightRadius
            ),
            radius = rightRadius,
            center = Offset(centerX + width * 0.20f + swayOffset * 0.8f, branchY - 30f)
        )

        // Top Crown Cloud
        val crownRadius = (width * 0.18f) * canopyScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(foliagePrimary, Color(species.secondaryColor)),
                center = Offset(centerX + swayOffset * 1.2f, treeTopY - mainRadius * 0.7f),
                radius = crownRadius
            ),
            radius = crownRadius,
            center = Offset(centerX + swayOffset * 1.2f, treeTopY - mainRadius * 0.7f)
        )

        // Floating Arcane Fireflies / Spores if >= 80% or 100%
        if (progress >= 0.75f) {
            val particleCount = if (progress >= 1.0f) 8 else 4
            for (i in 0 until particleCount) {
                val angle = particlePhase + (i * (6.28f / particleCount))
                val distance = (width * 0.32f) * (0.8f + 0.2f * sin(angle * 2f))
                val px = centerX + cos(angle) * distance
                val py = treeTopY + sin(angle) * (height * 0.20f)
                val pSize = 3.5f + 2f * sin(angle * 3f)

                drawCircle(
                    color = Color(species.foliageColor).copy(alpha = 0.75f * pulseGlow),
                    radius = pSize,
                    center = Offset(px, py)
                )
            }
        }

        // Open Thesis Tome / Ancient Scroll at base if 100% complete
        if (progress >= 1.0f) {
            drawThesisTome(centerX, groundY - 4f)
        }
    }
}

private fun DrawScope.drawThesisTome(centerX: Float, tomeY: Float) {
    val bookWidth = 36f
    val bookHeight = 16f

    // Open Book Wings
    val leftPage = Path().apply {
        moveTo(centerX, tomeY)
        cubicTo(centerX - bookWidth * 0.5f, tomeY - 4f, centerX - bookWidth, tomeY - 2f, centerX - bookWidth, tomeY + bookHeight * 0.6f)
        lineTo(centerX, tomeY + bookHeight)
        close()
    }
    drawPath(leftPage, color = Color(0xFFFBF8E8))

    val rightPage = Path().apply {
        moveTo(centerX, tomeY)
        cubicTo(centerX + bookWidth * 0.5f, tomeY - 4f, centerX + bookWidth, tomeY - 2f, centerX + bookWidth, tomeY + bookHeight * 0.6f)
        lineTo(centerX, tomeY + bookHeight)
        close()
    }
    drawPath(rightPage, color = Color(0xFFF3EED2))

    // Book Cover Spine
    drawLine(
        color = Color(0xFF854D0E),
        start = Offset(centerX, tomeY - 2f),
        end = Offset(centerX, tomeY + bookHeight + 2f),
        strokeWidth = 3.5f
    )
}

private fun DrawScope.drawWitheredTree(
    centerX: Float,
    groundY: Float,
    width: Float,
    height: Float,
    swayOffset: Float
) {
    val witheredColor = Color(0xFF4B4846)
    val ashTrunkColor = Color(0xFF383533)
    val trunkHeight = height * 0.45f
    val treeTopY = groundY - trunkHeight

    // Twisted withered trunk
    val trunkPath = Path().apply {
        moveTo(centerX - 12f, groundY)
        cubicTo(
            centerX - 16f, groundY - trunkHeight * 0.5f,
            centerX + swayOffset * 0.5f, groundY - trunkHeight * 0.8f,
            centerX + swayOffset, treeTopY
        )
        lineTo(centerX + 8f + swayOffset, treeTopY)
        cubicTo(
            centerX + 6f + swayOffset * 0.5f, groundY - trunkHeight * 0.8f,
            centerX + 12f, groundY - trunkHeight * 0.5f,
            centerX + 12f, groundY
        )
        close()
    }
    drawPath(trunkPath, color = ashTrunkColor)

    // Bare drooping branches
    val branchPath = Path().apply {
        // Left drooping branch
        moveTo(centerX - 4f, groundY - trunkHeight * 0.6f)
        cubicTo(
            centerX - width * 0.15f, groundY - trunkHeight * 0.55f,
            centerX - width * 0.25f, groundY - trunkHeight * 0.45f,
            centerX - width * 0.30f, groundY - trunkHeight * 0.35f
        )
        // Right drooping branch
        moveTo(centerX + 4f, groundY - trunkHeight * 0.7f)
        cubicTo(
            centerX + width * 0.12f, groundY - trunkHeight * 0.65f,
            centerX + width * 0.22f, groundY - trunkHeight * 0.55f,
            centerX + width * 0.28f, groundY - trunkHeight * 0.42f
        )
        // Upper broken split
        moveTo(centerX, treeTopY)
        lineTo(centerX - 15f, treeTopY - 20f)
        moveTo(centerX + 4f, treeTopY)
        lineTo(centerX + 18f, treeTopY - 15f)
    }
    drawPath(branchPath, color = witheredColor, style = Stroke(width = 4.5f))

    // A few dead fallen leaves on the ground
    drawCircle(Color(0xFF6B5E52), radius = 4f, center = Offset(centerX - 35f, groundY - 4f))
    drawCircle(Color(0xFF6B5E52), radius = 3.5f, center = Offset(centerX + 28f, groundY - 3f))
    drawCircle(Color(0xFF524A42), radius = 3f, center = Offset(centerX - 12f, groundY - 2f))
}
