package ir.hadipoor.eviltower.data

import ir.hadipoor.eviltower.game.render.RenderStyles

/** Currency a shop entry is priced in. */
enum class Currency { COIN, GEM }

sealed interface ShopEntry {
    val id: String
    val persianName: String
    val englishName: String
    val persianDescription: String
    val price: Int
    val currency: Currency
}

data class SkinEntry(
    override val id: String,
    override val persianName: String,
    override val englishName: String,
    override val persianDescription: String,
    override val price: Int,
    override val currency: Currency,
) : ShopEntry

data class ThemeEntry(
    override val id: String,
    override val persianName: String,
    override val englishName: String,
    override val persianDescription: String,
    override val price: Int,
    override val currency: Currency,
) : ShopEntry

data class UpgradeEntry(
    override val id: String,
    override val persianName: String,
    override val englishName: String,
    override val persianDescription: String,
    val maxLevel: Int,
    /** Price of each level, index 0 = first level. */
    val prices: List<Int>,
    override val currency: Currency,
) : ShopEntry {
    override val price: Int get() = prices.first()
    fun priceFor(level: Int): Int = prices.getOrElse(level) { prices.last() }
}

/**
 * The meta-progression catalogue (فروشگاه). Everything here is bought with currency earned by
 * playing; nothing is gated behind a real-money purchase.
 */
object ShopCatalog {

    val skins: List<SkinEntry> = listOf(
        SkinEntry("knight", "شوالیه گرفتار", "Trapped Knight", "قهرمان اصلی داستان، آماده صعود.", 0, Currency.COIN),
        SkinEntry("shadow", "راهب سایه", "Shadow Monk", "از تاریکی زاده شده؛ در طبقات تاریک دیده نمی‌شود.", 1500, Currency.COIN),
        SkinEntry("ember", "جنگاور آتش", "Ember Warrior", "زره‌ای از خاکستر آتشین برج.", 3000, Currency.COIN),
        SkinEntry("bone", "پهلوان استخوانی", "Bone Champion", "روح یک صعودکننده‌ی قدیمی.", 25, Currency.GEM),
    )

    val themes: List<ThemeEntry> = listOf(
        ThemeEntry("classic", "سنگ نفرین‌شده", "Cursed Stone", "برج اصلی؛ سنگ سرد و مشعل‌های لرزان.", 0, Currency.COIN),
        ThemeEntry("ember", "برج آتش", "Ember Tower", "دیوارهایی که هنوز از آتش دیشب داغ‌اند.", 2000, Currency.COIN),
        ThemeEntry("frost", "برج یخ‌زده", "Frozen Tower", "نفرین سرد؛ همه‌چیز در یخ آبی فرو رفته.", 4000, Currency.COIN),
        ThemeEntry("abyss", "پرتگاه سبز", "Green Abyss", "نور سبز سمی از عمق برج بالا می‌آید.", 30, Currency.GEM),
    )

    val upgrades: List<UpgradeEntry> = listOf(
        UpgradeEntry(
            id = "extra_heart",
            persianName = "قلب اضافه",
            englishName = "Extra heart",
            persianDescription = "هر سطح یک قلب به شروع هر صعود اضافه می‌کند.",
            maxLevel = 2,
            prices = listOf(800, 2200),
            currency = Currency.COIN,
        ),
        UpgradeEntry(
            id = "start_shield",
            persianName = "سپر آغازین",
            englishName = "Starting shield",
            persianDescription = "هر صعود را با سپر محافظ شروع می‌کنی.",
            maxLevel = 1,
            prices = listOf(1200),
            currency = Currency.COIN,
        ),
        UpgradeEntry(
            id = "coin_insurance",
            persianName = "بیمه سکه",
            englishName = "Coin insurance",
            persianDescription = "پس از سقوط، همه‌ی سکه‌های همان صعود را نگه می‌داری.",
            maxLevel = 1,
            prices = listOf(2500),
            currency = Currency.COIN,
        ),
        UpgradeEntry(
            id = "coin_bonus",
            persianName = "کیسه طلا",
            englishName = "Gold pouch",
            persianDescription = "هر سطح، ارزش سکه‌های جمع‌شده را بیشتر می‌کند.",
            maxLevel = 3,
            prices = listOf(1000, 2500, 5000),
            currency = Currency.COIN,
        ),
        UpgradeEntry(
            id = "start_wings",
            persianName = "بال آغازین",
            englishName = "Starting wings",
            persianDescription = "با بال پرواز (پرش دوم) صعود را آغاز می‌کنی.",
            maxLevel = 1,
            prices = listOf(20),
            currency = Currency.GEM,
        ),
    )

    fun skin(id: String) = skins.firstOrNull { it.id == id } ?: skins.first()
    fun theme(id: String) = themes.firstOrNull { it.id == id } ?: themes.first()
    fun upgrade(id: String) = upgrades.first { it.id == id }

    /** Coin multiplier granted by the "کیسه طلا" upgrade. */
    fun coinMultiplier(level: Int): Float = when (level) {
        0 -> 1f
        1 -> 1.25f
        2 -> 1.5f
        else -> 2f
    }

    /** Fraction of run coins kept after a fall (100% with بیمه سکه). */
    fun coinKeepFraction(insuranceLevel: Int): Float = if (insuranceLevel > 0) 1f else 0.4f

    init {
        // keep the catalogue and the renderer palettes in sync
        require(skins.map { it.id }.toSet() == RenderStyles.heroSkins.map { it.id }.toSet())
        require(themes.map { it.id }.toSet() == RenderStyles.towerThemes.map { it.id }.toSet())
    }
}
