package ir.hadipoor.eviltower.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ir.hadipoor.eviltower.audio.GameAudio
import ir.hadipoor.eviltower.data.ControlScheme
import ir.hadipoor.eviltower.data.GameSettings
import ir.hadipoor.eviltower.data.PlayerProfile
import ir.hadipoor.eviltower.game.engine.GameEngine
import ir.hadipoor.eviltower.game.engine.InputState
import ir.hadipoor.eviltower.game.model.GameEvent
import ir.hadipoor.eviltower.game.model.PowerUp
import ir.hadipoor.eviltower.game.model.RunPhase
import ir.hadipoor.eviltower.game.render.GameRenderer
import ir.hadipoor.eviltower.game.render.RenderStyles
import ir.hadipoor.eviltower.game.render.SvgPaths
import ir.hadipoor.eviltower.game.render.drawSvg
import ir.hadipoor.eviltower.ui.LocalStrings
import ir.hadipoor.eviltower.ui.components.CoinIcon
import ir.hadipoor.eviltower.ui.components.HeartIcon
import ir.hadipoor.eviltower.ui.theme.TowerPalette
import ir.hadipoor.eviltower.util.PersianNumbers
import kotlin.math.abs

/**
 * صفحه بازی — the 60fps game surface, HUD, control overlays and the pause / game-over dialogs.
 *
 * The loop runs on [withFrameNanos]: one engine tick + one Canvas invalidation per frame,
 * with no allocations in the hot path.
 */
@Composable
fun GameScreen(
    engine: GameEngine,
    profile: PlayerProfile,
    settings: GameSettings,
    audio: GameAudio,
    tiltX: State<Float>,
    onExitToMenu: () -> Unit,
    onRestart: () -> Unit,
    onFinished: (Boolean) -> Unit,
    overlay: @Composable (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val persianDigits = settings.language == "fa"
    val renderer = remember { GameRenderer() }
    val towerStyle = remember(profile.selectedTheme) { RenderStyles.tower(profile.selectedTheme) }
    val heroStyle = remember(profile.selectedSkin) { RenderStyles.hero(profile.selectedSkin) }

    // all per-run state is keyed on the engine so "restart" starts perfectly clean
    var frame by remember { mutableIntStateOf(0) }
    val clock = remember(engine) { FrameClock() }
    var showPause by remember(engine) { mutableStateOf(false) }
    var bannerText by remember(engine) { mutableStateOf<String?>(null) }

    // --- input state shared between the gesture handlers and the loop -----------------------
    val input = remember(engine) { MutableInput() }
    val currentTilt by rememberUpdatedState(tiltX.value)

    // pause the run when the app goes to the background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    engine.pause()
                    showPause = true
                    audio.pauseMusic()
                }

                Lifecycle.Event.ON_RESUME -> audio.resumeMusic()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- the game loop -----------------------------------------------------------------------
    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 1f / 60f else ((now - last) / 1_000_000_000f)
                last = now
                clock.time += dt

                if (!showPause) {
                    val moveX = when (settings.controlScheme) {
                        ControlScheme.TILT -> currentTilt
                        else -> input.moveX
                    }
                    engine.update(
                        dt,
                        InputState(
                            moveX = moveX,
                            jumpPressed = input.consumeJump(),
                            attackPressed = input.consumeAttack(),
                        ),
                    )
                    engine.consumeEvents().forEach { event ->
                        audio.handle(event)
                        when (event) {
                            is GameEvent.FloorCleared -> {
                                val floor = engine.visibleFloors().firstOrNull { it.number == event.floor }
                                if (floor?.gateLocked == true) {
                                    bannerText = strings.bossWarning
                                    clock.bannerTimer = 2.2f
                                }
                            }

                            GameEvent.TrapTrigger -> {
                                if (engine.player.controlsReversed > 0f) {
                                    bannerText = strings.controlsReversed
                                    clock.bannerTimer = 1.8f
                                }
                            }

                            else -> Unit
                        }
                    }
                    if (clock.bannerTimer > 0f) {
                        clock.bannerTimer -= dt
                        if (clock.bannerTimer <= 0f) bannerText = null
                    }
                }
                frame++
            }
            if (engine.phase == RunPhase.PLAYING) clock.finishReported = false
            if (!clock.finishReported &&
                (engine.phase == RunPhase.GAME_OVER || engine.phase == RunPhase.VICTORY)
            ) {
                clock.finishReported = true
                onFinished(engine.phase == RunPhase.VICTORY)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(TowerPalette.Shadow)) {
        // ---------------------------------------------------------------- game surface
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(settings.controlScheme) {
                    if (settings.controlScheme == ControlScheme.SWIPE) {
                        detectDragGestures(
                            onDragStart = { input.dragTotalY = 0f; input.dragTotalX = 0f },
                            onDragEnd = { input.moveX = 0f; input.dragTotalY = 0f },
                            onDragCancel = { input.moveX = 0f },
                        ) { change, drag ->
                            change.consume()
                            input.dragTotalX += drag.x
                            input.dragTotalY += drag.y
                            // swipe up = jump (fires once per gesture)
                            if (input.dragTotalY < -38f) {
                                input.jump = true
                                input.dragTotalY = 0f
                            }
                            input.moveX = when {
                                input.dragTotalX > 12f -> 1f
                                input.dragTotalX < -12f -> -1f
                                else -> 0f
                            }
                            input.dragTotalX = input.dragTotalX.coerceIn(-60f, 60f)
                        }
                    }
                }
                .pointerInput(settings.controlScheme) {
                    // no double-tap handler: taps must fire instantly (attack latency matters)
                    detectTapGestures(onTap = { input.attack = true })
                },
        ) {
            @Suppress("UNUSED_EXPRESSION")
            frame // read the frame counter so Compose redraws every frame
            with(renderer) {
                render(engine, clock.time, towerStyle, heroStyle, settings.screenShake)
            }
        }

        // ---------------------------------------------------------------- HUD
        Hud(
            engine = engine,
            persianDigits = persianDigits,
            onPause = {
                engine.pause()
                showPause = true
            },
        )

        // banner (boss warning / sleeping gas)
        AnimatedVisibility(
            visible = bannerText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = bannerText ?: "",
                style = MaterialTheme.typography.headlineMedium,
                color = TowerPalette.Ember,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(TowerPalette.Shadow.copy(alpha = 0.75f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

        // ---------------------------------------------------------------- controls overlay
        if (settings.controlScheme != ControlScheme.SWIPE) {
            TouchControls(
                showDpad = settings.controlScheme == ControlScheme.BUTTONS,
                input = input,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ---------------------------------------------------------------- pause / game over
        if (showPause && engine.phase == RunPhase.PAUSED) {
            PauseOverlay(
                onResume = {
                    showPause = false
                    engine.resume()
                },
                onRestart = onRestart,
                onExit = onExitToMenu,
            )
        }

        overlay?.invoke()
    }
}

/** Plain (non-Compose-state) per-frame values so the loop never triggers recomposition. */
class FrameClock {
    var time: Float = 0f
    var bannerTimer: Float = 0f
    var finishReported: Boolean = false
}

/** Mutable, allocation-free input buffer shared by gesture handlers and the loop. */
class MutableInput {
    var moveX: Float = 0f
    var jump: Boolean = false
    var attack: Boolean = false
    var dragTotalX: Float = 0f
    var dragTotalY: Float = 0f

    fun consumeJump(): Boolean {
        val v = jump
        jump = false
        return v
    }

    fun consumeAttack(): Boolean {
        val v = attack
        attack = false
        return v
    }
}

@Composable
private fun Hud(engine: GameEngine, persianDigits: Boolean, onPause: () -> Unit) {
    val strings = LocalStrings.current
    val num = { value: Int -> PersianNumbers.format(value, persianDigits) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(engine.player.maxHealth) { index ->
                    HeartIcon(
                        filled = index < engine.player.health,
                        modifier = Modifier.size(22.dp).padding(end = 3.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CoinIcon(Modifier.size(18.dp))
                Text(
                    text = num(engine.coins),
                    style = MaterialTheme.typography.labelLarge,
                    color = TowerPalette.Gold,
                )
                Spacer(Modifier.size(6.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(TowerPalette.Purple.copy(alpha = 0.8f))
                        .clickable { onPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Pause,
                        contentDescription = strings.pause,
                        tint = TowerPalette.EmberSoft,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${strings.floor} ${num(engine.currentFloor)}",
                style = MaterialTheme.typography.titleLarge,
                color = TowerPalette.TextPrimary,
            )
            Text(
                text = "${strings.score} ${num(engine.score)}",
                style = MaterialTheme.typography.labelLarge,
                color = TowerPalette.TextMuted,
            )
        }
        // active power-ups
        val powers = engine.player.powerUps.keys.toList()
        if (powers.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                powers.forEach { power ->
                    val path = when (power) {
                        PowerUp.SHIELD -> SvgPaths.SHIELD_BODY
                        PowerUp.WINGS -> SvgPaths.WINGS_BODY
                        PowerUp.SPEED -> SvgPaths.SPEED_BOLT
                        PowerUp.MAGNET -> SvgPaths.MAGNET_BODY
                    }
                    val color = when (power) {
                        PowerUp.SHIELD -> Color(0xFF4F7FD6)
                        PowerUp.WINGS -> TowerPalette.Bone
                        PowerUp.SPEED -> Color(0xFFFFE45C)
                        PowerUp.MAGNET -> Color(0xFFD6444F)
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TowerPalette.Shadow.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(18.dp)) {
                            drawSvg(path, 0f, 0f, size.width, size.height, color)
                        }
                    }
                }
            }
        }
    }
}

/** On-screen D-pad + action buttons (کنترل با دکمه) — also used by the tilt scheme. */
@Composable
private fun TouchControls(showDpad: Boolean, input: MutableInput, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (showDpad) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HoldButton(label = "◀", onPress = { input.moveX = -1f }, onRelease = { input.moveX = 0f })
                HoldButton(label = "▶", onPress = { input.moveX = 1f }, onRelease = { input.moveX = 0f })
            }
        } else {
            Spacer(Modifier.size(1.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(label = "⚔") { input.attack = true }
            ActionButton(label = "▲", primary = true) { input.jump = true }
        }
    }
}

@Composable
private fun HoldButton(label: String, onPress: () -> Unit, onRelease: () -> Unit) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .background(TowerPalette.Purple.copy(alpha = 0.55f))
            .border(1.dp, TowerPalette.PurpleLight.copy(alpha = 0.5f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onPress()
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        pressed = event.changes.any { it.pressed }
                    }
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium, color = TowerPalette.TextPrimary)
    }
}

@Composable
private fun ActionButton(label: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (primary) 74.dp else 62.dp)
            .clip(CircleShape)
            .background(
                if (primary) TowerPalette.Ember.copy(alpha = 0.75f)
                else TowerPalette.Purple.copy(alpha = 0.55f)
            )
            .border(
                1.dp,
                if (primary) TowerPalette.Torch else TowerPalette.PurpleLight.copy(alpha = 0.5f),
                CircleShape,
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.headlineMedium,
            color = if (primary) TowerPalette.Shadow else TowerPalette.TextPrimary,
        )
    }
}

/** توقف */
@Composable
private fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TowerPalette.Shadow.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        ir.hadipoor.eviltower.ui.components.StonePanel(
            Modifier
                .fillMaxWidth(0.82f)
                .padding(20.dp)
        ) {
            Text(
                text = strings.paused,
                style = MaterialTheme.typography.headlineLarge,
                color = TowerPalette.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            ir.hadipoor.eviltower.ui.components.TowerButton(strings.resume, primary = true, onClick = onResume)
            Spacer(Modifier.height(10.dp))
            ir.hadipoor.eviltower.ui.components.TowerButton(strings.restart, onClick = onRestart)
            Spacer(Modifier.height(10.dp))
            ir.hadipoor.eviltower.ui.components.TowerButton(strings.exitToMenu, onClick = onExit)
        }
    }
}
