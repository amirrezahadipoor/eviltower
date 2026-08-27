#!/usr/bin/env python3
"""
Render the Cafe Bazaar store-listing artwork for برج شیطانی.

Produces (in `store-listing/`):
  * icon-512.png            512x512   app icon
  * feature-graphic.png    1024x500   cover / feature graphic
  * screenshots/*.png      1080x1920  six portrait store screenshots

NOTE: the screenshots are *mock-ups* drawn with the game's own palette, fonts and shapes.
They are placeholders for the store listing; before the real submission replace them with
captures from the app running on a device (see store-listing/README.md).

Usage:
    python3 tools/generate_store_assets.py
"""
from __future__ import annotations

import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont, features

# Pillow with libraqm shapes Persian text correctly (HarfBuzz + FriBiDi).
# The manual reshaper is only a fallback for builds without raqm.
HAS_RAQM = features.check("raqm")
if not HAS_RAQM:  # pragma: no cover
    import arabic_reshaper
    from bidi.algorithm import get_display

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "store-listing")
SHOTS = os.path.join(OUT, "screenshots")
FONT_DIR = os.path.join(ROOT, "app/src/main/res/font")

NIGHT = (11, 7, 19)
DEEP = (22, 14, 34)
PURPLE = (42, 27, 61)
PURPLE_L = (91, 62, 140)
EMBER = (255, 122, 24)
EMBER_S = (255, 176, 103)
TORCH = (255, 210, 125)
STONE = (110, 106, 124)
STONE_D = (59, 54, 70)
GOLD = (255, 201, 60)
GEM = (79, 214, 255)
BLOOD = (200, 32, 60)
TEXT = (242, 236, 255)
MUTED = (182, 173, 200)
PLATFORM = (74, 65, 89)
PLATFORM_E = (110, 98, 136)


def font(size, bold=False):
    name = "vazirmatn_bold.ttf" if bold else "vazirmatn_regular.ttf"
    return ImageFont.truetype(os.path.join(FONT_DIR, name), size)


def fa(text: str) -> str:
    if HAS_RAQM:
        return text
    return get_display(arabic_reshaper.reshape(text))


def text_rtl(draw, xy, string, fnt, fill, anchor="ra"):
    """Draw shaped, right-to-left Persian text."""
    if HAS_RAQM:
        draw.text(xy, string, font=fnt, fill=fill, anchor=anchor, direction="rtl", language="fa")
    else:
        draw.text(xy, fa(string), font=fnt, fill=fill, anchor=anchor)


def vgrad(size, top, bottom):
    w, h = size
    img = Image.new("RGB", (1, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.point((0, y), fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return img.resize((w, h))


def glow(img, box, color, radius, alpha=110):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x, y = box
    d.ellipse([x - radius, y - radius, x + radius, y + radius], fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(radius * 0.45))
    img.alpha_composite(layer)


# ------------------------------------------------------------------ art primitives
def draw_hero(d, x, y, s, armor=(138, 147, 181), dark=(86, 94, 124), cape=(177, 58, 78),
              trim=GOLD, visor=(26, 22, 38)):
    """s = height in px, hero drawn standing on (x, y)."""
    w = s * 0.62
    d.polygon([(x - w * 0.5, y - s * 0.86), (x - w * 0.95, y - s * 0.1),
               (x - w * 0.1, y - s * 0.3)], fill=cape)
    d.rectangle([x - w * 0.28, y - s * 0.42, x - w * 0.06, y], fill=dark)
    d.rectangle([x + w * 0.04, y - s * 0.42, x + w * 0.26, y], fill=dark)
    d.polygon([(x - w * 0.42, y - s * 0.86), (x + w * 0.42, y - s * 0.86),
               (x + w * 0.34, y - s * 0.34), (x - w * 0.34, y - s * 0.34)], fill=armor)
    d.rectangle([x - w * 0.36, y - s * 0.44, x + w * 0.36, y - s * 0.34], fill=trim)
    d.rounded_rectangle([x - w * 0.4, y - s * 1.18, x + w * 0.4, y - s * 0.82], radius=int(s * 0.12), fill=armor)
    d.rectangle([x - w * 0.4, y - s * 1.04, x + w * 0.4, y - s * 0.95], fill=visor)
    d.ellipse([x + w * 0.12, y - s * 1.03, x + w * 0.26, y - s * 0.94], fill=EMBER)
    d.polygon([(x - w * 0.1, y - s * 1.34), (x + w * 0.1, y - s * 1.34),
               (x + w * 0.2, y - s * 1.14), (x - w * 0.2, y - s * 1.14)], fill=trim)
    # sword
    d.polygon([(x + w * 0.5, y - s * 0.9), (x + w * 0.66, y - s * 0.9),
               (x + w * 0.62, y - s * 0.1), (x + w * 0.54, y - s * 0.1)], fill=(231, 236, 255))
    d.rectangle([x + w * 0.36, y - s * 0.92, x + w * 0.8, y - s * 0.84], fill=trim)


def draw_platform(d, x0, x1, y, h=16):
    d.rectangle([x0, y, x1, y + h], fill=PLATFORM)
    d.rectangle([x0, y, x1, y + h * 0.3], fill=PLATFORM_E)
    d.rectangle([x0, y + h * 0.8, x1, y + h], fill=(20, 14, 28))
    x = x0 + 26
    while x < x1 - 6:
        d.line([(x, y), (x, y + h)], fill=(18, 14, 28), width=2)
        x += 52


def draw_torch(img, d, x, y, s=1.0):
    d.rectangle([x - 6 * s, y, x + 6 * s, y + 34 * s], fill=STONE_D)
    glow(img, (x, y - 6 * s), EMBER, int(90 * s), 90)
    d.polygon([(x, y - 46 * s), (x + 15 * s, y - 8 * s), (x, y + 6 * s), (x - 15 * s, y - 8 * s)], fill=EMBER)
    d.polygon([(x, y - 30 * s), (x + 8 * s, y - 6 * s), (x, y + 2 * s), (x - 8 * s, y - 6 * s)], fill=TORCH)


def draw_coin(d, x, y, r=16):
    d.ellipse([x - r, y - r, x + r, y + r], fill=GOLD)
    d.ellipse([x - r * 0.66, y - r * 0.66, x + r * 0.66, y + r * 0.66], fill=(255, 233, 163))
    d.rectangle([x - r * 0.16, y - r * 0.42, x + r * 0.16, y + r * 0.42], fill=(176, 122, 18))
    d.rectangle([x - r * 0.42, y - r * 0.16, x + r * 0.42, y + r * 0.16], fill=(176, 122, 18))


def draw_heart(d, x, y, r=16, filled=True):
    color = BLOOD if filled else STONE_D
    d.ellipse([x - r, y - r * 0.9, x, y + r * 0.1], fill=color)
    d.ellipse([x, y - r * 0.9, x + r, y + r * 0.1], fill=color)
    d.polygon([(x - r, y - r * 0.35), (x + r, y - r * 0.35), (x, y + r)], fill=color)


def draw_gem(d, x, y, r=16):
    d.polygon([(x, y - r), (x + r * 0.85, y - r * 0.3), (x + r * 0.5, y + r),
               (x - r * 0.5, y + r), (x - r * 0.85, y - r * 0.3)], fill=GEM)
    d.polygon([(x, y - r), (x + r * 0.5, y + r), (x - r * 0.5, y + r)], fill=(191, 243, 255))


def draw_bat(d, x, y, s=34):
    d.polygon([(x, y), (x - s, y - s * 0.5), (x - s * 0.7, y + s * 0.15)], fill=(67, 48, 92))
    d.polygon([(x, y), (x + s, y - s * 0.5), (x + s * 0.7, y + s * 0.15)], fill=(67, 48, 92))
    d.ellipse([x - s * 0.3, y - s * 0.35, x + s * 0.3, y + s * 0.45], fill=(53, 37, 77))
    d.polygon([(x - s * 0.26, y - s * 0.3), (x - s * 0.1, y - s * 0.75), (x - s * 0.02, y - s * 0.28)], fill=(44, 31, 64))
    d.polygon([(x + s * 0.26, y - s * 0.3), (x + s * 0.1, y - s * 0.75), (x + s * 0.02, y - s * 0.28)], fill=(44, 31, 64))
    d.ellipse([x - s * 0.2, y - s * 0.16, x - s * 0.06, y - s * 0.02], fill=(255, 59, 87))
    d.ellipse([x + s * 0.06, y - s * 0.16, x + s * 0.2, y - s * 0.02], fill=(255, 59, 87))


def draw_blade(d, x, y, r=34, angle=0.0):
    pts = []
    for i in range(16):
        a = angle + i * math.pi / 8
        rad = r if i % 2 == 0 else r * 0.45
        pts.append((x + math.cos(a) * rad, y + math.sin(a) * rad))
    d.polygon(pts, fill=(201, 206, 221))
    d.ellipse([x - r * 0.3, y - r * 0.3, x + r * 0.3, y + r * 0.3], fill=(90, 95, 112))


def draw_guardian(d, x, y, s=190):
    w = s * 0.8
    d.polygon([(x - w * 0.5, y - s * 0.78), (x - w * 0.72, y - s * 0.98), (x - w * 0.3, y - s * 0.86)], fill=(232, 226, 208))
    d.polygon([(x + w * 0.5, y - s * 0.78), (x + w * 0.72, y - s * 0.98), (x + w * 0.3, y - s * 0.86)], fill=(232, 226, 208))
    d.rounded_rectangle([x - w * 0.55, y - s * 0.9, x + w * 0.55, y], radius=int(s * 0.16), fill=(107, 47, 62))
    d.ellipse([x - w * 0.34, y - s * 0.68, x - w * 0.1, y - s * 0.5], fill=TORCH)
    d.ellipse([x + w * 0.1, y - s * 0.68, x + w * 0.34, y - s * 0.5], fill=TORCH)
    d.polygon([(x - w * 0.3, y - s * 0.38), (x + w * 0.3, y - s * 0.38),
               (x + w * 0.2, y - s * 0.16), (x - w * 0.2, y - s * 0.16)], fill=(26, 13, 18))
    for i in range(4):
        tx = x - w * 0.24 + i * w * 0.16
        d.polygon([(tx, y - s * 0.38), (tx + w * 0.06, y - s * 0.24), (tx + w * 0.12, y - s * 0.38)], fill=(240, 240, 235))


def tower_background(w, h, floors=6, base=0):
    img = vgrad((w, h), (38, 26, 60), NIGHT).convert("RGBA")
    d = ImageDraw.Draw(img)
    # brick wall
    bw, bh = 132, 74
    for row in range(-1, h // bh + 2):
        for col in range(-1, w // bw + 2):
            x = col * bw + (bw // 2 if row % 2 else 0)
            y = row * bh
            shade = PURPLE if (row + col) % 3 else (26, 19, 38)
            d.rectangle([x + 3, y + 3, x + bw - 3, y + bh - 3], fill=shade)
    # side walls
    d.rectangle([0, 0, 58, h], fill=(20, 14, 30))
    d.rectangle([w - 58, 0, w, h], fill=(20, 14, 30))
    return img, d


# ------------------------------------------------------------------ screens
def screen_menu(w, h):
    img, d = tower_background(w, h)
    overlay = Image.new("RGBA", (w, h), (11, 7, 19, 150))
    img.alpha_composite(overlay)
    d = ImageDraw.Draw(img)

    glow(img, (w // 2, 470), EMBER, 320, 70)
    d = ImageDraw.Draw(img)
    # logo tower
    cx = w // 2
    d.polygon([(cx, 210), (cx + 150, 360), (cx - 150, 360)], fill=PURPLE_L)
    d.rectangle([cx - 110, 360, cx + 110, 640], fill=PURPLE)
    d.rectangle([cx - 110, 360, cx + 110, 640], outline=PURPLE_L, width=4)
    for i in range(4):  # crenellations
        bx = cx - 110 + i * 62
        d.rectangle([bx, 330, bx + 34, 366], fill=PURPLE_L)
    d.ellipse([cx - 34, 440, cx + 34, 508], fill=EMBER)
    for i in range(3):
        d.line([(cx - 110, 470 + i * 60), (cx + 110, 470 + i * 60)], fill=(24, 16, 36), width=3)

    text_rtl(d, (cx, 680), "برج شیطانی", font(92, True), TEXT, anchor="ma")
    text_rtl(d, (cx, 828), "هر شب یک طبقه بلندتر، هر شب یک روح بیشتر", font(34), MUTED, anchor="ma")

    buttons = [("شروع صعود", True), ("فروشگاه", False), ("دستاوردها", False),
               ("امتیازات برتر", False), ("تنظیمات", False)]
    y = 950
    for label, primary in buttons:
        fill = EMBER if primary else PURPLE
        d.rounded_rectangle([90, y, w - 90, y + 118], radius=26, fill=fill,
                            outline=TORCH if primary else PURPLE_L, width=3)
        text_rtl(d, (w // 2, y + 34), label, font(46, True), (11, 7, 19) if primary else TEXT, anchor="ma")
        y += 142

    # currency chips
    d.rounded_rectangle([70, 70, 330, 150], radius=40, fill=(7, 4, 13, 220), outline=PURPLE_L, width=2)
    draw_coin(d, 290, 110, 26)
    text_rtl(d, (250, 82), "۴٬۲۵۰", font(40, True), GOLD)
    d.rounded_rectangle([360, 70, 590, 150], radius=40, fill=(7, 4, 13, 220), outline=PURPLE_L, width=2)
    draw_gem(d, 552, 110, 24)
    text_rtl(d, (512, 82), "۱۸", font(40, True), GEM)
    return img


def screen_gameplay(w, h):
    img, d = tower_background(w, h)
    draw_torch(img, d, 96, 420)
    draw_torch(img, d, w - 96, 900)
    d = ImageDraw.Draw(img)

    draw_platform(d, 58, w - 58, 1700, 26)
    draw_platform(d, 120, 520, 1440)
    draw_platform(d, 620, 1000, 1210)
    draw_platform(d, 180, 560, 980)
    draw_platform(d, 660, 1020, 750)
    draw_platform(d, 240, 620, 520)

    draw_coin(d, 320, 1380, 20)
    draw_coin(d, 800, 1150, 20)
    draw_coin(d, 380, 920, 20)
    draw_gem(d, 840, 690, 22)
    draw_bat(d, 780, 980, 54)
    draw_blade(d, 300, 1120, 52, 0.4)

    # serpent
    d.polygon([(660, 1200), (700, 1178), (760, 1200), (820, 1178), (860, 1200),
               (860, 1210), (760, 1216), (660, 1210)], fill=(122, 139, 107))
    d.ellipse([840, 1170, 890, 1208], fill=(147, 166, 131))
    d.ellipse([866, 1180, 882, 1194], fill=EMBER)

    draw_hero(d, 360, 1440, 150)
    glow(img, (360, 1360), EMBER, 150, 45)
    d = ImageDraw.Draw(img)

    # HUD
    for i in range(3):
        draw_heart(d, 110 + i * 62, 120, 24, filled=i < 2)
    draw_coin(d, w - 250, 120, 22)
    text_rtl(d, (w - 290, 96), "۱۲۷", font(40, True), GOLD)
    d.ellipse([w - 190, 84, w - 118, 156], fill=(42, 27, 61, 220))
    d.rectangle([w - 168, 104, w - 156, 136], fill=EMBER_S)
    d.rectangle([w - 148, 104, w - 136, 136], fill=EMBER_S)

    text_rtl(d, (w - 90, 190), "طبقه ۲۳", font(56, True), TEXT)
    text_rtl(d, (100, 200), "امتیاز ۶٬۹۴۰", font(36), MUTED, anchor="la")
    return img


def screen_boss(w, h):
    img, d = tower_background(w, h)
    overlay = Image.new("RGBA", (w, h), (60, 8, 20, 60))
    img.alpha_composite(overlay)
    d = ImageDraw.Draw(img)
    draw_platform(d, 58, w - 58, 1700, 26)
    draw_platform(d, 90, 420, 1380)
    draw_platform(d, 660, 990, 1380)
    draw_platform(d, 340, 740, 1080)

    glow(img, (w // 2, 1420), (255, 59, 87), 340, 80)
    d = ImageDraw.Draw(img)
    draw_guardian(d, w // 2 + 120, 1700, 380)
    draw_hero(d, 300, 1380, 150)

    # boss bar
    d.rounded_rectangle([160, 300, w - 160, 356], radius=14, fill=(7, 4, 13, 220), outline=STONE_D, width=2)
    d.rounded_rectangle([164, 304, 700, 352], radius=12, fill=BLOOD)
    text_rtl(d, (w // 2, 220), "دیو دروازه‌بان بیدار شد!", font(52, True), EMBER, anchor="ma")

    for i in range(3):
        draw_heart(d, 110 + i * 62, 120, 24, filled=i < 3)
    text_rtl(d, (w - 90, 110), "طبقه ۳۰", font(50, True), TEXT)
    return img


def screen_shop(w, h):
    img = vgrad((w, h), DEEP, NIGHT).convert("RGBA")
    d = ImageDraw.Draw(img)
    text_rtl(d, (w - 80, 90), "فروشگاه", font(64, True), TEXT)
    d.rounded_rectangle([80, 80, 300, 160], radius=40, fill=(42, 27, 61), outline=PURPLE_L, width=2)
    draw_coin(d, 262, 120, 24)
    text_rtl(d, (226, 94), "۴٬۲۵۰", font(38, True), GOLD)

    tabs = ["پوسته قهرمان", "پوسته برج", "ارتقاها"]
    x = 70
    tw = (w - 140 - 40) // 3
    for i, tab in enumerate(tabs):
        fill = EMBER if i == 0 else (42, 27, 61)
        d.rounded_rectangle([x, 220, x + tw, 310], radius=20, fill=fill)
        text_rtl(d, (x + tw // 2, 240), tab, font(34, True), (11, 7, 19) if i == 0 else MUTED, anchor="ma")
        x += tw + 20

    items = [
        ("شوالیه گرفتار", "قهرمان اصلی داستان، آماده صعود.", "فعال", (138, 147, 181), (177, 58, 78)),
        ("راهب سایه", "از تاریکی زاده شده؛ در طبقات تاریک دیده نمی‌شود.", "۱۵۰۰", (69, 58, 99), (109, 59, 196)),
        ("جنگاور آتش", "زره‌ای از خاکستر آتشین برج.", "۳۰۰۰", (140, 58, 30), (255, 122, 24)),
        ("پهلوان استخوانی", "روح یک صعودکننده‌ی قدیمی.", "۲۵ جواهر", (232, 226, 208), (62, 74, 91)),
    ]
    y = 360
    for name, desc, price, armor, cape in items:
        d.rounded_rectangle([70, y, w - 70, y + 250], radius=28, fill=(22, 14, 34),
                            outline=EMBER if price == "فعال" else (60, 48, 82), width=3)
        d.rounded_rectangle([w - 300, y + 30, w - 110, y + 220], radius=22, fill=(7, 4, 13))
        draw_hero(d, w - 205, y + 200, 130, armor=armor, cape=cape)
        text_rtl(d, (w - 330, y + 44), name, font(44, True), TEXT)
        text_rtl(d, (w - 330, y + 110), desc, font(30), MUTED)
        colour = EMBER if price == "فعال" else GOLD
        text_rtl(d, (250, y + 110), price, font(40, True), colour, anchor="la")
        if price not in ("فعال",):
            draw_coin(d, 200, y + 128, 22) if "جواهر" not in price else draw_gem(d, 200, y + 128, 22)
        y += 280
    return img


def screen_achievements(w, h):
    img = vgrad((w, h), DEEP, NIGHT).convert("RGBA")
    d = ImageDraw.Draw(img)
    text_rtl(d, (w - 80, 90), "دستاوردها", font(64, True), TEXT)
    rows = [
        ("اولین نگهبان", "به طبقه ۱۰ برس", 1.0, "۱"),
        ("به طبقه ۲۵ برس", "ارباب برج در طبقه ۲۵ منتظر توست", 1.0, "۲"),
        ("نیمه‌ی برج", "به طبقه ۵۰ برس", 0.62, "۴"),
        ("۱۰۰۰ سکه جمع کن", "در مجموع ۱۰۰۰ سکه طلا جمع کن", 1.0, "۱"),
        ("شکارچی هیولا", "۱۰۰ دشمن را شکست بده", 0.44, "۳"),
        ("فاتح برج", "به طبقه ۱۰۰ برس و نفرین را پایان بده", 0.31, "۱۵"),
    ]
    y = 220
    for title, desc, prog, gems in rows:
        done = prog >= 1.0
        d.rounded_rectangle([70, y, w - 70, y + 230], radius=26, fill=(22, 14, 34),
                            outline=EMBER if done else (60, 48, 82), width=3)
        d.rounded_rectangle([w - 250, y + 40, w - 120, y + 170], radius=20, fill=(7, 4, 13))
        skull_c = EMBER if done else STONE_D
        d.ellipse([w - 226, y + 62, w - 144, y + 140], fill=skull_c)
        d.rectangle([w - 210, y + 118, w - 160, y + 152], fill=skull_c)
        d.ellipse([w - 214, y + 84, w - 190, y + 108], fill=(11, 7, 19))
        d.ellipse([w - 180, y + 84, w - 156, y + 108], fill=(11, 7, 19))
        text_rtl(d, (w - 280, y + 46), title, font(42, True), TEXT if done else MUTED)
        text_rtl(d, (w - 280, y + 104), desc, font(28), MUTED)
        d.rounded_rectangle([220, y + 160, w - 280, y + 178], radius=9, fill=PURPLE)
        bar_w = (w - 280 - 220) * prog
        d.rounded_rectangle([w - 280 - bar_w, y + 160, w - 280, y + 178], radius=9,
                            fill=EMBER if done else PURPLE_L)
        draw_gem(d, 150, y + 100, 24)
        text_rtl(d, (150, y + 130), gems, font(32, True), GEM, anchor="ma")
        y += 258
    return img


def screen_gameover(w, h):
    img = screen_gameplay(w, h)
    img.alpha_composite(Image.new("RGBA", (w, h), (7, 4, 13, 220)))
    d = ImageDraw.Draw(img)
    panel = [90, 480, w - 90, 1560]
    d.rounded_rectangle(panel, radius=34, fill=(26, 17, 40), outline=PURPLE_L, width=3)
    draw_hero(d, w // 2, 780, 170)
    text_rtl(d, (w // 2, 830), "سقوط کردی!", font(78, True), BLOOD, anchor="ma")
    text_rtl(d, (w // 2, 940), "برج تو را به طبقه‌ی اول بازگرداند", font(34), MUTED, anchor="ma")

    rows = [("طبقات پیموده‌شده", "۲۳"), ("امتیاز", "۶٬۹۴۰"), ("سکه‌های به‌دست‌آمده", "۱۲۷")]
    y = 1030
    for label, value in rows:
        text_rtl(d, (w - 150, y), label, font(38), MUTED)
        text_rtl(d, (170, y), value, font(42, True), TEXT, anchor="la")
        y += 80
    text_rtl(d, (w // 2, 1270), "رکورد تازه!", font(44, True), EMBER, anchor="ma")

    d.rounded_rectangle([150, 1350, w - 150, 1450], radius=24, fill=EMBER, outline=TORCH, width=3)
    text_rtl(d, (w // 2, 1372), "دوباره تلاش کن", font(44, True), (11, 7, 19), anchor="ma")
    d.rounded_rectangle([150, 1470, w - 150, 1545], radius=22, fill=PURPLE, outline=PURPLE_L, width=2)
    text_rtl(d, (w // 2, 1487), "منوی اصلی", font(38, True), TEXT, anchor="ma")
    return img


def make_icon(size=512):
    img = Image.new("RGBA", (size, size), (22, 14, 34, 255))
    d = ImageDraw.Draw(img)
    u = size / 108
    glow(img, (size // 2, int(58 * u)), EMBER, int(38 * u), 90)
    d = ImageDraw.Draw(img)
    d.polygon([(54 * u, 10 * u), (78 * u, 38 * u), (30 * u, 38 * u)], fill=PURPLE_L)
    d.rectangle([36 * u, 38 * u, 72 * u, 96 * u], fill=(58, 42, 85))
    d.ellipse([46 * u, 48 * u, 62 * u, 64 * u], fill=EMBER)
    d.rectangle([46 * u, 56 * u, 62 * u, 72 * u], fill=EMBER)
    d.rounded_rectangle([46 * u, 74 * u, 62 * u, 96 * u], radius=int(8 * u), fill=(27, 18, 41))
    for i in range(3):
        d.line([(36 * u, (52 + i * 14) * u), (72 * u, (52 + i * 14) * u)], fill=(36, 25, 52), width=int(1.6 * u))
    return img


def make_feature_graphic(w=1024, h=500):
    img = vgrad((w, h), (38, 26, 60), NIGHT).convert("RGBA")
    d = ImageDraw.Draw(img)
    for row in range(-1, h // 44 + 2):
        for col in range(-1, w // 78 + 2):
            x = col * 78 + (39 if row % 2 else 0)
            y = row * 44
            if (row + col) % 3 == 0:
                d.rectangle([x + 2, y + 2, x + 76, y + 42], fill=(26, 19, 38))
    glow(img, (250, 250), EMBER, 240, 90)
    d = ImageDraw.Draw(img)
    # tower
    d.polygon([(250, 60), (340, 150), (160, 150)], fill=PURPLE_L)
    d.rectangle([180, 150, 320, 430], fill=PURPLE, outline=PURPLE_L, width=3)
    d.ellipse([228, 210, 272, 254], fill=EMBER)
    draw_hero(d, 400, 430, 150)
    draw_bat(d, 520, 190, 60)
    draw_blade(d, 150, 380, 40, 0.3)
    text_rtl(d, (w - 60, 130), "برج شیطانی", font(96, True), TEXT)
    text_rtl(d, (w - 60, 250), "صعود کن، زنده بمان، ارباب برج را شکست بده", font(34), EMBER_S)
    text_rtl(d, (w - 60, 320), "۱۰۰ طبقه • گرافیک برداری • آفلاین", font(30), MUTED)
    return img


def main():
    os.makedirs(SHOTS, exist_ok=True)
    w, h = 1080, 1920
    shots = {
        "01-main-menu.png": screen_menu,
        "02-gameplay.png": screen_gameplay,
        "03-boss-fight.png": screen_boss,
        "04-shop.png": screen_shop,
        "05-achievements.png": screen_achievements,
        "06-game-over.png": screen_gameover,
    }
    for name, fn in shots.items():
        img = fn(w, h).convert("RGB")
        img.save(os.path.join(SHOTS, name), quality=92)
        print("  screenshots/" + name)

    make_icon(512).convert("RGB").save(os.path.join(OUT, "icon-512.png"))
    print("  icon-512.png")
    make_feature_graphic().convert("RGB").save(os.path.join(OUT, "feature-graphic.png"))
    print("  feature-graphic.png")


if __name__ == "__main__":
    main()
