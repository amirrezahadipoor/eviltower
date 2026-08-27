package ir.hadipoor.eviltower.data

/** A single achievement (دستاورد) with a progress goal evaluated against the saved profile. */
data class Achievement(
    val id: String,
    val persianTitle: String,
    val englishTitle: String,
    val persianDescription: String,
    val goal: Int,
    val rewardGems: Int,
    val progressOf: (PlayerProfile) -> Int,
)

object Achievements {

    val all: List<Achievement> = listOf(
        Achievement(
            id = "floor_10",
            persianTitle = "اولین نگهبان",
            englishTitle = "First guardian",
            persianDescription = "به طبقه ۱۰ برس",
            goal = 10,
            rewardGems = 1,
            progressOf = { it.bestFloor },
        ),
        Achievement(
            id = "floor_25",
            persianTitle = "به طبقه ۲۵ برس",
            englishTitle = "Reach floor 25",
            persianDescription = "ارباب برج در طبقه ۲۵ منتظر توست",
            goal = 25,
            rewardGems = 2,
            progressOf = { it.bestFloor },
        ),
        Achievement(
            id = "floor_50",
            persianTitle = "نیمه‌ی برج",
            englishTitle = "Half way up",
            persianDescription = "به طبقه ۵۰ برس",
            goal = 50,
            rewardGems = 4,
            progressOf = { it.bestFloor },
        ),
        Achievement(
            id = "floor_100",
            persianTitle = "فاتح برج",
            englishTitle = "Tower conqueror",
            persianDescription = "به طبقه ۱۰۰ برس و نفرین را پایان بده",
            goal = 100,
            rewardGems = 15,
            progressOf = { it.bestFloor },
        ),
        Achievement(
            id = "coins_1000",
            persianTitle = "۱۰۰۰ سکه جمع کن",
            englishTitle = "Collect 1000 coins",
            persianDescription = "در مجموع ۱۰۰۰ سکه طلا جمع کن",
            goal = 1000,
            rewardGems = 1,
            progressOf = { it.totalCoins },
        ),
        Achievement(
            id = "coins_10000",
            persianTitle = "گنج برج",
            englishTitle = "Tower treasure",
            persianDescription = "در مجموع ۱۰۰۰۰ سکه طلا جمع کن",
            goal = 10_000,
            rewardGems = 5,
            progressOf = { it.totalCoins },
        ),
        Achievement(
            id = "enemies_100",
            persianTitle = "شکارچی هیولا",
            englishTitle = "Monster hunter",
            persianDescription = "۱۰۰ دشمن را شکست بده",
            goal = 100,
            rewardGems = 3,
            progressOf = { it.totalEnemies },
        ),
        Achievement(
            id = "runs_25",
            persianTitle = "پشتکار",
            englishTitle = "Persistent",
            persianDescription = "۲۵ بار صعود را آغاز کن",
            goal = 25,
            rewardGems = 2,
            progressOf = { it.totalRuns },
        ),
        Achievement(
            id = "score_5000",
            persianTitle = "امتیازِ افسانه‌ای",
            englishTitle = "Legendary score",
            persianDescription = "امتیاز ۵۰۰۰ را در یک صعود بگیر",
            goal = 5000,
            rewardGems = 4,
            progressOf = { it.bestScore },
        ),
        Achievement(
            id = "collector",
            persianTitle = "کلکسیونر",
            englishTitle = "Collector",
            persianDescription = "همه‌ی پوسته‌های قهرمان را باز کن",
            goal = ShopCatalog.skins.size,
            rewardGems = 6,
            progressOf = { it.unlockedSkins.size },
        ),
    )

    fun isUnlocked(achievement: Achievement, profile: PlayerProfile): Boolean =
        achievement.progressOf(profile) >= achievement.goal

    /** Achievements that just became true and were not stored yet. */
    fun newlyUnlocked(profile: PlayerProfile): List<Achievement> =
        all.filter { it.id !in profile.achievements && isUnlocked(it, profile) }
}
