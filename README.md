# برج شیطانی — Evil Tower

یک بازی اکشن-آرکید عمودیِ صعود به برج، ساخته‌شده برای اندروید با Kotlin و Jetpack Compose.
تمام متن‌ها، داستان و رابط کاربری فارسی و راست‌به‌چپ (RTL) هستند و تمام گرافیک بازی برداری (SVG/Vector) و متحرک است.

> A vertical tower-climbing action/arcade game for Android. Persian (RTL) UI, animated vector art,
> procedurally generated floors, rogue-lite meta progression, Cafe Bazaar release target.

## وضعیت پروژه / Project status

در حال ساخت — این فایل با هر گام از توسعه به‌روزرسانی می‌شود.

- [x] اسکلت پروژه (Gradle + Compose + تم تاریک + فونت وزیرمتن)
- [ ] موتور بازی و تولید رویه‌ای طبقات
- [ ] دشمنان، تله‌ها و پاورآپ‌ها
- [ ] صفحات UI (منو، فروشگاه، تنظیمات، دستاوردها)
- [ ] ذخیره‌سازی و پیشرفت
- [ ] صدا
- [ ] آماده‌سازی انتشار در کافه بازار

## Build

```bash
./gradlew assembleDebug     # debug APK  -> app/build/outputs/apk/debug/
./gradlew assembleRelease   # release APK -> app/build/outputs/apk/release/
./gradlew test              # unit tests
```

Requirements: JDK 17, Android SDK 35, minSdk 24, portrait-locked.

APKها به‌صورت خودکار توسط GitHub Actions ساخته می‌شوند (بخش Actions → آخرین run → Artifacts).

## Tech stack

| بخش | انتخاب |
| --- | --- |
| زبان | Kotlin |
| UI | Jetpack Compose (Material 3) |
| رندر بازی | Compose Canvas + مسیرهای برداری SVG با `PathParser` (۶۰ فریم بر ثانیه) |
| معماری | MVVM: `GameEngine` / `GameViewModel` / Composables |
| ذخیره‌سازی | DataStore (Preferences) — کاملاً آفلاین |
| صدا | SoundPool (SFX) + MediaPlayer (موسیقی) |
| پرداخت | Cafe Bazaar Poolakey (بدون Google Play Billing) |
| فونت | Vazirmatn (محلی، SIL OFL) |

## License

Game code: MIT. فونت وزیرمتن تحت مجوز SIL OFL (فایل `VAZIRMATN-OFL.txt`).
