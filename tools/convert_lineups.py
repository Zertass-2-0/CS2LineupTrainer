"""
Конвертер лайнапов: сырые скриншоты + текстовый файл -> картинки и Kotlin-код для приложения.

Что делает для каждого лайнапа:
  1. Берёт скриншот, на котором прицел наведён строго в нужную точку (в центре кадра).
  2. Сохраняет две картинки в app/src/main/res/drawable-nodpi/:
       gen_<id>_stand.jpg  - целый кадр БЕЗ прицела. Для тренировки: приложение само показывает его половину
                             со случайным сдвигом на каждом вопросе
       gen_<id>_howto.jpg  - кадр для туториала: центральная половина с нарисованным прицелом в центре
  4. Создаёт Kotlin-файл CacheGenerated.kt со всеми лайнапами.

Запуск (из корня проекта):
    python tools/convert_lineups.py            # сконвертировать
    python tools/convert_lineups.py --check    # только проверить пак, ничего не записывать

Нужна библиотека Pillow:  pip install pillow
Подробности формата - в CONTENT_GUIDE.md.
"""

import argparse
import re
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw
except ImportError:
    sys.exit(
        "Не установлена библиотека Pillow для этого Python.\n"
        f"Установите её командой:\n    \"{sys.executable}\" -m pip install pillow\n"
        "и запустите скрипт снова."
    )

# ---------- Настройки ----------

CROP = 0.5            # во сколько раз уменьшаем кадр по каждой стороне (0.5 = обрезать вдвое)
MAX_OUT_WIDTH = 1280  # если кадр после обрезки шире, он уменьшается до этой ширины
MIN_ZOOM, MAX_ZOOM = 1.0, 5.0  # допустимое увеличение кадра в тренировке (поле zoom)
MAX_STAND_WIDTH = 2560  # целый кадр для тренировки уменьшается до этой ширины, если он больше (запас чёткости для большого зума)
JPEG_QUALITY = 88
GEN_PREFIX = "gen_"   # все создаваемые скриптом картинки начинаются с этого, остальные файлы он не трогает

# Допустимые значения в txt -> имена в Kotlin
GRENADES = {"smoke": "SMOKE", "flash": "FLASH", "molotov": "MOLOTOV", "he": "HE"}
SIDES = {"t": "T", "ct": "CT"}
AREAS = {"a": "A", "b": "B", "mid": "MID"}
THROWS = {
    "lmb": "LMB", "rmb": "RMB", "lmb_rmb": "LMB_RMB",
    "jump_throw": "JUMP_THROW", "run_jump_throw": "RUN_JUMP_THROW", "w_lmb": "W_LMB",
}

REQUIRED_KEYS = ["grenade", "side", "area", "title", "throw", "origin", "target"]
OPTIONAL_KEYS = ["description", "tolerance", "zoom"]
ID_PATTERN = re.compile(r"^[a-z][a-z0-9_]*$")
NUMBER = re.compile(r"-?\d+(?:\.\d+)?")


class ContentError(Exception):
    """Ошибка в данных пака. Текст показывается пользователю."""


# ---------- Разбор lineups.txt ----------

def parse_blocks(path):
    """Читает файл и возвращает список блоков [{'id', 'line', 'fields': {ключ: (значение, номер строки)}}]."""
    blocks = []
    current = None
    errors = []
    for number, raw in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("[") and line.endswith("]"):
            current = {"id": line[1:-1].strip(), "line": number, "fields": {}}
            blocks.append(current)
            continue
        if current is None:
            errors.append(f"строка {number}: запись вне блока. Блок начинается с [id_лайнапа]")
            continue
        if "=" not in line:
            errors.append(f"строка {number}: ожидалось 'ключ = значение', а найдено '{line}'")
            continue
        key, value = (part.strip() for part in line.split("=", 1))
        key = key.lower()
        if key not in REQUIRED_KEYS + OPTIONAL_KEYS:
            errors.append(f"строка {number}: неизвестный ключ '{key}'. Допустимые: {', '.join(REQUIRED_KEYS + OPTIONAL_KEYS)}")
        elif key in current["fields"]:
            errors.append(f"строка {number}: ключ '{key}' указан в блоке [{current['id']}] дважды")
        else:
            current["fields"][key] = (value, number)
    return blocks, errors


def parse_coordinates(text, line):
    """Достаёт X и Y из 'setpos 1 2 3;setang ...' или просто '1 2'."""
    before_semicolon = text.split(";")[0]
    numbers = [float(n) for n in NUMBER.findall(before_semicolon)]
    if len(numbers) < 2:
        raise ContentError(f"строка {line}: в '{text}' не нашлось двух чисел X и Y")
    return numbers[0], numbers[1]


def parse_point(value, line, registry):
    """
    Точка записывается как 'id | Название | координаты' при первом использовании
    и просто 'id' при повторных. registry хранит уже описанные точки.
    """
    parts = [p.strip() for p in value.split("|")]
    point_id = parts[0]
    if not ID_PATTERN.match(point_id):
        raise ContentError(f"строка {line}: id точки '{point_id}' должен состоять из строчных латинских букв, цифр и _")

    if len(parts) == 1:
        if point_id not in registry:
            raise ContentError(
                f"строка {line}: точка '{point_id}' ещё не описана. "
                f"При первом использовании пишите: {point_id} | Название | setpos X Y Z"
            )
        return point_id
    if len(parts) != 3 or not parts[1]:
        raise ContentError(f"строка {line}: точка должна быть записана как 'id | Название | координаты' или просто 'id'")

    x, y = parse_coordinates(parts[2], line)
    if point_id in registry:
        _, old_x, old_y = registry[point_id]
        if abs(old_x - x) > 0.5 or abs(old_y - y) > 0.5:
            raise ContentError(
                f"строка {line}: точка '{point_id}' уже описана с другими координатами "
                f"({old_x}, {old_y}). Для повторного использования пишите только '{point_id}'"
            )
    else:
        registry[point_id] = (parts[1], x, y)
    return point_id


def parse_floats(value, line, count, name):
    numbers = NUMBER.findall(value)
    if len(numbers) != count:
        hint = " Дробные числа пишите через точку, например 2.5." if "," in value else ""
        raise ContentError(f"строка {line}: '{name}' должен содержать {count} числа(о), а найдено: '{value}'.{hint}")
    return [float(n) for n in numbers]


def build_lineup(block, registry):
    """Проверяет блок и превращает его в словарь с готовыми значениями."""
    fields = block["fields"]
    lineup_id = block["id"]
    if not ID_PATTERN.match(lineup_id):
        raise ContentError(f"строка {block['line']}: id лайнапа '{lineup_id}' должен состоять из строчных латинских букв, цифр и _")
    missing = [k for k in REQUIRED_KEYS if k not in fields]
    if missing:
        raise ContentError(f"[{lineup_id}] (строка {block['line']}): не хватает ключей: {', '.join(missing)}")

    def choice(key, options):
        value, line = fields[key]
        if value.lower() not in options:
            raise ContentError(f"строка {line}: '{key} = {value}'. Допустимо: {', '.join(options)}")
        return options[value.lower()]

    lineup = {
        "id": lineup_id,
        "grenade": choice("grenade", GRENADES),
        "side": choice("side", SIDES),
        "area": choice("area", AREAS),
        "throw": choice("throw", THROWS),
        "title": fields["title"][0],
        "description": fields["description"][0] if "description" in fields else "",
        "origin": parse_point(*fields["origin"], registry),
        "target": parse_point(*fields["target"], registry),
        "aim": (0.5, 0.5),  # прицел наведён в центр исходного скриншота
        "tolerance": None,
        "zoom": None,
    }
    if "tolerance" in fields:
        value, line = fields["tolerance"]
        (lineup["tolerance"],) = parse_floats(value, line, 1, "tolerance")
    if "zoom" in fields:
        value, line = fields["zoom"]
        (zoom,) = parse_floats(value, line, 1, "zoom")
        if not (MIN_ZOOM <= zoom <= MAX_ZOOM):
            raise ContentError(f"строка {line}: zoom должен быть от {MIN_ZOOM:g} до {MAX_ZOOM:g}, а не '{value}'")
        lineup["zoom"] = zoom
    return lineup


# ---------- Картинки ----------

def find_screenshot(screens_dir, lineup_id):
    for extension in (".png", ".jpg", ".jpeg", ".PNG", ".JPG", ".JPEG"):
        candidate = screens_dir / f"{lineup_id}{extension}"
        if candidate.exists():
            return candidate
    raise ContentError(f"[{lineup_id}]: не найден скриншот. Положите его в {screens_dir} как {lineup_id}.png (или .jpg)")


def to_16_by_9(image):
    """Если скриншот не 16:9, аккуратно подрезает его по центру."""
    width, height = image.size
    target_height = round(width * 9 / 16)
    if abs(target_height - height) <= 2:
        return image
    if target_height < height:
        top = (height - target_height) // 2
        return image.crop((0, top, width, top + target_height))
    target_width = round(height * 16 / 9)
    left = (width - target_width) // 2
    return image.crop((left, 0, left + target_width, height))


def center_crop_box(width, height):
    """Рамка обрезки по центру: центральная часть кадра размером CROP по каждой стороне."""
    crop_w, crop_h = round(width * CROP), round(height * CROP)
    left, top = (width - crop_w) // 2, (height - crop_h) // 2
    return left, top, left + crop_w, top + crop_h


def draw_crosshair(image):
    """Рисует прицел в центре: четыре короткие черты, зелёные с чёрной обводкой. Центр остаётся пустым, чтобы было видно ориентир."""
    width, height = image.size
    cx, cy = width / 2, height / 2
    arm, gap = width * 0.016, width * 0.009
    thick = max(2, round(width * 0.003))
    outline = max(1, thick // 2)

    rects = []
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        x0, x1 = cx + dx * gap, cx + dx * (gap + arm)
        y0, y1 = cy + dy * gap, cy + dy * (gap + arm)
        if dx:
            rects.append((min(x0, x1), cy - thick / 2, max(x0, x1), cy + thick / 2))
        else:
            rects.append((cx - thick / 2, min(y0, y1), cx + thick / 2, max(y0, y1)))

    draw = ImageDraw.Draw(image)
    for x0, y0, x1, y1 in rects:  # сначала все обводки, чтобы они не перекрывали соседние зелёные части
        draw.rectangle((x0 - outline, y0 - outline, x1 + outline, y1 + outline), fill=(0, 0, 0))
    for rect in rects:
        draw.rectangle(rect, fill=(0, 255, 0))


def limit_width(image, max_width):
    """Уменьшает слишком большой кадр."""
    if image.width > max_width:
        height = round(image.height * max_width / image.width)
        image = image.resize((max_width, height), Image.LANCZOS)
    return image


def make_images(source_path, lineup):
    """Возвращает (целый кадр для тренировки, кадр для туториала с прицелом)."""
    original = Image.open(source_path).convert("RGB")
    image = to_16_by_9(original)
    if image.size != original.size:
        print(f"Предупреждение: скриншот {source_path.name} не 16:9 ({original.width}x{original.height}), он подрезан по центру")
    width, height = image.size

    stand = limit_width(image, MAX_STAND_WIDTH)
    howto = limit_width(image.crop(center_crop_box(width, height)), MAX_OUT_WIDTH)
    draw_crosshair(howto)
    return stand, howto


# ---------- Kotlin ----------

def kotlin_string(text):
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def kotlin_float(value):
    return f"{float(value)!r}f"


def generate_kotlin(lineups, registry):
    used = []
    for lineup in lineups:
        for key in ("origin", "target"):
            if lineup[key] not in used:
                used.append(lineup[key])

    lines = [
        "package com.example.cs2lineuptrainer.data",
        "",
        "import com.example.cs2lineuptrainer.R",
        "import com.example.cs2lineuptrainer.domain.Area",
        "import com.example.cs2lineuptrainer.domain.GrenadeType",
        "import com.example.cs2lineuptrainer.domain.Lineup",
        "import com.example.cs2lineuptrainer.domain.LineupMedia",
        "import com.example.cs2lineuptrainer.domain.Offset01",
        "import com.example.cs2lineuptrainer.domain.Side",
        "import com.example.cs2lineuptrainer.domain.ThrowType",
        "",
        "// Этот файл создаёт скрипт tools/convert_lineups.py. Вручную не править.",
        "object CacheGenerated {",
    ]
    if used:
        lines.append("    // Каждая точка описана один раз, лайнапы ссылаются на неё.")
    for point_id in used:
        name, x, y = registry[point_id]
        lines.append(f"    private val p_{point_id} = cachePoint({kotlin_string(point_id)}, {kotlin_string(name)}, {kotlin_float(x)}, {kotlin_float(y)})")
    if not lineups:
        lines.append("    val lineups: List<Lineup> = emptyList()")
    else:
        lines += ["", "    val lineups: List<Lineup> = listOf("]
        for item in lineups:
            tolerance = f"            tolerance = {kotlin_float(item['tolerance'])},\n" if item["tolerance"] is not None else ""
            zoom = f"            zoom = {kotlin_float(item['zoom'])},\n" if item["zoom"] is not None else ""
            lines.append(
                "        Lineup(\n"
                f"            id = {kotlin_string(item['id'])},\n"
                f"            mapId = CacheData.MAP_ID,\n"
                f"            grenade = GrenadeType.{item['grenade']},\n"
                f"            side = Side.{item['side']},\n"
                f"            area = Area.{item['area']},\n"
                f"            title = {kotlin_string(item['title'])},\n"
                f"            target = p_{item['target']},\n"
                f"            origin = p_{item['origin']},\n"
                f"            standView = LineupMedia.Image(R.drawable.{GEN_PREFIX}{item['id']}_stand),\n"
                f"            aimPoint = Offset01({kotlin_float(item['aim'][0])}, {kotlin_float(item['aim'][1])}),\n"
                f"{tolerance}"
                f"{zoom}"
                f"            throwType = ThrowType.{item['throw']},\n"
                f"            howTo = LineupMedia.{item['howto_kind']}(R.drawable.{GEN_PREFIX}{item['id']}_howto),\n"
                f"            description = {kotlin_string(item['description'])},\n"
                "        ),"
            )
        lines.append("    )")
    lines += ["}", ""]
    return "\n".join(lines)


# ---------- Основная часть ----------

def main():
    # Чтобы русский текст одинаково выводился в любой консоли.
    for stream in (sys.stdout, sys.stderr):
        stream.reconfigure(encoding="utf-8", errors="replace")

    root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description="Конвертер лайнапов для CS2LineupTrainer")
    parser.add_argument("--check", action="store_true", help="только проверить пак, ничего не записывать")
    parser.add_argument("--content", type=Path, default=root / "content", help="папка с lineups.txt и screens/")
    parser.add_argument("--res", type=Path, default=root / "app/src/main/res/drawable-nodpi", help="куда класть картинки")
    parser.add_argument("--kotlin", type=Path,
                        default=root / "app/src/main/java/com/example/cs2lineuptrainer/data/CacheGenerated.kt",
                        help="куда записать Kotlin-файл")
    args = parser.parse_args()

    txt_path = args.content / "lineups.txt"
    screens_dir = args.content / "screens"
    if not txt_path.exists():
        sys.exit(f"Не найден файл {txt_path}")

    blocks, errors = parse_blocks(txt_path)
    registry, lineups, images = {}, [], {}
    seen_ids = set()

    for block in blocks:
        try:
            if block["id"] in seen_ids:
                raise ContentError(f"строка {block['line']}: лайнап [{block['id']}] описан дважды")
            seen_ids.add(block["id"])
            lineup = build_lineup(block, registry)
            screenshot = find_screenshot(screens_dir, lineup["id"])
            images[lineup["id"]] = make_images(screenshot, lineup)

            gif = screens_dir / f"{lineup['id']}_howto.gif"
            lineup["howto_kind"] = "Gif" if gif.exists() else "Image"
            lineup["howto_gif"] = gif if gif.exists() else None
            lineups.append(lineup)
        except ContentError as error:
            errors.append(str(error))
        except OSError as error:
            errors.append(f"[{block['id']}]: не удалось открыть изображение: {error}")

    known = {b["id"] for b in blocks}
    for file in sorted(screens_dir.glob("*")) if screens_dir.exists() else []:
        stem = file.stem[:-len("_howto")] if file.stem.endswith("_howto") else file.stem
        if stem not in known and file.suffix.lower() in (".png", ".jpg", ".jpeg", ".gif"):
            print(f"Предупреждение: файл {file.name} не относится ни к одному лайнапу из lineups.txt (проверьте имя)")

    if errors:
        print(f"\nНайдено ошибок: {len(errors)}. Ничего не записано.\n")
        for error in errors:
            print(" -", error)
        sys.exit(1)

    print(f"Проверено лайнапов: {len(lineups)}, точек: {len(registry)}.")
    if args.check:
        print("Режим --check: файлы не записывались.")
        return

    args.res.mkdir(parents=True, exist_ok=True)
    for old in args.res.glob(f"{GEN_PREFIX}*"):
        old.unlink()
    for lineup in lineups:
        stand, howto = images[lineup["id"]]
        stand.save(args.res / f"{GEN_PREFIX}{lineup['id']}_stand.jpg", quality=JPEG_QUALITY, optimize=True)
        if lineup["howto_gif"]:
            (args.res / f"{GEN_PREFIX}{lineup['id']}_howto.gif").write_bytes(lineup["howto_gif"].read_bytes())
        else:
            howto.save(args.res / f"{GEN_PREFIX}{lineup['id']}_howto.jpg", quality=JPEG_QUALITY, optimize=True)

    args.kotlin.parent.mkdir(parents=True, exist_ok=True)
    args.kotlin.write_text(generate_kotlin(lineups, registry), encoding="utf-8")
    print(f"Картинки записаны в {args.res}")
    print(f"Kotlin-файл записан: {args.kotlin}")
    print("Теперь в Android Studio: Build -> Make Project, затем запускайте приложение.")


if __name__ == "__main__":
    main()
