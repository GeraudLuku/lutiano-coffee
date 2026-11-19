"""Draws the LUTIANO mark (gold coffee bean) and writes the app icons, splash images and in-app logo.
Run from apps/seller-app:  python3 tool/make_brand_assets.py [app folder, e.g. ../customer-app]   (needs Pillow; the design pack holds no standalone logo file, so the
bean is redrawn from the Batch 01 sign-in board: a gold disc with a curved dark slash, with the LUTIANO wordmark below it)."""
import json, math, os, sys
from PIL import Image, ImageDraw, ImageFont

NAVY = (7, 26, 53)          # AppColors.navyDeep, the sign-in screen background
GOLD_TOP, GOLD_BOTTOM = (255, 197, 28), (240, 168, 0)
S = 4                       # supersampling
ROOT = sys.argv[1] if len(sys.argv) > 1 else "."   # the app folder (apps/seller-app or apps/customer-app)


def bezier(p0, p1, p2, p3, n=400):
    for i in range(n + 1):
        t = i / n
        a, b, c, d = (1 - t) ** 3, 3 * t * (1 - t) ** 2, 3 * t * t * (1 - t), t ** 3
        yield (a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0], a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1])


def bean(size, bg=None):
    """The bean filling a size x size square (transparent unless bg is given)."""
    big = size * S
    img = Image.new("RGBA", (big, big), bg or (0, 0, 0, 0))
    r = big / 2
    # disc with a soft vertical gradient
    disc = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    grad = Image.new("RGBA", (big, big))
    gd = ImageDraw.Draw(grad)
    for y in range(big):
        t = y / big
        gd.line([(0, y), (big, y)], fill=tuple(int(GOLD_TOP[i] + (GOLD_BOTTOM[i] - GOLD_TOP[i]) * t) for i in range(3)) + (255,))
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, big - 1, big - 1], fill=255)
    disc.paste(grad, (0, 0), mask)
    # the slash: a tapered S curve from top right to bottom left, in the background colour
    slash = Image.new("L", (big, big), 0)
    sd = ImageDraw.Draw(slash)
    pts = list(bezier((0.80, -0.52), (0.05, -0.38), (0.28, 0.30), (-0.34, 0.98)))
    for i, (x, y) in enumerate(pts):
        t = i / (len(pts) - 1)
        w = r * (0.035 + 0.075 * math.sin(math.pi * t))
        cx, cy = r + x * r, r + y * r
        sd.ellipse([cx - w, cy - w, cx + w, cy + w], fill=255)
    disc.paste(Image.new("RGBA", (big, big), NAVY + (255,)), (0, 0), Image.composite(slash, Image.new("L", (big, big), 0), mask))
    img.alpha_composite(disc)
    return img.resize((size, size), Image.LANCZOS)


def font(size):
    return ImageFont.truetype("/System/Library/Fonts/Supplemental/Futura.ttc", size, index=2)  # Futura Medium


def wordmark(width):
    """LUTIANO in spaced capitals, white on transparent, `width` pixels wide."""
    text, spacing = "LUTIANO", 0.22
    size = 100
    f = font(size)
    widths = [f.getlength(c) for c in text]
    total = sum(widths) + spacing * size * (len(text) - 1)
    img = Image.new("RGBA", (int(total) + 20, int(size * 1.4)), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x = 10
    for c, w in zip(text, widths):
        d.text((x, 0), c, font=f, fill=(255, 255, 255, 255))
        x += w + spacing * size
    img = img.crop(img.getbbox())
    return img.resize((width, int(img.height * width / img.width)), Image.LANCZOS)


def splash(scale):
    """Bean over the wordmark, transparent, on a 168 x 185 pt canvas (iOS launch image)."""
    w, h = 168 * scale, 185 * scale
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    b = bean(int(84 * scale))
    img.alpha_composite(b, ((w - b.width) // 2, int(22 * scale)))
    m = wordmark(int(132 * scale))
    img.alpha_composite(m, ((w - m.width) // 2, int(22 * scale) + b.height + int(20 * scale)))
    return img


def save(img, path, flat=False):
    path = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if flat:
        bg = Image.new("RGB", img.size, NAVY)
        bg.paste(img, mask=img.split()[3])
        img = bg
    img.save(path)


def icon(size, fill=0.60, rounded=0, circle=False):
    """Bean centred on the navy square (or rounded square / circle for legacy Android icons)."""
    base = Image.new("RGBA", (size, size), NAVY + (255,))
    b = bean(int(size * fill))
    base.alpha_composite(b, ((size - b.width) // 2, (size - b.height) // 2))
    if rounded or circle:
        m = Image.new("L", (size * S, size * S), 0)
        d = ImageDraw.Draw(m)
        box = [0, 0, size * S - 1, size * S - 1]
        d.ellipse(box, fill=255) if circle else d.rounded_rectangle(box, radius=int(size * S * rounded), fill=255)
        m = m.resize((size, size), Image.LANCZOS)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        out.paste(base, (0, 0), m)
        return out
    return base


# in-app logo (the sign-in and About screens)
if os.path.basename(os.path.abspath(ROOT)) == "seller-app":
    save(bean(512), "../../packages/lutiano_core/assets/bean.png")

# iOS app icons, from the sizes the asset catalogue asks for (no transparency allowed)
cat = "ios/Runner/Assets.xcassets/AppIcon.appiconset"
for im in json.load(open(os.path.join(ROOT, cat, "Contents.json")))["images"]:
    px = round(float(im["size"].split("x")[0]) * int(im["scale"][0]))
    save(icon(px, 0.60).convert("RGB"), f"{cat}/{im['filename']}")

# iOS launch image: bean and wordmark; the storyboard supplies the navy background
for name, sc in (("LaunchImage.png", 1), ("LaunchImage@2x.png", 2), ("LaunchImage@3x.png", 3)):
    save(splash(sc), f"ios/Runner/Assets.xcassets/LaunchImage.imageset/{name}")

# Android: legacy icons, adaptive foreground (bean inside the 66 dp safe zone of a 108 dp canvas), splash
res = "android/app/src/main/res"
for d, px, fg in (("mdpi", 48, 108), ("hdpi", 72, 162), ("xhdpi", 96, 216), ("xxhdpi", 144, 324), ("xxxhdpi", 192, 432)):
    save(icon(px, 0.60, rounded=0.18), f"{res}/mipmap-{d}/ic_launcher.png")
    save(icon(px, 0.60, circle=True), f"{res}/mipmap-{d}/ic_launcher_round.png")
    layer = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
    b = bean(int(fg * 0.40))
    layer.alpha_composite(b, ((fg - b.width) // 2, (fg - b.height) // 2))
    save(layer, f"{res}/mipmap-{d}/ic_launcher_foreground.png")
    save(splash(1).resize((int(168 * fg / 108 * 0.5), int(185 * fg / 108 * 0.5)), Image.LANCZOS), f"{res}/drawable-{d}/launch_logo.png")
print("done")
