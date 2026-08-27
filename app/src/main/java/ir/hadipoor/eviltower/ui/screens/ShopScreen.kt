package ir.hadipoor.eviltower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ir.hadipoor.eviltower.data.Currency
import ir.hadipoor.eviltower.data.PlayerProfile
import ir.hadipoor.eviltower.data.ShopCatalog
import ir.hadipoor.eviltower.data.ShopEntry
import ir.hadipoor.eviltower.data.SkinEntry
import ir.hadipoor.eviltower.data.ThemeEntry
import ir.hadipoor.eviltower.data.UpgradeEntry
import ir.hadipoor.eviltower.game.render.RenderStyles
import ir.hadipoor.eviltower.game.render.SvgPaths
import ir.hadipoor.eviltower.game.render.drawSvg
import ir.hadipoor.eviltower.ui.LocalStrings
import ir.hadipoor.eviltower.ui.components.CoinIcon
import ir.hadipoor.eviltower.ui.components.CurrencyChip
import ir.hadipoor.eviltower.ui.components.GemIcon
import ir.hadipoor.eviltower.ui.components.ScreenScaffold
import ir.hadipoor.eviltower.ui.theme.TowerPalette
import ir.hadipoor.eviltower.util.PersianNumbers

private enum class ShopTab { SKINS, THEMES, UPGRADES }

/** فروشگاه — hero skins, tower themes and permanent upgrades. */
@Composable
fun ShopScreen(
    profile: PlayerProfile,
    persianDigits: Boolean,
    onBack: () -> Unit,
    onBuy: (ShopEntry) -> Unit,
    onEquipSkin: (String) -> Unit,
    onEquipTheme: (String) -> Unit,
    onBuyGems: () -> Unit,
) {
    val strings = LocalStrings.current
    var tab by remember { mutableStateOf(ShopTab.SKINS) }
    val num = { value: Int -> PersianNumbers.format(value, persianDigits) }

    ScreenScaffold(
        title = strings.shop,
        onBack = onBack,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CurrencyChip(num(profile.coins))
                CurrencyChip(num(profile.gems), gem = true)
            }
        },
    ) { modifier ->
        Column(modifier.padding(horizontal = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabChip(strings.heroSkins, tab == ShopTab.SKINS, Modifier.weight(1f)) { tab = ShopTab.SKINS }
                TabChip(strings.towerThemes, tab == ShopTab.THEMES, Modifier.weight(1f)) { tab = ShopTab.THEMES }
                TabChip(strings.upgrades, tab == ShopTab.UPGRADES, Modifier.weight(1f)) { tab = ShopTab.UPGRADES }
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (tab) {
                    ShopTab.SKINS -> items(ShopCatalog.skins) { skin ->
                        val owned = skin.id in profile.unlockedSkins
                        ShopRow(
                            title = skin.persianName,
                            description = skin.persianDescription,
                            price = skin.price,
                            currency = skin.currency,
                            owned = owned,
                            equipped = profile.selectedSkin == skin.id,
                            persianDigits = persianDigits,
                            preview = { SkinPreview(skin) },
                            onAction = { if (owned) onEquipSkin(skin.id) else onBuy(skin) },
                        )
                    }

                    ShopTab.THEMES -> items(ShopCatalog.themes) { theme ->
                        val owned = theme.id in profile.unlockedThemes
                        ShopRow(
                            title = theme.persianName,
                            description = theme.persianDescription,
                            price = theme.price,
                            currency = theme.currency,
                            owned = owned,
                            equipped = profile.selectedTheme == theme.id,
                            persianDigits = persianDigits,
                            preview = { ThemePreview(theme) },
                            onAction = { if (owned) onEquipTheme(theme.id) else onBuy(theme) },
                        )
                    }

                    ShopTab.UPGRADES -> {
                        items(ShopCatalog.upgrades) { upgrade ->
                            val level = profile.upgradeLevel(upgrade.id)
                            val maxed = level >= upgrade.maxLevel
                            ShopRow(
                                title = "${upgrade.persianName} • ${strings.level} ${PersianNumbers.format(level, persianDigits)}/${PersianNumbers.format(upgrade.maxLevel, persianDigits)}",
                                description = upgrade.persianDescription,
                                price = upgrade.priceFor(level),
                                currency = upgrade.currency,
                                owned = maxed,
                                equipped = maxed,
                                persianDigits = persianDigits,
                                preview = { UpgradePreview(upgrade, level) },
                                onAction = { if (!maxed) onBuy(upgrade) },
                            )
                        }
                        item {
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TowerPalette.Purple.copy(alpha = 0.7f))
                                    .border(1.dp, TowerPalette.Gem.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .clickable { onBuyGems() }
                                    .padding(14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GemIcon(Modifier.size(24.dp))
                                    Spacer(Modifier.size(10.dp))
                                    Column {
                                        Text(
                                            strings.buyGems,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TowerPalette.TextPrimary,
                                        )
                                        Text(
                                            "پرداخت از طریق کافه بازار",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TowerPalette.TextMuted,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) TowerPalette.Ember else TowerPalette.Purple.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TowerPalette.Shadow else TowerPalette.TextMuted,
        )
    }
}

@Composable
private fun ShopRow(
    title: String,
    description: String,
    price: Int,
    currency: Currency,
    owned: Boolean,
    equipped: Boolean,
    persianDigits: Boolean,
    preview: @Composable () -> Unit,
    onAction: () -> Unit,
) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TowerPalette.DeepPurple.copy(alpha = 0.9f))
            .border(
                1.dp,
                if (equipped) TowerPalette.Ember.copy(alpha = 0.8f) else TowerPalette.PurpleLight.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp),
            )
            .clickable { onAction() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TowerPalette.Shadow.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) { preview() }

        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TowerPalette.TextPrimary)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = TowerPalette.TextMuted,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                equipped -> Text(
                    text = strings.equipped,
                    style = MaterialTheme.typography.labelMedium,
                    color = TowerPalette.Ember,
                )

                owned -> Text(
                    text = strings.equip,
                    style = MaterialTheme.typography.labelMedium,
                    color = TowerPalette.EmberSoft,
                )

                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currency == Currency.GEM) GemIcon(Modifier.size(16.dp)) else CoinIcon(Modifier.size(16.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(
                        text = PersianNumbers.format(price, persianDigits),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (currency == Currency.GEM) TowerPalette.Gem else TowerPalette.Gold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkinPreview(skin: SkinEntry) {
    val style = RenderStyles.hero(skin.id)
    androidx.compose.foundation.Canvas(Modifier.size(44.dp)) {
        val w = size.width
        val h = size.height
        drawSvg(
            SvgPaths.HERO_CAPE, w * 0.1f, h * 0.2f, w * 0.8f, h * 0.7f, style.cape,
        )
        drawSvg(
            SvgPaths.HERO_TORSO, w * 0.15f, h * 0.25f, w * 0.7f, h * 0.6f, style.armor,
        )
        drawSvg(
            SvgPaths.HERO_HEAD, w * 0.22f, h * 0.05f, w * 0.56f, h * 0.4f, style.armor,
        )
        drawSvg(
            SvgPaths.HERO_VISOR, w * 0.22f, h * 0.05f, w * 0.56f, h * 0.4f, style.visor,
        )
        drawSvg(
            SvgPaths.HERO_HELMET_CREST, w * 0.22f, h * 0.02f, w * 0.56f, h * 0.4f, style.trim,
        )
    }
}

@Composable
private fun ThemePreview(theme: ThemeEntry) {
    val style = RenderStyles.tower(theme.id)
    androidx.compose.foundation.Canvas(Modifier.size(44.dp)) {
        drawRect(style.sky)
        drawRect(style.wall, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.25f), size = size.copy(height = size.height * 0.5f))
        drawRect(style.platform, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.62f), size = size.copy(height = size.height * 0.14f))
        drawCircle(style.accent, radius = size.width * 0.1f, center = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.4f))
    }
}

@Composable
private fun UpgradePreview(upgrade: UpgradeEntry, level: Int) {
    val path = when (upgrade.id) {
        "extra_heart" -> SvgPaths.HEART_BODY
        "start_shield" -> SvgPaths.SHIELD_BODY
        "coin_insurance" -> SvgPaths.COIN_BODY
        "coin_bonus" -> SvgPaths.GEM_BODY
        else -> SvgPaths.WINGS_BODY
    }
    val color = when (upgrade.id) {
        "extra_heart" -> TowerPalette.Blood
        "start_shield" -> Color(0xFF4F7FD6)
        "coin_insurance" -> TowerPalette.Gold
        "coin_bonus" -> TowerPalette.Gem
        else -> TowerPalette.Bone
    }
    androidx.compose.foundation.Canvas(Modifier.size(34.dp)) {
        drawSvg(path, 0f, 0f, size.width, size.height, color)
    }
}
