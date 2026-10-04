from __future__ import annotations

import argparse
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


PETS = {
    "bird": "pet-1.mp4",
    "dog": "pet-dog.mp4",
    "deer": "pet-3.mp4",
}

DENSE_RANGES = {
    "bird": (6.25, 8.05),
    "dog": (0.10, 3.85),
    "deer": (0.10, 1.45),
}

IDLE_TIMES = {
    # Each sequence returns toward its first pose so the one-shot idle animation
    # can rest without a visible pop.
    "bird": (7.19, 7.27, 7.35, 7.50, 7.35, 7.27),
    "dog": (0.10, 0.43, 0.59, 0.75, 0.59, 0.43),
    "deer": (0.39, 0.51, 0.57, 0.69, 0.57, 0.51),
}

CELL_WIDTH = 192
CELL_HEIGHT = 208


def read_frame(video_path: Path, seconds: float) -> Image.Image:
    capture = cv2.VideoCapture(str(video_path))
    capture.set(cv2.CAP_PROP_POS_MSEC, seconds * 1000)
    ok, frame = capture.read()
    capture.release()
    if not ok:
        raise RuntimeError(f"Cannot read {video_path.name} at {seconds:.2f}s")
    return Image.fromarray(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))


def crop_square(image: Image.Image, pet_id: str) -> Image.Image:
    width, height = image.size
    zoom = 1.08 if pet_id == "dog" else 1.0
    crop_size = min(width, height) / zoom
    left = (width - crop_size) / 2
    top = (height - crop_size) / 2
    return image.crop((left, top, left + crop_size, top + crop_size))


def make_contact_sheet(video_path: Path, pet_id: str, output_path: Path) -> None:
    capture = cv2.VideoCapture(str(video_path))
    fps = capture.get(cv2.CAP_PROP_FPS) or 25
    frame_count = capture.get(cv2.CAP_PROP_FRAME_COUNT) or 1
    duration = frame_count / fps
    capture.release()

    count = 16
    start = min(0.15, duration * 0.04)
    end = max(start, min(duration - 0.08, 8.0))
    times = [start + (end - start) * index / (count - 1) for index in range(count)]
    thumb_size = 190
    label_height = 28
    columns = 4
    rows = (count + columns - 1) // columns
    sheet = Image.new("RGB", (columns * thumb_size, rows * (thumb_size + label_height)), "white")
    draw = ImageDraw.Draw(sheet)

    for index, seconds in enumerate(times):
        frame = crop_square(read_frame(video_path, seconds), pet_id).resize((thumb_size, thumb_size), Image.Resampling.LANCZOS)
        x = (index % columns) * thumb_size
        y = (index // columns) * (thumb_size + label_height)
        sheet.paste(frame, (x, y))
        draw.text((x + 7, y + thumb_size + 5), f"{seconds:.2f}s", fill=(25, 35, 35))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output_path, quality=92)


def make_dense_contact_sheet(video_path: Path, pet_id: str, output_path: Path) -> None:
    start, end = DENSE_RANGES[pet_id]
    count = 24
    times = [start + (end - start) * index / (count - 1) for index in range(count)]
    thumb_size = 160
    label_height = 24
    columns = 6
    rows = (count + columns - 1) // columns
    sheet = Image.new("RGB", (columns * thumb_size, rows * (thumb_size + label_height)), "white")
    draw = ImageDraw.Draw(sheet)
    for index, seconds in enumerate(times):
        frame = crop_square(read_frame(video_path, seconds), pet_id).resize((thumb_size, thumb_size), Image.Resampling.LANCZOS)
        x = (index % columns) * thumb_size
        y = (index // columns) * (thumb_size + label_height)
        sheet.paste(frame, (x, y))
        draw.text((x + 5, y + thumb_size + 4), f"{seconds:.2f}s", fill=(25, 35, 35))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output_path, quality=92)


def remove_connected_background(image: Image.Image) -> Image.Image:
    rgb = np.asarray(image.convert("RGB"), dtype=np.uint8)
    _, width, _ = rgb.shape
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0)
    sample_indexes = np.linspace(0, len(border) - 1, 20, dtype=int)
    samples = border[sample_indexes].astype(np.int32)
    pixels = rgb.astype(np.int32)
    distances = np.min(np.sum((pixels[:, :, None, :] - samples[None, None, :, :]) ** 2, axis=3), axis=2)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    saturation = hsv[:, :, 1]
    value = hsv[:, :, 2]
    # Build a solid silhouette from the illustrated outline and coloured areas.
    # Filling the outer contour preserves pale bellies/fur that are visually
    # similar to the paper background and would disappear with chroma-keying.
    subject_core = ((saturation > 28) | (value < 188) | (distances > 74**2)).astype(np.uint8) * 255
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    subject_core = cv2.morphologyEx(subject_core, cv2.MORPH_CLOSE, kernel, iterations=2)
    contours, _ = cv2.findContours(subject_core, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        raise RuntimeError("Could not isolate pet from its background")
    main_contour = max(contours, key=cv2.contourArea)
    alpha = np.zeros(subject_core.shape, dtype=np.uint8)
    cv2.drawContours(alpha, [main_contour], -1, 255, thickness=cv2.FILLED)
    alpha = cv2.GaussianBlur(alpha, (3, 3), 0.65)
    rgba = np.dstack((rgb, alpha))
    rgba[alpha == 0, :3] = 0
    return Image.fromarray(rgba, mode="RGBA")


def make_idle_frame(video_path: Path, pet_id: str, seconds: float) -> Image.Image:
    cell = Image.new("RGBA", (CELL_WIDTH, CELL_HEIGHT), (0, 0, 0, 0))
    source = read_frame(video_path, seconds)
    if pet_id == "dog":
        # The dog source is almost the same aspect ratio as a pet cell; keeping
        # the full frame prevents the ears and paws from being cropped.
        resized = source.resize((184, 200), Image.Resampling.LANCZOS)
        cutout = remove_connected_background(resized)
        cell.alpha_composite(cutout, (4, 8))
    else:
        cropped = crop_square(source, pet_id)
        resized = cropped.resize((CELL_WIDTH, CELL_WIDTH), Image.Resampling.LANCZOS)
        cutout = remove_connected_background(resized)
        cell.alpha_composite(cutout, (0, CELL_HEIGHT - CELL_WIDTH))
    return cell


def checkerboard(size: tuple[int, int], tile: int = 12) -> Image.Image:
    width, height = size
    canvas = Image.new("RGBA", size, (255, 255, 255, 255))
    draw = ImageDraw.Draw(canvas)
    for y in range(0, height, tile):
        for x in range(0, width, tile):
            if (x // tile + y // tile) % 2:
                draw.rectangle((x, y, x + tile - 1, y + tile - 1), fill=(226, 233, 231, 255))
    return canvas


def build_idle_assets(video_path: Path, pet_id: str, pets_dir: Path, qa_dir: Path) -> None:
    frames = [make_idle_frame(video_path, pet_id, seconds) for seconds in IDLE_TIMES[pet_id]]
    sprite = Image.new("RGBA", (CELL_WIDTH * len(frames), CELL_HEIGHT), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        sprite.alpha_composite(frame, (index * CELL_WIDTH, 0))
    sprite.save(pets_dir / f"pet-{pet_id}-idle.webp", format="WEBP", lossless=True, method=6)

    scale = 2
    preview = checkerboard((CELL_WIDTH * len(frames) * scale, CELL_HEIGHT * scale), tile=16)
    for index, frame in enumerate(frames):
        preview.alpha_composite(frame.resize((CELL_WIDTH * scale, CELL_HEIGHT * scale), Image.Resampling.NEAREST), (index * CELL_WIDTH * scale, 0))
    preview.save(qa_dir / f"{pet_id}-idle-contact.png")

    animation_frames = []
    for frame in frames:
        background = checkerboard((CELL_WIDTH * scale, CELL_HEIGHT * scale), tile=16)
        background.alpha_composite(frame.resize((CELL_WIDTH * scale, CELL_HEIGHT * scale), Image.Resampling.LANCZOS))
        animation_frames.append(background.convert("P", palette=Image.Palette.ADAPTIVE))
    animation_frames[0].save(
        qa_dir / f"{pet_id}-idle-preview.gif",
        save_all=True,
        append_images=animation_frames[1:],
        duration=(260, 130, 130, 180, 130, 130),
        loop=0,
        disposal=2,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--contact-sheets", action="store_true")
    parser.add_argument("--dense-contact-sheets", action="store_true")
    parser.add_argument("--build", action="store_true")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[1]
    pets_dir = root / "public" / "pets"
    qa_dir = root / "scripts" / "pet-qa"
    qa_dir.mkdir(parents=True, exist_ok=True)

    if args.contact_sheets:
        for pet_id, filename in PETS.items():
            output = qa_dir / f"{pet_id}-source-contact.jpg"
            make_contact_sheet(pets_dir / filename, pet_id, output)
            print(output.name)
    if args.dense_contact_sheets:
        for pet_id, filename in PETS.items():
            output = qa_dir / f"{pet_id}-dense-contact.jpg"
            make_dense_contact_sheet(pets_dir / filename, pet_id, output)
            print(output.name)
    if args.build:
        for pet_id, filename in PETS.items():
            build_idle_assets(pets_dir / filename, pet_id, pets_dir, qa_dir)
            print(f"pet-{pet_id}-idle.webp")


if __name__ == "__main__":
    main()
