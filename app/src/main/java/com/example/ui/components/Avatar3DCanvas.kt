package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AvatarCustomization
import com.example.model.AvatarEmotion
import com.example.model.BackgroundTheme
import com.example.model.PetSpecies
import com.example.model.TouchZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

data class TouchParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val symbol: String,
    val color: Color
)

/**
 * Fully integrated modular 3D cartoon Talking Tom pet avatar.
 *
 * All facial and bodily components (eyes, pupils, eyelids, snout, talking mouth cavity,
 * teeth, tongue, ears, torso, limbs) are natively integrated in a unified 3D mathematical
 * coordinate system:
 * - ZERO ghosting, duplicate layers, or desynchronized overlays.
 * - Dynamic pupil gaze tracking that follows the user's touch.
 * - Natural eyelid blinking and rich emotional expressions (happy squint, laughter, surprise).
 * - Synchronized talking jaw with pearly white cartoon teeth and animated tongue.
 * - Massive 76% screen height presence with dual-pass floor contact shadows on the carpet.
 * - Responsive 3D drag physics and multi-zone touch reactions (Head, Belly, Hands, Feet).
 */
@Composable
fun Avatar3DCanvas(
    species: PetSpecies,
    customization: AvatarCustomization,
    emotion: AvatarEmotion,
    isTalking: Boolean,
    onZoneTouched: (TouchZone) -> Unit,
    modifier: Modifier = Modifier,
    backgroundTheme: BackgroundTheme = BackgroundTheme.LIVING_ROOM
) {
    val coroutineScope = rememberCoroutineScope()

    // 3D Tilt angles (rotationX, rotationY in degrees)
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }

    // Dynamic Gaze Tracking (pupils follow finger/touch location)
    val gazeX = remember { Animatable(0f) }
    val gazeY = remember { Animatable(0f) }

    // Zone reaction animations
    val headNuzzle = remember { Animatable(0f) }      // Head tilts up into petting hand
    val bellyJiggle = remember { Animatable(0f) }     // Belly squash & stretch on tickle
    val handsWave = remember { Animatable(0f) }       // Paw high-five
    val feetJump = remember { Animatable(0f) }        // Jump up holding foot
    val cheekBlushAlpha = remember { Animatable(0f) } // Rosy cheeks on petting
    val earTwitch = remember { Animatable(0f) }       // Playful ear wiggle

    // Active touch reaction zone for expressive rendering
    var activeReactionZone by remember { mutableStateOf<TouchZone?>(null) }

    // Natural eye blinking loop (smooth eyelid closing and opening)
    var blinkProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay((2800..4500).random().toLong())
            val anim = Animatable(0f)
            anim.animateTo(1f, tween(110, easing = LinearEasing))
            blinkProgress = 1f
            anim.animateTo(0f, tween(120, easing = LinearEasing))
            blinkProgress = 0f
        }
    }

    // Occasional playful ear twitch loop
    LaunchedEffect(Unit) {
        while (isActive) {
            delay((3400..5500).random().toLong())
            earTwitch.animateTo(7f, tween(80))
            earTwitch.animateTo(-5f, tween(90))
            earTwitch.animateTo(0f, tween(80))
        }
    }

    // Idle chest breathing & floating physics
    val infiniteTransition = rememberInfiniteTransition(label = "idleTalkingTomModular")
    val idleFloatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleFloat"
    )

    // Breathing chest expansion (squash & stretch)
    val chestBreathScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chestBreath"
    )

    // Tail wagging animation
    val tailWag by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tailWag"
    )

    // Animated mouth speech cycle
    val mouthCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(125, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthCycle"
    )

    // Floating particles on touch
    val touchParticles = remember { mutableStateListOf<TouchParticle>() }

    // Particle cleanup loop
    LaunchedEffect(touchParticles.size) {
        if (touchParticles.isNotEmpty()) {
            delay(1100)
            if (touchParticles.isNotEmpty()) {
                touchParticles.removeAt(0)
            }
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newY = (tiltY.value + dragAmount.x * 0.16f).coerceIn(-18f, 18f)
                            val newX = (tiltX.value + dragAmount.y * 0.12f).coerceIn(-14f, 14f)
                            tiltY.snapTo(newY)
                            tiltX.snapTo(newX)

                            gazeX.snapTo((newY / 18f).coerceIn(-1f, 1f))
                            gazeY.snapTo((newX / 14f).coerceIn(-1f, 1f))
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            tiltX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                        }
                        coroutineScope.launch {
                            tiltY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                        }
                        coroutineScope.launch {
                            gazeX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f))
                            gazeY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f))
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        val charH = h * 0.76f
                        val charTop = h - charH - (h * 0.04f)
                        val relativeY = (offset.y - charTop) / charH
                        val relativeX = (offset.x - (w * 0.5f)) / (w * 0.5f)

                        // Direct pupil gaze to tap point
                        coroutineScope.launch {
                            gazeX.animateTo(relativeX.coerceIn(-1f, 1f), tween(90))
                            gazeY.animateTo((relativeY - 0.25f).coerceIn(-1f, 1f), tween(90))
                            delay(850)
                            gazeX.animateTo(0f, spring(dampingRatio = 0.6f))
                            gazeY.animateTo(0f, spring(dampingRatio = 0.6f))
                        }

                        val zone = when {
                            relativeY < 0.32f -> TouchZone.HEAD
                            relativeY > 0.76f -> TouchZone.FEET
                            abs(relativeX) > 0.44f && relativeY in 0.32f..0.74f -> TouchZone.HANDS
                            else -> TouchZone.BELLY
                        }

                        activeReactionZone = zone
                        onZoneTouched(zone)

                        // Trigger expressive living character physics
                        coroutineScope.launch {
                            when (zone) {
                                TouchZone.HEAD -> {
                                    launch {
                                        cheekBlushAlpha.animateTo(1f, tween(200))
                                        delay(1400)
                                        cheekBlushAlpha.animateTo(0f, tween(400))
                                    }
                                    headNuzzle.animateTo(-16f, spring(dampingRatio = 0.4f, stiffness = 600f))
                                    headNuzzle.animateTo(6f, spring(dampingRatio = 0.5f))
                                    headNuzzle.animateTo(0f, spring(dampingRatio = 0.6f))
                                    activeReactionZone = null
                                }
                                TouchZone.BELLY -> {
                                    bellyJiggle.animateTo(24f, spring(dampingRatio = 0.3f, stiffness = 800f))
                                    bellyJiggle.animateTo(-18f, spring(dampingRatio = 0.35f))
                                    bellyJiggle.animateTo(10f, spring(dampingRatio = 0.45f))
                                    bellyJiggle.animateTo(0f, spring(dampingRatio = 0.6f))
                                    activeReactionZone = null
                                }
                                TouchZone.HANDS -> {
                                    handsWave.animateTo(25f, spring(dampingRatio = 0.4f, stiffness = 650f))
                                    handsWave.animateTo(0f, spring(dampingRatio = 0.6f))
                                    activeReactionZone = null
                                }
                                TouchZone.FEET -> {
                                    feetJump.animateTo(-55f, spring(dampingRatio = 0.35f, stiffness = 550f))
                                    delay(90)
                                    feetJump.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                    activeReactionZone = null
                                }
                            }
                        }

                        val (symbol, color) = when (zone) {
                            TouchZone.HEAD -> "💖" to Color(0xFFFF4081)
                            TouchZone.BELLY -> "✨" to Color(0xFFFFB300)
                            TouchZone.HANDS -> "⭐" to Color(0xFF00B4D8)
                            TouchZone.FEET -> "💫" to Color(0xFF4CAF50)
                        }
                        touchParticles.add(
                            TouchParticle(
                                id = System.currentTimeMillis() + Random.nextLong(1000),
                                x = offset.x,
                                y = offset.y,
                                symbol = symbol,
                                color = color
                            )
                        )
                    }
                )
            }
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("avatar_3d_canvas")
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            // ========================================================
            // 76% SCREEN HEIGHT SCALE (Talking Tom Standard)
            // ========================================================
            val targetHeight = h * 0.76f
            val baseScale = (targetHeight / 480f).coerceIn(1.20f, 2.70f)

            // Anchor point: feet planted firmly on carpet floor
            val floorY = h * 0.84f
            val feetAnchor = 220f * baseScale
            val cy = floorY - feetAnchor
            val dynamicY = cy + idleFloatY + feetJump.value + (headNuzzle.value * 0.4f)

            // Room lighting colors
            val (roomKeyLight, roomRimColor, roomShadowColor) = when (backgroundTheme) {
                BackgroundTheme.LIVING_ROOM -> Triple(
                    Color(0xFFFFF9ED),
                    Color(0xFFFFD54F).copy(alpha = 0.55f),
                    Color(0x95261405)
                )
                BackgroundTheme.BEDROOM -> Triple(
                    Color(0xFFEAF2FD),
                    Color(0xFF81D4FA).copy(alpha = 0.60f),
                    Color(0x900D152B)
                )
                BackgroundTheme.KITCHEN -> Triple(
                    Color(0xFFF0FDF4),
                    Color(0xFFA7F3D0).copy(alpha = 0.55f),
                    Color(0x850E2413)
                )
                else -> Triple(
                    Color(0xFFFFFBEB),
                    Color(0xFFFFE082).copy(alpha = 0.50f),
                    Color(0x851C1710)
                )
            }

            // ========================================================
            // 1. DUAL-PASS FLOOR CONTACT SHADOWS (Room Grounding)
            // ========================================================
            val jumpPercent = (abs(feetJump.value) / 55f).coerceIn(0f, 1f)
            val shadowScale = (1f - jumpPercent * 0.35f)
            val shadowAlpha = (1f - jumpPercent * 0.45f)
            val shadowWidth = 280f * baseScale * shadowScale
            val shadowHeight = 56f * baseScale * shadowScale
            val shadowCenterY = floorY + 10f * baseScale

            // Pass 1: Soft Ambient Occlusion Body Shadow on the rug
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        roomShadowColor.copy(alpha = 0.88f * shadowAlpha),
                        roomShadowColor.copy(alpha = 0.35f * shadowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx + tiltY.value * 1.8f, shadowCenterY),
                    radius = shadowWidth * 0.55f
                ),
                topLeft = Offset(cx - shadowWidth * 0.5f + tiltY.value * 1.8f, shadowCenterY - shadowHeight * 0.5f),
                size = Size(shadowWidth, shadowHeight)
            )

            // Pass 2: Tight Dark Contact Shadows directly under left & right foot
            val footContactSpacing = 68f * baseScale
            val contactShadowW = 68f * baseScale * shadowScale
            val contactShadowH = 22f * baseScale * shadowScale
            val contactColor = Color(0xB5080503).copy(alpha = 0.88f * shadowAlpha)

            drawOval(
                color = contactColor,
                topLeft = Offset(cx - footContactSpacing - contactShadowW * 0.5f, floorY - contactShadowH * 0.2f),
                size = Size(contactShadowW, contactShadowH)
            )
            drawOval(
                color = contactColor,
                topLeft = Offset(cx + footContactSpacing - contactShadowW * 0.5f, floorY - contactShadowH * 0.2f),
                size = Size(contactShadowW, contactShadowH)
            )

            // ========================================================
            // 2. UNIFIED MODULAR 3D CARTOON CHARACTER (100% Integrated)
            // ========================================================
            val rotY = tiltY.value
            val rotX = tiltX.value
            val offsetX3D = rotY * 1.8f
            val offsetY3D = -rotX * 1.2f

            translate(left = offsetX3D, top = offsetY3D) {
                rotate(degrees = rotY * 0.22f, pivot = Offset(cx, dynamicY)) {
                    // Squash & stretch on breathing + tickle
                    val tickleSquash = (bellyJiggle.value * 0.007f) - (feetJump.value * 0.003f)
                    val sX = chestBreathScale + tickleSquash
                    val sY = (2f - chestBreathScale) - tickleSquash

                    scale(scaleX = sX, scaleY = sY, pivot = Offset(cx, floorY)) {
                        drawModularTalkingTomCharacter(
                            species = species,
                            customization = customization,
                            emotion = emotion,
                            cx = cx,
                            cy = dynamicY,
                            scale = baseScale,
                            rotY = rotY,
                            rotX = rotX,
                            gazeX = gazeX.value,
                            gazeY = gazeY.value,
                            blinkProgress = blinkProgress,
                            earTwitch = earTwitch.value,
                            tailWag = tailWag,
                            isTalking = isTalking,
                            mouthCycle = mouthCycle,
                            activeReactionZone = activeReactionZone,
                            cheekBlushAlpha = cheekBlushAlpha.value,
                            roomKeyLight = roomKeyLight,
                            roomRimColor = roomRimColor
                        )
                    }
                }
            }
        }

        // ========================================================
        // 3. FLOATING TOUCH PARTICLES (Hearts, Stars, Sparks)
        // ========================================================
        touchParticles.forEach { p ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(p.x.toInt() - 20, p.y.toInt() - 20) }
                    .size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = p.symbol,
                    fontSize = 26.sp
                )
            }
        }
    }
}

/**
 * Unified modular renderer for all 5 Talking Tom characters.
 * All eyes, eyelids, pupils, snout, teeth, tongue, ears, and limbs
 * exist in one continuous, locked 3D mathematical model.
 */
private fun DrawScope.drawModularTalkingTomCharacter(
    species: PetSpecies,
    customization: AvatarCustomization,
    emotion: AvatarEmotion,
    cx: Float,
    cy: Float,
    scale: Float,
    rotY: Float,
    rotX: Float,
    gazeX: Float,
    gazeY: Float,
    blinkProgress: Float,
    earTwitch: Float,
    tailWag: Float,
    isTalking: Boolean,
    mouthCycle: Float,
    activeReactionZone: TouchZone?,
    cheekBlushAlpha: Float,
    roomKeyLight: Color,
    roomRimColor: Color
) {
    // -------------------------------------------------------------
    // Species-Specific Color Palettes & Proportions
    // -------------------------------------------------------------
    val (primaryFur, darkFur, chestCream) = when (species) {
        PetSpecies.CAT -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFF90A4AE), // Cool grey Talking Tom fur
            Color(0xFF546E7A),
            Color(0xFFECEFF1)
        )
        PetSpecies.RABBIT -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFFFAFAFA), // Soft white Angela fur
            Color(0xFFE0E0E0),
            Color(0xFFFFF0F5)
        )
        PetSpecies.DOG -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFFE5A93C), // Warm golden brown dog fur
            Color(0xFFB27B1E),
            Color(0xFFFFF8E7)
        )
        PetSpecies.CAPYBARA -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFF8D6E63), // Caramel brown capybara fur
            Color(0xFF5D4037),
            Color(0xFFD7CCC8)
        )
        PetSpecies.BEAR -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFF5D4037), // Chocolate grizzly fur
            Color(0xFF3E2723),
            Color(0xFFD7CCC8)
        )
    }

    // Iris colors
    val (irisTop, irisBottom) = when (species) {
        PetSpecies.CAT -> Color(0xFF00C853) to Color(0xFF69F0AE)      // Zayn: Sparkling emerald cat eyes
        PetSpecies.RABBIT -> Color(0xFFE91E63) to Color(0xFFFF80AB)   // Siti: Loving ruby-pink anime eyes
        PetSpecies.DOG -> Color(0xFFFF8F00) to Color(0xFFFFD54F)      // Rizky: Golden amber loyal dog eyes
        PetSpecies.CAPYBARA -> Color(0xFF5D4037) to Color(0xFF8D6E63) // Jamal: Relaxed hazel eyes
        PetSpecies.BEAR -> Color(0xFFE65100) to Color(0xFFFFB300)     // Heru: Honey amber bear eyes
    }

    val noseColor = when (species) {
        PetSpecies.CAT, PetSpecies.RABBIT -> Color(0xFFFF8A80)
        PetSpecies.DOG -> Color(0xFF263238)
        PetSpecies.CAPYBARA -> Color(0xFF4E342E)
        PetSpecies.BEAR -> Color(0xFF212121)
    }

    // -------------------------------------------------------------
    // Anatomical Spatial Anchors
    // -------------------------------------------------------------
    val bodyW = 168f * scale
    val bodyH = 205f * scale
    val bodyY = cy + 42f * scale

    val headRadius = 104f * scale
    val headY = cy - 78f * scale

    // -------------------------------------------------------------
    // 1. TAIL (Behind Body)
    // -------------------------------------------------------------
    if (species == PetSpecies.CAT || species == PetSpecies.DOG) {
        val tailStartX = cx + (if (species == PetSpecies.CAT) 52f else 46f) * scale
        val tailStartY = bodyY + 68f * scale
        val tailPath = Path().apply {
            moveTo(tailStartX, tailStartY)
            quadraticBezierTo(
                tailStartX + (62f + tailWag) * scale,
                tailStartY - 48f * scale,
                tailStartX + (78f + tailWag * 1.5f) * scale,
                tailStartY - 92f * scale
            )
        }
        drawPath(
            path = tailPath,
            color = darkFur,
            style = Stroke(width = 24f * scale, cap = StrokeCap.Round)
        )
        // White tail tip
        drawCircle(
            color = Color.White,
            radius = 14f * scale,
            center = Offset(tailStartX + (78f + tailWag * 1.5f) * scale, tailStartY - 92f * scale)
        )
    }

    // -------------------------------------------------------------
    // 2. LEGS & FEET (Planting on Floor)
    // -------------------------------------------------------------
    val legSpacing = 48f * scale
    val footW = 56f * scale
    val footH = 34f * scale
    val footY = cy + 195f * scale

    // Left Leg & Paw
    drawOval(
        brush = Brush.verticalGradient(listOf(primaryFur, darkFur), startY = bodyY + 60f * scale, endY = footY),
        topLeft = Offset(cx - legSpacing - footW * 0.5f, footY - footH * 0.5f),
        size = Size(footW, footH)
    )
    // Left Toe Claws / Pads
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx - legSpacing - 12f * scale, footY + 4f * scale))
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx - legSpacing, footY + 6f * scale))
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx - legSpacing + 12f * scale, footY + 4f * scale))

    // Right Leg & Paw
    drawOval(
        brush = Brush.verticalGradient(listOf(primaryFur, darkFur), startY = bodyY + 60f * scale, endY = footY),
        topLeft = Offset(cx + legSpacing - footW * 0.5f, footY - footH * 0.5f),
        size = Size(footW, footH)
    )
    // Right Toe Claws / Pads
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx + legSpacing - 12f * scale, footY + 4f * scale))
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx + legSpacing, footY + 6f * scale))
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx + legSpacing + 12f * scale, footY + 4f * scale))

    // -------------------------------------------------------------
    // 3. TORSO & CHUBBY BELLY
    // -------------------------------------------------------------
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(primaryFur, darkFur),
            center = Offset(cx - 24f * scale, bodyY - 20f * scale),
            radius = bodyW * 0.85f
        ),
        topLeft = Offset(cx - bodyW * 0.5f, bodyY - bodyH * 0.5f),
        size = Size(bodyW, bodyH)
    )

    // Soft Cream Chest & Belly Patch
    val bellyW = bodyW * 0.65f
    val bellyH = bodyH * 0.68f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(chestCream, chestCream.copy(alpha = 0.85f), darkFur.copy(alpha = 0.15f)),
            center = Offset(cx, bodyY + 12f * scale),
            radius = bellyW * 0.75f
        ),
        topLeft = Offset(cx - bellyW * 0.5f, bodyY - bellyH * 0.42f),
        size = Size(bellyW, bellyH)
    )

    // -------------------------------------------------------------
    // 4. ARMS & FRONT PAWS
    // -------------------------------------------------------------
    val armW = 44f * scale
    val armH = 92f * scale
    val armY = bodyY - 14f * scale

    // Left Arm (High five or resting)
    rotate(degrees = if (activeReactionZone == TouchZone.HANDS) -28f else 12f, pivot = Offset(cx - bodyW * 0.45f, armY)) {
        drawOval(
            brush = Brush.linearGradient(listOf(primaryFur, darkFur)),
            topLeft = Offset(cx - bodyW * 0.55f - armW * 0.5f, armY),
            size = Size(armW, armH)
        )
        // White Paw Tip
        drawCircle(
            color = chestCream,
            radius = armW * 0.44f,
            center = Offset(cx - bodyW * 0.55f, armY + armH - 12f * scale)
        )
    }

    // Right Arm (High five or resting)
    rotate(degrees = if (activeReactionZone == TouchZone.HANDS) 28f else -12f, pivot = Offset(cx + bodyW * 0.45f, armY)) {
        drawOval(
            brush = Brush.linearGradient(listOf(primaryFur, darkFur)),
            topLeft = Offset(cx + bodyW * 0.55f - armW * 0.5f, armY),
            size = Size(armW, armH)
        )
        // White Paw Tip
        drawCircle(
            color = chestCream,
            radius = armW * 0.44f,
            center = Offset(cx + bodyW * 0.55f, armY + armH - 12f * scale)
        )
    }

    // -------------------------------------------------------------
    // 5. 3D SCULPTED HEAD & CHUBBY CHEEKS
    // -------------------------------------------------------------
    // Ears (Behind Head)
    val earSpacing = (when (species) {
        PetSpecies.RABBIT -> 44f
        PetSpecies.DOG -> 82f
        else -> 66f
    }) * scale

    val earBaseY = headY - 68f * scale

    // Left Ear
    drawModularEar(
        species = species,
        isLeft = true,
        baseX = cx - earSpacing,
        baseY = earBaseY,
        scale = scale,
        furColor = primaryFur,
        darkFur = darkFur,
        twitch = earTwitch
    )

    // Right Ear
    drawModularEar(
        species = species,
        isLeft = false,
        baseX = cx + earSpacing,
        baseY = earBaseY,
        scale = scale,
        furColor = primaryFur,
        darkFur = darkFur,
        twitch = -earTwitch
    )

    // Main 3D Head Sphere with Volumetric Lighting
    val headHighlightX = cx - 34f * scale + (rotY * 0.8f)
    val headHighlightY = headY - 34f * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(roomKeyLight.copy(alpha = 0.55f), primaryFur, darkFur),
            center = Offset(headHighlightX, headHighlightY),
            radius = headRadius * 1.15f
        ),
        radius = headRadius,
        center = Offset(cx, headY)
    )

    // Chubby Cheeks (Talking Tom's signature round cheeks)
    val cheekRadius = 48f * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(chestCream, primaryFur),
            center = Offset(cx - 56f * scale, headY + 34f * scale),
            radius = cheekRadius * 1.1f
        ),
        radius = cheekRadius,
        center = Offset(cx - 56f * scale, headY + 34f * scale)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(chestCream, primaryFur),
            center = Offset(cx + 56f * scale, headY + 34f * scale),
            radius = cheekRadius * 1.1f
        ),
        radius = cheekRadius,
        center = Offset(cx + 56f * scale, headY + 34f * scale)
    )

    // -------------------------------------------------------------
    // 6. INTEGRATED 3D EYES (Sclera, Gaze Iris, Specular & Lids)
    // -------------------------------------------------------------
    val eyeSpacing = 42f * scale
    val eyeCenterY = headY - 14f * scale
    val eyeW = 34f * scale
    val eyeH = 46f * scale

    val isHappyPet = activeReactionZone == TouchZone.HEAD ||
            emotion == AvatarEmotion.HAPPY ||
            emotion == AvatarEmotion.LOVING
    val isTickleGiggle = activeReactionZone == TouchZone.BELLY
    val isSurprised = activeReactionZone == TouchZone.FEET ||
            emotion == AvatarEmotion.SURPRISED

    // Left Eye
    drawIntegrated3DEye(
        centerX = cx - eyeSpacing,
        centerY = eyeCenterY,
        width = eyeW,
        height = eyeH,
        scale = scale,
        irisTop = irisTop,
        irisBottom = irisBottom,
        furColor = primaryFur,
        gazeX = gazeX,
        gazeY = gazeY,
        blinkProgress = blinkProgress,
        isHappy = isHappyPet,
        isTickle = isTickleGiggle,
        isSurprised = isSurprised
    )

    // Right Eye
    drawIntegrated3DEye(
        centerX = cx + eyeSpacing,
        centerY = eyeCenterY,
        width = eyeW,
        height = eyeH,
        scale = scale,
        irisTop = irisTop,
        irisBottom = irisBottom,
        furColor = primaryFur,
        gazeX = gazeX,
        gazeY = gazeY,
        blinkProgress = blinkProgress,
        isHappy = isHappyPet,
        isTickle = isTickleGiggle,
        isSurprised = isSurprised
    )

    // Rosy Cheeks when Happy / Petted
    val totalBlush = maxOf(cheekBlushAlpha, if (isHappyPet) 0.65f else 0f)
    if (totalBlush > 0.05f) {
        val blushY = headY + 28f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF4081).copy(alpha = totalBlush * 0.60f), Color.Transparent),
                center = Offset(cx - 56f * scale, blushY),
                radius = 28f * scale
            ),
            radius = 28f * scale,
            center = Offset(cx - 56f * scale, blushY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF4081).copy(alpha = totalBlush * 0.60f), Color.Transparent),
                center = Offset(cx + 56f * scale, blushY),
                radius = 28f * scale
            ),
            radius = 28f * scale,
            center = Offset(cx + 56f * scale, blushY)
        )
    }

    // -------------------------------------------------------------
    // 7. INTEGRATED 3D SNOUT & TALKING MOUTH
    // -------------------------------------------------------------
    val snoutW = 86f * scale
    val snoutH = 58f * scale
    val snoutY = headY + 36f * scale

    // Protruding Cream Snout
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(chestCream, chestCream.copy(alpha = 0.90f), primaryFur.copy(alpha = 0.35f)),
            center = Offset(cx, snoutY - 10f * scale),
            radius = snoutW * 0.65f
        ),
        topLeft = Offset(cx - snoutW * 0.5f, snoutY - snoutH * 0.5f),
        size = Size(snoutW, snoutH)
    )

    // Cute Shiny Nose
    val noseW = 24f * scale
    val noseH = 16f * scale
    val noseY = snoutY - 14f * scale
    drawOval(
        color = noseColor,
        topLeft = Offset(cx - noseW * 0.5f, noseY - noseH * 0.5f),
        size = Size(noseW, noseH)
    )
    // Nose highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = 3.5f * scale,
        center = Offset(cx - 4f * scale, noseY - 3f * scale)
    )

    // Synchronized Integrated Mouth
    val mouthY = snoutY + 8f * scale
    val openProgress = when {
        isTickleGiggle -> 0.90f
        isTalking -> (0.25f + mouthCycle * 0.75f)
        isHappyPet -> 0.35f
        isSurprised -> 0.70f
        else -> 0.10f
    }

    drawIntegratedTalkingMouth(
        cx = cx,
        mouthY = mouthY,
        scale = scale,
        openProgress = openProgress,
        isTalking = isTalking,
        isLaughing = isTickleGiggle,
        isHappy = isHappyPet
    )

    // Whiskers (for Cat)
    if (species == PetSpecies.CAT) {
        val whiskerStroke = Stroke(width = 2.2f * scale, cap = StrokeCap.Round)
        val whiskerColor = Color(0x90263238)

        // Left whiskers
        drawLine(whiskerColor, Offset(cx - 36f * scale, snoutY), Offset(cx - 84f * scale, snoutY - 8f * scale), strokeWidth = whiskerStroke.width)
        drawLine(whiskerColor, Offset(cx - 36f * scale, snoutY + 8f * scale), Offset(cx - 88f * scale, snoutY + 12f * scale), strokeWidth = whiskerStroke.width)

        // Right whiskers
        drawLine(whiskerColor, Offset(cx + 36f * scale, snoutY), Offset(cx + 84f * scale, snoutY - 8f * scale), strokeWidth = whiskerStroke.width)
        drawLine(whiskerColor, Offset(cx + 36f * scale, snoutY + 8f * scale), Offset(cx + 88f * scale, snoutY + 12f * scale), strokeWidth = whiskerStroke.width)
    }

    // Capybara Yuzu Citrus on Head (Jamal signature)
    if (species == PetSpecies.CAPYBARA) {
        val yuzuRadius = 26f * scale
        val yuzuY = headY - headRadius - 10f * scale
        // Orange body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD54F), Color(0xFFFF9800)),
                center = Offset(cx - 6f * scale, yuzuY - 6f * scale),
                radius = yuzuRadius
            ),
            radius = yuzuRadius,
            center = Offset(cx, yuzuY)
        )
        // Little green leaf
        drawOval(
            color = Color(0xFF4CAF50),
            topLeft = Offset(cx - 4f * scale, yuzuY - yuzuRadius - 8f * scale),
            size = Size(18f * scale, 10f * scale)
        )
    }
}

/**
 * Draws a 100% integrated 3D eye with smooth eyelids, specular shine, and gaze tracking.
 */
private fun DrawScope.drawIntegrated3DEye(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    scale: Float,
    irisTop: Color,
    irisBottom: Color,
    furColor: Color,
    gazeX: Float,
    gazeY: Float,
    blinkProgress: Float,
    isHappy: Boolean,
    isTickle: Boolean,
    isSurprised: Boolean
) {
    when {
        // Happy squint crescents (⌒) when petted on head
        isHappy -> {
            val strokeW = 4.8f * scale
            val arcW = width * 1.35f
            val arcH = height * 0.75f
            val happyPath = Path().apply {
                moveTo(centerX - arcW * 0.5f, centerY + arcH * 0.25f)
                cubicTo(
                    centerX - arcW * 0.25f, centerY - arcH * 0.65f,
                    centerX + arcW * 0.25f, centerY - arcH * 0.65f,
                    centerX + arcW * 0.5f, centerY + arcH * 0.25f
                )
            }
            drawPath(happyPath, Color(0xFF263238), style = Stroke(strokeW, cap = StrokeCap.Round))
        }

        // Giggling squints (> <) when tickled on belly
        isTickle -> {
            val strokeW = 4.8f * scale
            val armL = width * 0.75f
            drawLine(
                color = Color(0xFF263238),
                start = Offset(centerX - armL * 0.6f, centerY - armL * 0.5f),
                end = Offset(centerX + armL * 0.3f, centerY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF263238),
                start = Offset(centerX + armL * 0.3f, centerY),
                end = Offset(centerX - armL * 0.6f, centerY + armL * 0.5f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // Normal Eye / Blinking / Gaze Tracking
        else -> {
            // White Sclera
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFECEFF1)),
                    center = Offset(centerX, centerY - 6f * scale),
                    radius = width
                ),
                topLeft = Offset(centerX - width * 0.5f, centerY - height * 0.5f),
                size = Size(width, height)
            )

            // Gaze calculation
            val maxShiftX = width * 0.26f
            val maxShiftY = height * 0.24f
            val px = centerX + (gazeX * maxShiftX)
            val py = centerY + (gazeY * maxShiftY)

            // Colorful 3D Iris
            val irisRadius = width * (if (isSurprised) 0.34f else 0.44f)
            drawCircle(
                brush = Brush.verticalGradient(listOf(irisTop, irisBottom)),
                radius = irisRadius,
                center = Offset(px, py)
            )

            // Dark Pupil
            val pupilRadius = irisRadius * 0.62f
            drawCircle(
                color = Color(0xFF1A1A1A),
                radius = pupilRadius,
                center = Offset(px, py)
            )

            // Big Glossy Specular Highlight (Talking Tom 3D glassy look)
            drawCircle(
                color = Color.White,
                radius = pupilRadius * 0.48f,
                center = Offset(px - pupilRadius * 0.34f, py - pupilRadius * 0.34f)
            )
            // Secondary small specular sparkle
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = pupilRadius * 0.22f,
                center = Offset(px + pupilRadius * 0.36f, py + pupilRadius * 0.36f)
            )

            // Dark Eyelash Outline
            drawOval(
                color = Color(0x60263238),
                topLeft = Offset(centerX - width * 0.5f, centerY - height * 0.5f),
                size = Size(width, height),
                style = Stroke(width = 2.4f * scale)
            )

            // Natural Smooth Eyelids (Blinking)
            if (blinkProgress > 0.05f) {
                val lidH = height * blinkProgress * 1.1f
                drawOval(
                    color = furColor,
                    topLeft = Offset(centerX - width * 0.55f, centerY - height * 0.55f),
                    size = Size(width * 1.1f, lidH)
                )
                // Eyelash line on lid
                drawLine(
                    color = Color(0xFF263238),
                    start = Offset(centerX - width * 0.45f, centerY - height * 0.55f + lidH),
                    end = Offset(centerX + width * 0.45f, centerY - height * 0.55f + lidH),
                    strokeWidth = 3f * scale,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Integrated talking mouth cavity with jaw flap, cartoon white teeth, and bouncing pink tongue.
 */
private fun DrawScope.drawIntegratedTalkingMouth(
    cx: Float,
    mouthY: Float,
    scale: Float,
    openProgress: Float,
    isTalking: Boolean,
    isLaughing: Boolean,
    isHappy: Boolean
) {
    val mW = (38f + openProgress * 14f) * scale
    val mH = (10f + openProgress * 28f) * scale

    if (openProgress > 0.18f) {
        // Deep Dark Mouth Cavity
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF240707), Color(0xFF150202)),
                center = Offset(cx, mouthY + mH * 0.4f),
                radius = mW * 0.6f
            ),
            topLeft = Offset(cx - mW * 0.5f, mouthY),
            size = Size(mW, mH)
        )

        // Top White Cartoon Teeth
        val teethH = mH * 0.35f
        val teethPath = Path().apply {
            moveTo(cx - mW * 0.32f, mouthY)
            lineTo(cx + mW * 0.32f, mouthY)
            lineTo(cx + mW * 0.24f, mouthY + teethH)
            lineTo(cx - mW * 0.24f, mouthY + teethH)
            close()
        }
        drawPath(teethPath, color = Color.White)

        // Bouncing Pink Tongue
        val tongueW = mW * 0.62f
        val tongueH = mH * 0.65f
        val tongueY = mouthY + mH * 0.40f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF6B81), Color(0xFFE04860)),
                center = Offset(cx, tongueY + tongueH * 0.5f),
                radius = tongueW * 0.6f
            ),
            topLeft = Offset(cx - tongueW * 0.5f, tongueY),
            size = Size(tongueW, tongueH)
        )
        // Tongue center line
        drawLine(
            color = Color(0xFFD83A56),
            start = Offset(cx, tongueY + tongueH * 0.25f),
            end = Offset(cx, tongueY + tongueH * 0.85f),
            strokeWidth = 2.2f * scale,
            cap = StrokeCap.Round
        )

        // Outer Lip Outline
        drawOval(
            color = Color(0x50263238),
            topLeft = Offset(cx - mW * 0.5f, mouthY),
            size = Size(mW, mH),
            style = Stroke(width = 2.4f * scale)
        )
    } else {
        // Gentle Closed Smile
        val smilePath = Path().apply {
            moveTo(cx - 18f * scale, mouthY + 2f * scale)
            quadraticBezierTo(cx, mouthY + 12f * scale, cx + 18f * scale, mouthY + 2f * scale)
        }
        drawPath(
            path = smilePath,
            color = Color(0xFF263238),
            style = Stroke(width = 3.2f * scale, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws species-appropriate ears (pointy cat, long floppy rabbit, cute dog, round bear/capybara).
 */
private fun DrawScope.drawModularEar(
    species: PetSpecies,
    isLeft: Boolean,
    baseX: Float,
    baseY: Float,
    scale: Float,
    furColor: Color,
    darkFur: Color,
    twitch: Float
) {
    val dir = if (isLeft) -1f else 1f
    rotate(degrees = (if (isLeft) -14f else 14f) + twitch, pivot = Offset(baseX, baseY)) {
        when (species) {
            // Pointy Cat Ears
            PetSpecies.CAT -> {
                val earW = 44f * scale
                val earH = 64f * scale
                val earPath = Path().apply {
                    moveTo(baseX - earW * 0.5f, baseY)
                    lineTo(baseX + dir * 8f * scale, baseY - earH)
                    lineTo(baseX + earW * 0.5f, baseY)
                    close()
                }
                drawPath(earPath, furColor)
                // Pink inner ear
                val innerEarPath = Path().apply {
                    moveTo(baseX - earW * 0.28f, baseY - 4f * scale)
                    lineTo(baseX + dir * 6f * scale, baseY - earH * 0.78f)
                    lineTo(baseX + earW * 0.28f, baseY - 4f * scale)
                    close()
                }
                drawPath(innerEarPath, Color(0xFFFF8A80).copy(alpha = 0.75f))
            }

            // Long Cute Bunny Ears (Siti / Angela style)
            PetSpecies.RABBIT -> {
                val earW = 34f * scale
                val earH = 110f * scale
                drawOval(
                    brush = Brush.linearGradient(listOf(Color.White, Color(0xFFE0E0E0))),
                    topLeft = Offset(baseX - earW * 0.5f, baseY - earH),
                    size = Size(earW, earH)
                )
                // Pink inner ear
                drawOval(
                    color = Color(0xFFFF80AB).copy(alpha = 0.65f),
                    topLeft = Offset(baseX - earW * 0.28f, baseY - earH * 0.88f),
                    size = Size(earW * 0.56f, earH * 0.76f)
                )
            }

            // Floppy Dog Ears (Rizky style)
            PetSpecies.DOG -> {
                val earW = 46f * scale
                val earH = 82f * scale
                drawOval(
                    brush = Brush.verticalGradient(listOf(darkFur, furColor)),
                    topLeft = Offset(baseX - earW * 0.5f, baseY - 12f * scale),
                    size = Size(earW, earH)
                )
            }

            // Round Bear / Capybara Ears
            PetSpecies.BEAR, PetSpecies.CAPYBARA -> {
                val earRadius = 28f * scale
                drawCircle(
                    brush = Brush.radialGradient(listOf(furColor, darkFur)),
                    radius = earRadius,
                    center = Offset(baseX, baseY - earRadius * 0.6f)
                )
                // Inner ear
                drawCircle(
                    color = Color(0x60000000),
                    radius = earRadius * 0.55f,
                    center = Offset(baseX, baseY - earRadius * 0.6f)
                )
            }
        }
    }
}
