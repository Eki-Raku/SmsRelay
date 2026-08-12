#!/usr/bin/env python3
"""Build Android legacy launcher icons from the generated transparent brand mark."""

from pathlib import Path
import sys

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable-nodpi/smsrelay_brand_mark.png"
RES = ROOT / "app/src/main/res"
SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}
BACKGROUND = (27, 25, 56, 255)


def contain(source: Image.Image, canvas_size: int, content_ratio: float = 0.70) -> Image.Image:
    target = round(canvas_size * content_ratio)
    source = source.copy()
    source.thumbnail((target, target), Image.Resampling.LANCZOS)
    layer = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    x = (canvas_size - source.width) // 2
    y = (canvas_size - source.height) // 2
    layer.alpha_composite(source, (x, y))
    return layer


def render(source: Image.Image, size: int, round_icon: bool) -> Image.Image:
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    if round_icon:
        draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    else:
        radius = round(size * 0.23)
        draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    base = Image.new("RGBA", (size, size), BACKGROUND)
    result.alpha_composite(base)
    result.putalpha(mask)
    result.alpha_composite(contain(source, size))
    return result


def main() -> int:
    if not SOURCE.exists():
        print(f"Missing generated source: {SOURCE}", file=sys.stderr)
        return 1
    source = Image.open(SOURCE).convert("RGBA")
    for density, size in SIZES.items():
        directory = RES / f"mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        render(source, size, round_icon=False).save(directory / "ic_launcher.png")
        render(source, size, round_icon=True).save(directory / "ic_launcher_round.png")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
