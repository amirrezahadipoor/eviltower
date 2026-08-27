from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parents[1] / 'store-listing'
font_path = Path(__file__).resolve().parents[1] / 'app/src/main/res/font/vazirmatn_bold.ttf'
try: font = ImageFont.truetype(str(font_path), 40)
except: font = ImageFont.load_default()
try: small = ImageFont.truetype(str(font_path), 24)
except: small = font

def screen(title, accent, filename, mode):
    W,H=1080,1920
    im=Image.new('RGB',(W,H),(10,7,18)); d=ImageDraw.Draw(im)
    for y in range(H):
        c=(24+int(y/H*12),14+int(y/H*8),39+int(y/H*15)); d.line((0,y,W,y),fill=c)
    d.rectangle((0,0,W,125),fill=(29,22,43)); d.text((W-60,42), 'برج شیطانی', font=small, fill=(255,209,102), anchor='ra')
    d.text((W-60,180), title, font=font, fill=(255,209,102), anchor='ra')
    if mode=='game':
        pts=[(950,350),(730,350),(600,580),(390,500),(275,800),(490,1020),(740,920),(850,1250),(570,1450),(140,1320)]
        d.line(pts,fill=(80,61,75),width=70,joint='curve'); d.line(pts,fill=(117,83,82),width=42,joint='curve')
        for x,y in [(820,500),(670,730),(480,390),(340,650),(230,1000),(480,1250),(700,1130),(840,1000),(300,1450),(180,700),(560,760),(740,1450)]:
            d.ellipse((x-28,y-28,x+28,y+28),outline=accent,width=6); d.ellipse((x-17,y-17,x+17,y+17),fill=(80,55,95))
        for x,y,c in [(800,345,(150,227,110)),(660,575,(255,91,77)),(560,1010,(112,214,255)),(310,800,(197,155,255)),(550,470,(255,209,102))]:
            d.ellipse((x-18,y-18,x+18,y+18),fill=c); d.ellipse((x-5,y-5,x+5,y+5),fill=(10,7,18))
        d.ellipse((100,1280,185,1365),fill=(105,182,255)); d.ellipse((900,285,1000,385),fill=(90,43,100),outline=(152,227,110),width=7)
        d.rectangle((0,1640,W,H),fill=(29,22,43)); d.text((W-40,1705),'برج آتش     برج یخ     توپخانه     تیرانداز',font=small,fill=(255,209,102),anchor='ra')
    elif mode=='menu':
        d.polygon([(330,1180),(410,500),(540,320),(670,500),(750,1180)],fill=(85,42,83),outline=(255,117,76)); d.ellipse((500,625,580,705),fill=accent); d.ellipse((530,650,550,670),fill=(255,240,210))
        for i,txt in enumerate(['شروع دفاع','فروشگاه','رکوردها','تنظیمات']):
            y=1250+i*110; d.rounded_rectangle((190,y,890,y+76),radius=20,fill=(54,37,68) if i else (255,117,76)); d.text((540,y+38),txt,font=small,fill=(255,244,255),anchor='mm')
    else:
        for i,txt in enumerate(['برج جادوی اهریمنی','سکه‌ی دائمی','دستاوردها','موج ۳۰۰']):
            y=410+i*180; d.rounded_rectangle((100,y,980,y+125),radius=24,fill=(36,27,52),outline=accent,width=3); d.text((930,y+62),txt,font=small,fill=(255,244,255),anchor='ra'); d.text((170,y+62),('باز شده' if i==0 else '+۸۰' if i==1 else 'پیشرفت'),font=small,fill=accent,anchor='la')
    d.text((W-60,1840),'نمونه‌ی تصویری پیشخان — نسخه‌ی داخل برنامه',font=small,fill=(180,160,190),anchor='ra')
    im.save(OUT/'screenshots'/filename,optimize=True)

(OUT/'screenshots').mkdir(exist_ok=True)
screen('شروع دفاع', (255,117,76), '01-main-menu.png','menu')
screen('جاده‌ی هسته را نگه دار', (112,214,255), '02-gameplay.png','game')
screen('ارباب برج بیدار شد', (255,71,111), '03-boss-fight.png','game')
screen('پیشرفت دائمی', (230,140,255), '04-shop.png','shop')
screen('دستاوردها', (255,209,102), '05-achievements.png','shop')
screen('نتیجه‌ی دفاع', (255,117,76), '06-game-over.png','shop')
# square icon and feature graphic
icon=Image.new('RGB',(512,512),(17,11,31)); di=ImageDraw.Draw(icon); di.polygon([(128,430),(175,130),(255,55),(337,130),(390,430)],fill=(125,49,71),outline=(255,117,76)); di.ellipse((205,170,305,270),fill=(255,117,76)); di.ellipse((247,205,266,224),fill=(255,240,210)); icon.save(OUT/'icon-512.png')
fg=Image.new('RGB',(1024,500),(15,9,25)); df=ImageDraw.Draw(fg); df.polygon([(60,450),(180,90),(250,45),(320,90),(430,450)],fill=(85,42,83),outline=(255,117,76)); df.ellipse((210,160,280,230),fill=(255,117,76)); df.text((940,190),'برج شیطانی',font=font,fill=(255,209,102),anchor='ra'); df.text((940,250),'دفاع تا آخرین نفس',font=small,fill=(210,194,220),anchor='ra'); fg.save(OUT/'feature-graphic.png')
