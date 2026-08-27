package ir.hadipoor.eviltower.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.hadipoor.eviltower.data.ControlScheme
import ir.hadipoor.eviltower.data.GameSettings
import ir.hadipoor.eviltower.ui.LocalStrings
import ir.hadipoor.eviltower.ui.components.ScreenScaffold
import ir.hadipoor.eviltower.ui.components.StonePanel
import ir.hadipoor.eviltower.ui.components.TowerButton
import ir.hadipoor.eviltower.ui.theme.TowerPalette

/** تنظیمات */
@Composable
fun SettingsScreen(
    settings: GameSettings,
    onBack: () -> Unit,
    onMusic: (Float) -> Unit,
    onSfx: (Float) -> Unit,
    onControls: (ControlScheme) -> Unit,
    onVibration: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onScreenShake: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    val strings = LocalStrings.current
    var confirmReset by remember { mutableStateOf(false) }

    ScreenScaffold(title = strings.settings, onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            StonePanel(Modifier.fillMaxWidth()) {
                SettingLabel(strings.musicVolume, "${(settings.musicVolume * 100).toInt()}%")
                Slider(
                    value = settings.musicVolume,
                    onValueChange = onMusic,
                    colors = SliderDefaults.colors(
                        thumbColor = TowerPalette.Ember,
                        activeTrackColor = TowerPalette.Ember,
                        inactiveTrackColor = TowerPalette.Purple,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                SettingLabel(strings.soundVolume, "${(settings.sfxVolume * 100).toInt()}%")
                Slider(
                    value = settings.sfxVolume,
                    onValueChange = onSfx,
                    colors = SliderDefaults.colors(
                        thumbColor = TowerPalette.Ember,
                        activeTrackColor = TowerPalette.Ember,
                        inactiveTrackColor = TowerPalette.Purple,
                    ),
                )
            }

            Spacer(Modifier.height(14.dp))
            StonePanel(Modifier.fillMaxWidth()) {
                Text(
                    text = strings.controlScheme,
                    style = MaterialTheme.typography.titleMedium,
                    color = TowerPalette.TextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                val options = listOf(
                    ControlScheme.SWIPE to strings.controlSwipe,
                    ControlScheme.BUTTONS to strings.controlButtons,
                    ControlScheme.TILT to strings.controlTilt,
                )
                options.forEach { (scheme, label) ->
                    ChoiceRow(
                        label = label,
                        selected = settings.controlScheme == scheme,
                        onClick = { onControls(scheme) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            StonePanel(Modifier.fillMaxWidth()) {
                ToggleRow(strings.vibration, settings.vibration, onVibration)
                ToggleRow("لرزش صفحه هنگام ضربه", settings.screenShake, onScreenShake)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.language,
                    style = MaterialTheme.typography.titleMedium,
                    color = TowerPalette.TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                ChoiceRow(strings.persian, settings.language == "fa") { onLanguage("fa") }
                ChoiceRow(strings.english, settings.language == "en") { onLanguage("en") }
            }

            Spacer(Modifier.height(14.dp))
            StonePanel(Modifier.fillMaxWidth()) {
                Text(
                    text = strings.about,
                    style = MaterialTheme.typography.titleMedium,
                    color = TowerPalette.TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.aboutText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TowerPalette.TextMuted,
                )
            }

            Spacer(Modifier.height(18.dp))
            TowerButton(strings.resetProgress) { confirmReset = true }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = TowerPalette.DeepPurple,
            titleContentColor = TowerPalette.TextPrimary,
            textContentColor = TowerPalette.TextMuted,
            title = { Text(strings.resetProgress) },
            text = { Text(strings.resetProgressConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) { Text(strings.confirm, color = TowerPalette.Blood) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(strings.cancel, color = TowerPalette.TextMuted)
                }
            },
        )
    }
}

@Composable
private fun SettingLabel(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TowerPalette.TextPrimary)
        Text(value, style = MaterialTheme.typography.labelLarge, color = TowerPalette.EmberSoft)
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TowerPalette.TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TowerPalette.Ember,
                checkedTrackColor = TowerPalette.Purple,
                uncheckedThumbColor = TowerPalette.StoneDark,
                uncheckedTrackColor = TowerPalette.Shadow,
            ),
        )
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onClick,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = TowerPalette.Ember,
                unselectedColor = TowerPalette.StoneDark,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) TowerPalette.TextPrimary else TowerPalette.TextMuted,
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() },
        )
    }
}
