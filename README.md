# برج شیطانی — Evil Tower

**برج شیطانی** یک بازی تک‌نقشه‌ی دفاع از برج برای اندروید است: از هسته‌ی آخرین قلعه در برابر
موج‌های پیوسته‌ای که از برج شیطانی می‌آیند دفاع کن. هر دور از موج ۱ شروع می‌شود، نقشه ثابت است،
اما ساخت و ارتقای برج‌ها و ترکیب دشمن‌ها در طول موج‌ها تصمیم بازیکن را مهم می‌کند. بعد از موج ۳۰۰
بازی بدون سقف ادامه پیدا می‌کند و هدف، رکوردشکنی است.

این مخزن عمداً از نسخه‌ی قبلی که «صعود از برج» بود جدا شده و از commit `477eaa5` به‌صورت کامل
با مدل دفاع موجی بازسازی شده است.

## اجرا و ساخت

نیازمندی: Android Studio جدید، JDK 17 و Android SDK 35.

```bash
git clone https://github.com/amirrezahadipoor/eviltower.git
cd eviltower
./gradlew test                  # تست موتور موج، ارتقا و شبیه‌سازی
./gradlew assembleDebug         # فقط برای تست محلی
./gradlew assembleRelease       # با کلید تنظیم‌شده یا کلید debug fallback
```

فایل APK نهایی طبق درخواست فقط از workflow دستی GitHub ساخته می‌شود:
1. در GitHub به `Actions` برو.
2. workflow با نام «تست و بسته‌بندی برج شیطانی» را انتخاب کن.
3. روی `Run workflow` بزن.
4. artifactهای `eviltower-debug-apk` و `eviltower-release-apk` و Release ساخته‌شده را دانلود کن.

Push و Pull Request فقط تست واحد را اجرا می‌کنند؛ این کار جلوی انتشار APK نیمه‌کاره را می‌گیرد.

## معماری

```text
app/src/main/java/ir/hadipoor/eviltower/
├── game/model/Entities.kt       مدل برج، دشمن، موج، پرتابه و snapshot
├── game/engine/GameConfig.kt     فرمول‌های موج/اقتصاد/ارتقا و نقشه‌ی ثابت
├── game/engine/GameEngine.kt     حلقه‌ی مستقل Kotlin: spawn، مسیر، هدف‌گیری، آسیب و باس
├── game/engine/ObjectPool.kt     پایه‌ی pool برای transientهای پرتعداد
├── ui/GameViewModel.kt           MVVM، حلقه‌ی ۶۰fps با fixed timestep، شروع/پایان دور و ذخیره‌ی نتیجه
├── ui/screens/GameCanvas.kt      رندر برداری Canvas، برج، دشمن، جاده، ذرات و پرتابه
├── ui/screens/GameScreen.kt      HUD، ساخت، ارتقا، فروش، توان آتش و توقف
├── ui/screens/MetaScreens.kt     فروشگاه، دستاوردها، رکوردها، تنظیمات و نتیجه
├── data/GameRepository.kt        DataStore آفلاین برای پیشرفت و رکورد
├── audio/GameAudio.kt            SoundPool + موسیقی loop محلی
└── monetization/                 BillingProvider و اتصال رسمی Poolakey بازار
```

`GameEngine` هیچ وابستگی به Android ندارد و با snapshotهای immutable توسط Compose مشاهده می‌شود.
بازی portrait-locked است تا جاده و پنل انتخاب برج روی گوشی خوانا بماند.

## موج و تعادل نهایی

فرمول‌ها برای هر عدد موج `w` تعریف شده‌اند و روی ۳۰۰ متوقف نمی‌شوند:

- `HP(w) = 24 × 1.03^(w-1)`؛ رشد مجاور دقیقاً ۳٪ و بدون جهش ناگهانی.
- `speed(type,w) = baseSpeed(type) × 1.0018^(w-1)`.
- `count(w) = 7 + floor(w^0.72)`.
- پاداش کشت: `ceil(baseReward × (1 + 0.028 × (w-1)^0.78))`.
- پاداش پاک‌سازی: `ceil(28 × w^0.58)`؛ اقتصاد عمداً آهسته‌تر از قدرت دشمن رشد می‌کند.
- هر موج مضرب ۱۰ یک باس کامل و هر مضرب ۵ غیرمضرب ۱۰ یک مینی‌باس دارد؛ باس و مینی‌باس جای
  همدیگر را نمی‌گیرند و روی موج ۱۰ فقط باس وجود دارد.
- `BossHP = regularHP × (7 + w/90)` و `MiniBossHP = regularHP × (5.5 + w/110)`؛ هر دو با ضریب elite برابر ۱٫۱۸.
- از موج ۳۰ همه‌ی خانواده‌های دشمن وارد چرخش می‌شوند و هر ۴۰ موج نسخه‌ی بصری/نخبگی با ضریب
  `1 + 0.12 × floor((w-1)/40)` دیده می‌شود.
- از موج ۳۰۱ متن «شما وارد فاز بی‌پایان شده‌اید» نشان داده می‌شود و هیچ cap برای موج/HP وجود ندارد.

ارتقای هر برج:

- هزینه: `baseCost × 1.017^(level-1)`
- آسیب: `baseDamage × 1.07^(level-1)`
- برد: `baseRange × (1 + 0.006 × (level-1))`
- فاصله‌ی شلیک: `baseInterval × 0.992^(level-1)` با کف ۰٫۱۸ ثانیه
- مقاومت اسکلت ۳۲٪ و نامرئی‌بودن شبح ۴۵٪ کاهش آسیب غیر Arcane دارد؛ Arcane این دو را نادیده می‌گیرد.
- توپخانه تا پنج هدف نزدیک را splash می‌کند، رعد تا سه زنجیره می‌سازد و عنکبوت برج را ۲٫۵ ثانیه web می‌کند.
- سطح ۱ تا ۱۰۰؛ در هر ۱۰ سطح یک silhouette/SVG tier و در هر سطح glow، جزئیات و burst جدید.

`tools/balance_simulation.py` همان ضرایب را در چند نقطه‌ی موج چاپ می‌کند تا نسبت فشار مجاور،
رشد درآمد و فاز بی‌پایان قابل بازبینی باشد. تست‌ها در `app/src/test` موج‌های باس، موج ۳۰۱،
محدودیت‌ها و جریان ساخت/ارتقا/فروش را پوشش می‌دهند.

## محتوای بازی

هفت برج: `برج تیرانداز`، `برج توپخانه`، `برج یخ`، `برج آتش`، `برج رعد`، `برج کماندار آسمان`
و `برج جادوی اهریمنی` که در فروشگاه باز می‌شود؛ پوسته‌ی دائمی «شراره» هم برای همه‌ی برج‌ها قابل خرید است. فقط کماندار آسمان خفاش‌ها و دیگر دشمنان پرنده
را هدف می‌گیرد؛ توپخانه splash، یخ slow، آتش burn، رعد chain و جادوی اهریمنی مقاومت زره را دور
می‌زند.

هشت خانواده‌ی دشمن: پیاده اهریمنی، گرگ سایه، خفاش شیطانی، اسکلت زره‌پوش، عنکبوت اهریمنی،
غول سنگی، شبح سایه و جن آتشین؛ شش نام مینی‌باس و شش باس چرخشی در موتور ثبت شده‌اند.

## گرافیک و game feel

انتخاب فنی: مسیرها و شکل‌های Vector/SVG در `app/src/main/assets/svg` نگهداری می‌شوند و رندر زنده
با Compose Canvas انجام می‌شود؛ pathهای ثابت دوباره‌خوانی شبکه یا bitmap ندارند. اسکریپت
`tools/generate_vector_assets.py` برای هر ۷ برج، ۱۰ tier و برای خانواده‌ها variantهای editable
می‌سازد. رندرر با tier/level اندازه، هاله، gem/rune و جزئیات را پارامتریک می‌کند.

پیاده‌سازی‌شده: نقشه‌ی پویا و برج شیطانی glowing، مه متحرک، جاده و ۱۲ plot، enemy bob و stealth،
HP bar، arrow/fire/ice/cannon/arcane projectile، lightning chain، hit flash، damage number،
ذرات مرگ و ارتقا، combo، health bar و phase باس، هشدار باس، countdown، range indicator،
توان آتش و سپر هسته با cooldown، telegraph ضربه‌ی باس، لرزش دوربین، pinch-to-zoom/pan، دکمه‌ی bounce، رنگ‌بندی تاریک، فونت
Vazirmatn محلی و RTL. ذرات با pool بازیافت می‌شوند تا در فاز بی‌پایان فشار حافظه کنترل شود.
موسیقی زمینه‌ی loop و ۱۴ SFX با `tools/generate_audio.py` رویه‌ای و بدون فایل شخص ثالث ساخته می‌شوند.

## ذخیره‌سازی و فروشگاه

DataStore کاملاً آفلاین رکورد شخصی، ۱۰ دفاع اخیر، سکه‌ی دائمی، جواهر، تعداد دشمن/باس، بازشدن
برج اهریمنی، پاداش شروع و چهار تنظیم را نگه می‌دارد. نتیجه‌ی هر دور ۴۰٪ سکه‌ی آن دور را به meta
currency تبدیل می‌کند.

`BazaarBillingProvider` پیاده‌سازی اصلی `BillingProvider` با SDK رسمی Poolakey است؛ Google Play
Billing و Google Play Services در پروژه وجود ندارند. اگر کافه‌بازار روی دستگاه نصب نباشد خرید
Unavailable می‌شود و دفاع رایگان همچنان کامل کار می‌کند. کلید RSA بازار فقط از `BAZAAR_RSA_KEY`
در build گرفته می‌شود و هرگز در git ذخیره نمی‌شود.

## انتشار کافه‌بازار

- applicationId: `ir.hadipoor.eviltower`
- نام فروشگاهی: `برج شیطانی`
- Target SDK: 35، Min SDK: 24، بدون Max SDK
- کلید واقعی را هرگز commit نکن. `keystore.properties.example` و `.gitignore` آماده‌اند.
- ساخت کلید نمونه:

```bash
keytool -genkeypair -v -keystore eviltower-release.jks -alias eviltower \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Evil Tower, OU=Games, O=Hadipoor, L=Montreal, S=Quebec, C=CA"
```

در GitHub Secrets مقادیر `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` را
تنظیم کن. اگر تنظیم نباشند workflow برای تست یک کلید موقت می‌سازد؛ آن خروجی برای انتشار نهایی
Bazaar مناسب نیست و باید با کلید مالک برنامه امضا شود.

قواعدی که هنگام بسته‌بندی بازبینی شده‌اند: Target SDK کمتر از ۳۲ در بازار قابل انتشار نیست و
راهنمای پیشخان سقف ۱۵۰MB برای APK را ذکر می‌کند. متن، رده‌بندی و نمونه‌های تصویری در `store-listing`
هستند؛ تصاویر آنجا mockup برند هستند و پس از دریافت APK نهایی باید با screenshot واقعی جایگزین شوند.
راهنمای مرجع: https://developers.cafebazaar.ir/fa/guidelines/getting-started/practical-principles

## چک‌لیست QA دستی

- [ ] Splash به منو می‌رسد و همه‌ی نوشته‌ها RTL و با Vazirmatn هستند.
- [ ] شروع دفاع، countdown، spawn موج، حرکت دشمن و کم‌شدن HP هسته دیده می‌شود.
- [ ] لمس هر یک از ۱۲ plot پنل ساخت/ارتقای همان plot را باز می‌کند.
- [ ] کمبود طلا ساخت/ارتقا را غیرفعال می‌کند؛ فروش طلا را برمی‌گرداند.
- [ ] همه‌ی شش برج پایه قابلیت هدف‌گیری دارند و خفاش فقط از کماندار آسمان آسیب می‌گیرد.
- [ ] موج ۵ مینی‌باس، موج ۱۰ باس و نوار جان/هشدار آن‌ها بررسی شود.
- [ ] توان آتش و سپر هسته cooldown دارند؛ حلقه‌ی سپر، damage number، particle و combo دیده می‌شود.
- [ ] توقف، ادامه، شروع دوباره و خروج به منو درست کار می‌کند.
- [ ] پایان دور نتیجه، رکورد جدید، دوباره تلاش و منوی اصلی را نشان می‌دهد.
- [ ] فروشگاه، بازکردن برج اهریمنی، پوسته‌ی شراره و پاداش شروع پس از restart برنامه باقی می‌ماند.
- [ ] تنظیمات صدا/موسیقی/لرزش/گرافیک سبک persistence دارند.
- [ ] `./gradlew test` و سپس workflow دستی debug و release سبز هستند.
