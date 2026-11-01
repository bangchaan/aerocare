"""Pembuat aset logo AeroCare. Jalankan: python3 tools/generate_logo.py
Output: src/main/resources/images/*.png dan *.svg
"""
import os
from PIL import Image, ImageDraw, ImageFont

NAVY, TEAL, WHITE = (20, 43, 69, 255), (22, 166, 161, 255), (255, 255, 255, 255)
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "images")
FONT = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"
FONT_SUB = "/usr/share/fonts/truetype/google-fonts/Poppins-Regular.ttf"
SS = 4  # supersampling


def round_line(d, p1, p2, w, fill):
    d.line([p1, p2], fill=fill, width=int(w))
    r = w / 2
    for x, y in (p1, p2):
        d.ellipse([x - r, y - r, x + r, y + r], fill=fill)


def curve(d, p0, c, p1, w, fill, n=48):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t * t * p1[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t * t * p1[1]
        pts.append((x, y))
    for a, b in zip(pts, pts[1:]):
        round_line(d, a, b, w, fill)


def glyph(d, ox, oy, s, square=True, leg_left=WHITE):
    """Gambar simbol pada kotak 64x64 unit, skala s piksel per unit."""
    P = lambda x, y: (ox + x * s, oy + y * s)
    if square:
        d.rounded_rectangle([ox, oy, ox + 64 * s, oy + 64 * s], radius=14 * s, fill=NAVY)
    round_line(d, P(14, 50), P(30, 14), 5 * s, leg_left)
    round_line(d, P(34, 14), P(50, 50), 5 * s, TEAL)
    curve(d, P(22, 38), P(32, 31), P(42, 38), 3.5 * s, leg_left)
    cx, cy = P(32, 12)
    ro = 6.0 * s
    d.ellipse([cx - ro, cy - ro, cx + ro, cy + ro], outline=TEAL, width=int(3 * s))
    round_line(d, P(48, 30), P(58, 25), 3 * s, TEAL)
    round_line(d, P(52, 40), P(60, 36), 3 * s, TEAL)


def finish(img, path, size=None):
    if size:
        img = img.resize(size, Image.LANCZOS)
    else:
        img = img.resize((img.width // SS, img.height // SS), Image.LANCZOS)
    img.save(os.path.join(OUT, path))


def wordmark(d, x, y, size, aero_color, font_path=FONT):
    f = ImageFont.truetype(font_path, size)
    d.text((x, y), "Aero", font=f, fill=aero_color)
    w = d.textlength("Aero", font=f)
    d.text((x + w, y), "Care", font=f, fill=TEAL)
    return w + d.textlength("Care", font=f)


def icon_png(name, px, square=True):
    img = Image.new("RGBA", (px * SS, px * SS), (0, 0, 0, 0))
    glyph(ImageDraw.Draw(img), 0, 0, px * SS / 64, square=square)
    finish(img, name)


def horizontal(name, aero_color, height=160):
    icon = height * 0.8
    f = ImageFont.truetype(FONT, int(height * 0.52 * SS))
    tmp = ImageDraw.Draw(Image.new("RGBA", (10, 10)))
    tw = tmp.textlength("AeroCare", font=f)
    gap = height * 0.18 * SS
    W = int(icon * SS + gap + tw + height * 0.2 * SS)
    img = Image.new("RGBA", (W, height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    glyph(d, 0, (height * SS - icon * SS) / 2, icon * SS / 64, square=True)
    ty = (height * SS - f.size) / 2 - f.size * 0.12
    wordmark(d, icon * SS + gap, ty, f.size, aero_color)
    finish(img, name)


def primary(name, aero_color, w=640, h=640):
    img = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    icon = w * 0.42
    glyph(d, (w - icon) / 2 * SS, h * 0.08 * SS, icon * SS / 64, square=True)
    f = ImageFont.truetype(FONT, int(w * 0.16 * SS))
    tw = d.textlength("AeroCare", font=f)
    wordmark(d, (w * SS - tw) / 2, h * 0.58 * SS, f.size, aero_color)
    fs = ImageFont.truetype(FONT_SUB, int(w * 0.04 * SS))
    sub = "AIRPORT FACILITY MANAGEMENT SYSTEM"
    sw = d.textlength(sub, font=fs)
    d.text(((w * SS - sw) / 2, h * 0.82 * SS), sub, font=fs, fill=(91, 107, 127, 255))
    finish(img, name)


SVG_GLYPH = (
    '<path d="M14 50 L30 14" stroke="{l}" stroke-width="5" stroke-linecap="round" fill="none"/>'
    '<path d="M34 14 L50 50" stroke="#16A6A1" stroke-width="5" stroke-linecap="round" fill="none"/>'
    '<path d="M22 38 Q32 31 42 38" stroke="{l}" stroke-width="3.5" stroke-linecap="round" fill="none"/>'
    '<circle cx="32" cy="12" r="4.5" fill="none" stroke="#16A6A1" stroke-width="3"/>'
    '<path d="M48 30 L58 25 M52 40 L60 36" stroke="#16A6A1" stroke-width="3" stroke-linecap="round" fill="none"/>'
)
SQ = '<rect width="64" height="64" rx="14" fill="#142B45"/>'
FF = "font-family=\"Poppins,'Segoe UI',Arial,sans-serif\" font-weight=\"700\""


def write(name, text):
    with open(os.path.join(OUT, name), "w", encoding="utf-8") as fh:
        fh.write(text)


def svgs():
    write("logo-icon.svg",
          f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">{SQ}{SVG_GLYPH.format(l="#FFFFFF")}</svg>')
    write("logo-icon-transparent.svg",
          f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">{SVG_GLYPH.format(l="#FFFFFF")}</svg>')
    for name, c in (("logo-horizontal.svg", "#142B45"), ("logo-horizontal-white.svg", "#FFFFFF")):
        write(name,
              f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 330 64"><g>{SQ}{SVG_GLYPH.format(l="#FFFFFF")}</g>'
              f'<text x="80" y="45" font-size="34" {FF} fill="{c}">Aero<tspan fill="#16A6A1">Care</tspan></text></svg>')
    write("logo-primary.svg",
          f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 300"><g transform="translate(112,10) scale(1.5)">{SQ}{SVG_GLYPH.format(l="#FFFFFF")}</g>'
          f'<text x="160" y="186" text-anchor="middle" font-size="44" {FF} fill="#142B45">Aero<tspan fill="#16A6A1">Care</tspan></text>'
          f'<text x="160" y="214" text-anchor="middle" font-size="11" letter-spacing="1.4" font-family="Poppins,Arial,sans-serif" fill="#5B6B7F">AIRPORT FACILITY MANAGEMENT SYSTEM</text></svg>')


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    icon_png("logo-icon.png", 512)                       # Icon Logo
    icon_png("logo-icon-transparent.png", 512, False)    # simbol tanpa kotak (latar navy)
    icon_png("app-icon-256.png", 256)
    icon_png("app-icon-64.png", 64)
    horizontal("logo-horizontal.png", NAVY)              # header & login (latar terang)
    horizontal("logo-horizontal-white.png", WHITE)       # sidebar navy
    primary("logo-primary.png", NAVY)                    # Primary Logo
    svgs()
    print("OK", sorted(os.listdir(OUT)))
