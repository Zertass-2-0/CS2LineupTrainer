"""
Конвертер лайнапов: сырые скриншоты + текстовый файл -> картинки и Kotlin-код для приложения.

Что делает для каждого лайнапа:
  1. Берёт скриншот <id>.png, на котором прицел наведён строго в нужную точку (в центре кадра), без худа.
  2. Сохраняет две картинки в app/src/main/res/drawable-nodpi/:
       gen_<карта>_<id>_stand.jpg  - целый кадр БЕЗ прицела. Для тренировки: приложение само показывает его половину
                             со случайным сдвигом на каждом вопросе
       gen_<карта>_<id>_howto.jpg  - кадр для туториала (поиск). Откуда берётся, по приоритету:
                               a) <id>_howto.gif                - GIF копируется как есть;
                               b) <id>_howto.png / .jpg         - скриншот С худом и прицелом из игры: берётся
                                                                  его центральная часть, прицел не рисуется;
                               c) ничего из этого не лежит      - центральная часть чистого скриншота,
                                                                  прицел рисует скрипт
                             Центральная часть по умолчанию в 2 раза меньше кадра; необязательный ключ
                             howto_zoom в lineups.txt (1-5, как zoom) меняет его, 1 = кадр целиком
  3. Создаёт Kotlin-файл data/<Карта>Generated.kt со всеми лайнапами карты.

Карт может быть несколько: у каждой своя папка content/<карта>/ (например content/cache/, content/mirage/).
Новая карта: создайте папку с названием (строчные латинские буквы и цифры) и положите в неё radar.png (квадратный).
Скрипт сам создаст недостающее: screens/, lineups.txt с тестовым лайнапом <карта>_testlineup (смок под ноги в центре карты,
белый скриншот), файл data/<Карта>Data.kt с временной калибровкой. Радар он сжимает до 1536 px и кладёт в res как <карта>_radar.png.
Скрипт обрабатывает все карты сразу, для каждой создаёт data/<Карта>Generated.kt, а также data/GeneratedMaps.kt (список карт).
Id лайнапов в lineups.txt (и имена скриншотов) должны быть уникальными только в пределах карты, а в приложении
id лайнапа становится <карта>_<id> (например cache_smoke_bmain_heaven), поэтому на разных картах id могут совпадать.

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

DEFAULT_HOWTO_ZOOM = 2.0  # во сколько раз кадр туториала (Поиск) крупнее целого кадра: 2 = центральная половина
MAX_OUT_WIDTH = 1280  # если кадр после обрезки шире, он уменьшается до этой ширины
MIN_ZOOM, MAX_ZOOM = 1.0, 5.0  # допустимое увеличение кадра (поля zoom и howto_zoom)
MAX_STAND_WIDTH = 2560  # целый кадр для тренировки уменьшается до этой ширины, если он больше (запас чёткости для большого зума)
JPEG_QUALITY = 88
GEN_PREFIX = "gen_"   # все создаваемые скриптом картинки начинаются с этого, остальные файлы он не трогает

# Допустимые значения в txt -> имена в Kotlin
GRENADES = {"smoke": "SMOKE", "flash": "FLASH", "molotov": "MOLOTOV", "he": "HE"}
SIDES = {"t": "T", "ct": "CT"}
AREAS = {"a": "A", "b": "B", "mid": "MID"}
# Способ броска записывается через плюс из четырёх независимых частей, например: ctrl + forward_run + jump + lmb
THROW_MODIFIERS = {"shift": "SHIFT", "ctrl": "CTRL"}  # не указано = клавиша не зажата
THROW_MOVEMENTS = {  # не указано = стоя
    f"{direction}_{length}": f"{direction.upper()}_{length.upper()}"
    for direction in ("forward", "back", "left", "right")
    for length in ("step", "run")
}
THROW_JUMP = "jump"  # не указано = без прыжка
THROW_BUTTONS = {"lmb": "LMB", "rmb": "RMB", "lmb_rmb": "LMB_RMB"}  # обязательно ровно одна

REQUIRED_KEYS = ["grenade", "side", "area", "title", "throw", "origin", "target"]
OPTIONAL_KEYS = ["description", "tolerance", "zoom", "howto_zoom"]
ID_PATTERN = re.compile(r"^[a-z][a-z0-9_]*$")
MAP_PATTERN = re.compile(r"^[a-z][a-z0-9]*$")  # имя папки карты: строчные латинские буквы и цифры
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


def parse_throw(value, line):
    """
    Разбирает 'ctrl + forward_run + jump + lmb' в {'modifier', 'movement', 'jump', 'buttons'}.
    Порядок частей любой; клавиша, движение и прыжок необязательны, кнопки мыши обязательны.
    """
    result = {"modifier": "NONE", "movement": "STAND", "jump": False, "buttons": None}
    seen = set()
    for token in (t.strip().lower() for t in value.split("+")):
        if token in THROW_MODIFIERS:
            part, key, new_value = "клавиша (shift / ctrl)", "modifier", THROW_MODIFIERS[token]
        elif token in THROW_MOVEMENTS:
            part, key, new_value = "движение", "movement", THROW_MOVEMENTS[token]
        elif token == THROW_JUMP:
            part, key, new_value = "прыжок", "jump", True
        elif token in THROW_BUTTONS:
            part, key, new_value = "кнопки мыши", "buttons", THROW_BUTTONS[token]
        else:
            raise ContentError(
                f"строка {line}: в 'throw' неизвестная часть '{token}'. Допустимо: "
                f"{', '.join(THROW_MODIFIERS)}; {', '.join(THROW_MOVEMENTS)}; {THROW_JUMP}; {', '.join(THROW_BUTTONS)}"
            )
        if key in seen:
            raise ContentError(f"строка {line}: в 'throw' дважды указана часть «{part}» ('{token}')")
        seen.add(key)
        result[key] = new_value
    if result["buttons"] is None:
        raise ContentError(f"строка {line}: в 'throw' не указаны кнопки мыши ({', '.join(THROW_BUTTONS)}), например 'jump + lmb'")
    return result


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
        "throw": parse_throw(*fields["throw"]),
        "title": fields["title"][0],
        "description": fields["description"][0] if "description" in fields else "",
        "origin": parse_point(*fields["origin"], registry),
        "target": parse_point(*fields["target"], registry),
        "aim": (0.5, 0.5),  # прицел наведён в центр исходного скриншота
        "tolerance": None,
        "zoom": None,
        "howto_zoom": None,
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
    if "howto_zoom" in fields:
        value, line = fields["howto_zoom"]
        (howto_zoom,) = parse_floats(value, line, 1, "howto_zoom")
        if not (MIN_ZOOM <= howto_zoom <= MAX_ZOOM):
            raise ContentError(f"строка {line}: howto_zoom должен быть от {MIN_ZOOM:g} до {MAX_ZOOM:g}, а не '{value}'")
        lineup["howto_zoom"] = howto_zoom
    return lineup


# ---------- Картинки ----------

def find_image(screens_dir, name):
    """Ищет <name>.png / .jpg / .jpeg (любой регистр расширения). Возвращает путь или None."""
    for extension in (".png", ".jpg", ".jpeg", ".PNG", ".JPG", ".JPEG"):
        candidate = screens_dir / f"{name}{extension}"
        if candidate.exists():
            return candidate
    return None


def find_screenshot(screens_dir, lineup_id):
    path = find_image(screens_dir, lineup_id)
    if path is None:
        raise ContentError(f"[{lineup_id}]: не найден скриншот. Положите его в {screens_dir} как {lineup_id}.png (или .jpg)")
    return path


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


def center_crop_box(width, height, crop):
    """Рамка обрезки по центру: центральная часть кадра размером crop по каждой стороне."""
    crop_w, crop_h = round(width * crop), round(height * crop)
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


def load_16_by_9(path):
    """Открывает скриншот и подрезает до 16:9 (с предупреждением, если пришлось)."""
    original = Image.open(path).convert("RGB")
    image = to_16_by_9(original)
    if image.size != original.size:
        print(f"Предупреждение: скриншот {path.name} не 16:9 ({original.width}x{original.height}), он подрезан по центру")
    return image


def center_part(image, zoom=None):
    """Центральная часть кадра для туториала: в zoom раз меньше целого (по умолчанию вдвое), сжатая до MAX_OUT_WIDTH."""
    return limit_width(image.crop(center_crop_box(*image.size, 1 / (zoom or DEFAULT_HOWTO_ZOOM))), MAX_OUT_WIDTH)


def make_images(source_path, howto_source=None, howto_zoom=None):
    """
    Возвращает (целый кадр для тренировки, кадр для туториала).
    Если есть howto_source (скриншот с худом и прицелом из игры), из него берётся центральная часть как есть.
    Иначе центральная часть чистого кадра и прицел рисуется скриптом.
    howto_zoom: во сколько раз кадр туториала крупнее целого (по умолчанию 2 = центральная половина, 1 = кадр целиком).
    """
    image = load_16_by_9(source_path)
    stand = limit_width(image, MAX_STAND_WIDTH)
    if howto_source is not None:
        return stand, center_part(load_16_by_9(howto_source), howto_zoom)
    howto = center_part(image, howto_zoom)
    draw_crosshair(howto)
    return stand, howto


# ---------- Kotlin ----------

def kotlin_string(text):
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def kotlin_float(value):
    return f"{float(value)!r}f"


def generate_kotlin(lineups, registry, slug):
    name_prefix = slug.capitalize()  # cache -> Cache: от него имена CacheGenerated, CacheCalibration, CacheData
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
        "import com.example.cs2lineuptrainer.domain.MouseButtons",
        "import com.example.cs2lineuptrainer.domain.Movement",
        "import com.example.cs2lineuptrainer.domain.Offset01",
        "import com.example.cs2lineuptrainer.domain.Side",
        "import com.example.cs2lineuptrainer.domain.ThrowModifier",
        "import com.example.cs2lineuptrainer.domain.ThrowType",
        "",
        "// Этот файл создаёт скрипт tools/convert_lineups.py. Вручную не править.",
        f"object {name_prefix}Generated {{",
    ]
    if used:
        lines.append("    // Каждая точка описана один раз, лайнапы ссылаются на неё.")
    for point_id in used:
        name, x, y = registry[point_id]
        lines.append(f"    private val p_{point_id} = mapPoint({name_prefix}Calibration, {kotlin_string(point_id)}, {kotlin_string(name)}, {kotlin_float(x)}, {kotlin_float(y)})")
    if not lineups:
        lines.append("    val lineups: List<Lineup> = emptyList()")
    else:
        lines += ["", "    val lineups: List<Lineup> = listOf("]
        for item in lineups:
            tolerance = f"            tolerance = {kotlin_float(item['tolerance'])},\n" if item["tolerance"] is not None else ""
            zoom = f"            zoom = {kotlin_float(item['zoom'])},\n" if item["zoom"] is not None else ""
            lines.append(
                "        Lineup(\n"
                f"            id = {kotlin_string(item['full_id'])},\n"
                f"            mapId = {name_prefix}Data.MAP_ID,\n"
                f"            grenade = GrenadeType.{item['grenade']},\n"
                f"            side = Side.{item['side']},\n"
                f"            area = Area.{item['area']},\n"
                f"            title = {kotlin_string(item['title'])},\n"
                f"            target = p_{item['target']},\n"
                f"            origin = p_{item['origin']},\n"
                f"            standView = LineupMedia.Image(R.drawable.{GEN_PREFIX}{item['full_id']}_stand),\n"
                f"            aimPoint = Offset01({kotlin_float(item['aim'][0])}, {kotlin_float(item['aim'][1])}),\n"
                f"{tolerance}"
                f"{zoom}"
                f"            throwType = ThrowType(\n"
                f"                modifier = ThrowModifier.{item['throw']['modifier']},\n"
                f"                movement = Movement.{item['throw']['movement']},\n"
                f"                jump = {'true' if item['throw']['jump'] else 'false'},\n"
                f"                buttons = MouseButtons.{item['throw']['buttons']},\n"
                f"            ),\n"
                f"            howTo = LineupMedia.{item['howto_kind']}(R.drawable.{GEN_PREFIX}{item['full_id']}_howto),\n"
                f"            description = {kotlin_string(item['description'])},\n"
                "        ),"
            )
        lines.append("    )")
    lines += ["}", ""]
    return "\n".join(lines)


# ---------- Основная часть ----------

def process_map(map_dir, seen_ids, errors):
    """Разбирает одну карту (папку с lineups.txt и screens/). Возвращает словарь с лайнапами и картинками."""
    txt_path = map_dir / "lineups.txt"
    screens_dir = map_dir / "screens"
    blocks, parse_errors = parse_blocks(txt_path)
    errors += [f"[{map_dir.name}] {e}" for e in parse_errors]
    registry, lineups, images = {}, [], {}

    for block in blocks:
        try:
            if block["id"] in seen_ids:
                raise ContentError(
                    f"строка {block['line']}: лайнап [{block['id']}] описан в этом файле дважды "
                    "(id должны быть уникальными в пределах карты)"
                )
            seen_ids[block["id"]] = map_dir.name
            lineup = build_lineup(block, registry)
            lineup["full_id"] = f"{map_dir.name}_{lineup['id']}"  # id в приложении и в именах ресурсов
            screenshot = find_screenshot(screens_dir, lineup["id"])
            howto_screenshot = find_image(screens_dir, f"{lineup['id']}_howto")
            images[lineup["id"]] = make_images(screenshot, howto_screenshot, lineup["howto_zoom"])

            gif = screens_dir / f"{lineup['id']}_howto.gif"
            if gif.exists() and howto_screenshot is not None:
                print(f"Предупреждение: у [{lineup['id']}] есть и {gif.name}, и {howto_screenshot.name}. Используется GIF")
            if gif.exists() and lineup["howto_zoom"] is not None:
                print(f"Предупреждение: у [{lineup['id']}] howto_zoom не влияет на GIF ({gif.name})")
            lineup["howto_kind"] = "Gif" if gif.exists() else "Image"
            lineup["howto_gif"] = gif if gif.exists() else None
            lineups.append(lineup)
        except ContentError as error:
            errors.append(f"[{map_dir.name}] {error}")
        except OSError as error:
            errors.append(f"[{map_dir.name}] [{block['id']}]: не удалось открыть изображение: {error}")

    known = {b["id"] for b in blocks}
    for file in sorted(screens_dir.glob("*")) if screens_dir.exists() else []:
        stem = file.stem[:-len("_howto")] if file.stem.endswith("_howto") else file.stem
        if stem not in known and file.suffix.lower() in (".png", ".jpg", ".jpeg", ".gif"):
            print(f"Предупреждение: [{map_dir.name}] файл {file.name} не относится ни к одному лайнапу из lineups.txt (проверьте имя)")
    return {"slug": map_dir.name, "lineups": lineups, "registry": registry, "images": images}


# ---------- Новая карта: заготовки ----------

RADAR_MAX = 1536  # радар (квадрат) сжимается до этой стороны, если он больше
WHITE_FRAME = (1920, 1080)  # размер белого скриншота тестового лайнапа

TEST_LINEUP = """[testlineup]
grenade     = smoke
side        = t
area        = mid
title       = Тестовый смок ({name})
throw       = lmb
origin      = center | Центр карты | setpos 0.0 0.0 0.0
target      = center
description = Тестовый лайнап: смок под ноги в центре карты. Замените его своими лайнапами.
"""

DATA_TEMPLATE = """package com.example.cs2lineuptrainer.data

import com.example.cs2lineuptrainer.R
import com.example.cs2lineuptrainer.domain.Lineup
import com.example.cs2lineuptrainer.domain.MapInfo
import com.example.cs2lineuptrainer.domain.RadarCalibration

// TODO: временная калибровка (радар центрирован на нулевых игровых координатах). Подберите настоящие posX, posY, scale:
// стартовые значения из файла de_<карта>.txt, потом сверка по 3-4 точкам (см. CONTENT_GUIDE.md, раздел «Новая карта»).
val {name}Calibration = RadarCalibration(posX = -2560f, posY = 2560f, scale = 5f)

// Файл создан скриптом tools/convert_lineups.py один раз, дальше его можно править руками (название, калибровка).
// Лайнапы создаёт тот же скрипт ({name}Generated.kt).
object {name}Data {{
    const val MAP_ID = "{slug}"

    val map = MapInfo(id = MAP_ID, name = "{name}", radar = R.drawable.{slug}_radar)

    val lineups: List<Lineup> = {name}Generated.lineups
}}
"""


def scaffold_map(map_dir, kotlin_dir, check):
    """
    Создаёт то, чего не хватает у новой или недоделанной карты: папку screens/, lineups.txt с тестовым лайнапом
    (и белым скриншотом для него), файл <Карта>Data.kt. Существующее не трогает.
    Возвращает список того, что создано (в режиме --check: что было бы создано).
    """
    slug = map_dir.name
    name = slug.capitalize()
    created = []

    screens = map_dir / "screens"
    if not screens.exists():
        created.append("screens/")
        if not check:
            screens.mkdir()

    txt = map_dir / "lineups.txt"
    if not txt.exists():
        created.append("lineups.txt (тестовый лайнап)")
        if not check:
            txt.write_text(TEST_LINEUP.format(slug=slug, name=name), encoding="utf-8")
            test_id = "testlineup"  # в приложении id станет <карта>_testlineup
            if find_image(screens, test_id) is None:
                Image.new("RGB", WHITE_FRAME, (255, 255, 255)).save(screens / f"{test_id}.png")

    data_file = kotlin_dir / f"{name}Data.kt"
    if not data_file.exists():
        created.append(data_file.name)
        if not check:
            data_file.write_text(DATA_TEMPLATE.format(slug=slug, name=name), encoding="utf-8")
    return created


def load_radar(source):
    """Открывает радар, проверяет, что он квадратный, и сжимает до RADAR_MAX (если больше)."""
    image = Image.open(source)
    if abs(image.width - image.height) > 2:
        raise ContentError(f"радар {source.name} не квадратный ({image.width}x{image.height}): нужна квадратная картинка")
    image = image.convert("RGBA" if "A" in image.getbands() else "RGB")
    if image.width > RADAR_MAX:
        image = image.resize((RADAR_MAX, RADAR_MAX), Image.LANCZOS)
    return image


def generate_maps_kotlin(slugs):
    """Kotlin-файл со списком всех карт: приложение берёт карты и лайнапы отсюда."""
    names = [f"{slug.capitalize()}Data" for slug in slugs]
    return "\n".join([
        "package com.example.cs2lineuptrainer.data",
        "",
        "import com.example.cs2lineuptrainer.domain.Lineup",
        "import com.example.cs2lineuptrainer.domain.MapInfo",
        "",
        "// Этот файл создаёт скрипт tools/convert_lineups.py: список всех карт из папок content/. Вручную не править.",
        "// Первая карта списка выбрана при запуске приложения.",
        "object GeneratedMaps {",
        f"    val maps: List<MapInfo> = listOf({', '.join(n + '.map' for n in names)})",
        f"    val lineups: List<Lineup> = {' + '.join(n + '.lineups' for n in names)}",
        "}",
        "",
    ])


def main():
    # Чтобы русский текст одинаково выводился в любой консоли.
    for stream in (sys.stdout, sys.stderr):
        stream.reconfigure(encoding="utf-8", errors="replace")

    root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description="Конвертер лайнапов для CS2LineupTrainer")
    parser.add_argument("--check", action="store_true", help="только проверить пак, ничего не записывать и не создавать")
    parser.add_argument("--content", type=Path, default=root / "content", help="папка с папками карт (content/<карта>/)")
    parser.add_argument("--res", type=Path, default=root / "app/src/main/res/drawable-nodpi", help="куда класть картинки")
    parser.add_argument("--kotlin-dir", type=Path,
                        default=root / "app/src/main/java/com/example/cs2lineuptrainer/data",
                        help="куда записывать Kotlin-файлы <Карта>Generated.kt, <Карта>Data.kt, GeneratedMaps.kt")
    args = parser.parse_args()

    if (args.content / "lineups.txt").exists():
        sys.exit(
            f"Найден {args.content / 'lineups.txt'}: теперь у каждой карты своя папка.\n"
            "Переместите lineups.txt и папку screens/ в content/<карта>/ (например content/cache/)."
        )
    if args.content.exists():
        map_dirs = [d for d in sorted(args.content.iterdir()) if d.is_dir() and not d.name.startswith(".")]
    else:
        map_dirs = []
    if not map_dirs:
        sys.exit(f"В {args.content} нет ни одной папки карты. Создайте папку с названием карты, например content/mirage/")

    errors, maps = [], []
    for map_dir in map_dirs:
        slug = map_dir.name
        if not MAP_PATTERN.match(slug):
            errors.append(f"имя папки карты '{slug}' должно состоять из строчных латинских букв и цифр")
            continue

        created = scaffold_map(map_dir, args.kotlin_dir, args.check)
        if created:
            verb = "будет создано" if args.check else "создано"
            print(f"Карта {slug}: {verb}: {', '.join(created)}")

        radar_source = find_image(map_dir, "radar")
        radar_target = args.res / f"{slug}_radar.png"
        radar_image = None
        if radar_source is not None:
            try:
                radar_image = load_radar(radar_source)
            except ContentError as error:
                errors.append(f"[{slug}] {error}")
            except OSError as error:
                errors.append(f"[{slug}] не удалось открыть радар: {error}")
        elif not radar_target.exists():
            errors.append(f"[{slug}] нет радара: положите квадратную картинку {map_dir / 'radar.png'} (или radar.jpg) и запустите скрипт снова")

        if args.check and not (map_dir / "lineups.txt").exists():
            continue  # в режиме --check тестовый лайнап не создаётся, разбирать пока нечего
        item = process_map(map_dir, {}, errors)  # id уникальны только в пределах карты
        item["radar"] = radar_image
        maps.append(item)

    if errors:
        print(f"\nНайдено ошибок: {len(errors)}. Код приложения и картинки не записаны.\n")
        for error in errors:
            print(" -", error)
        sys.exit(1)

    for item in maps:
        print(f"Карта {item['slug']}: лайнапов {len(item['lineups'])}, точек {len(item['registry'])}.")
    if args.check:
        print("Режим --check: файлы не записывались.")
        return

    args.res.mkdir(parents=True, exist_ok=True)
    for old in args.res.glob(f"{GEN_PREFIX}*"):
        old.unlink()
    args.kotlin_dir.mkdir(parents=True, exist_ok=True)
    for item in maps:
        if item["radar"] is not None:
            item["radar"].save(args.res / f"{item['slug']}_radar.png", optimize=True)
        for lineup in item["lineups"]:
            stand, howto = item["images"][lineup["id"]]
            stand.save(args.res / f"{GEN_PREFIX}{lineup['full_id']}_stand.jpg", quality=JPEG_QUALITY, optimize=True)
            if lineup["howto_gif"]:
                (args.res / f"{GEN_PREFIX}{lineup['full_id']}_howto.gif").write_bytes(lineup["howto_gif"].read_bytes())
            else:
                howto.save(args.res / f"{GEN_PREFIX}{lineup['full_id']}_howto.jpg", quality=JPEG_QUALITY, optimize=True)
        kotlin_file = args.kotlin_dir / f"{item['slug'].capitalize()}Generated.kt"
        kotlin_file.write_text(generate_kotlin(item["lineups"], item["registry"], item["slug"]), encoding="utf-8")
        print(f"Kotlin-файл записан: {kotlin_file}")
    maps_file = args.kotlin_dir / "GeneratedMaps.kt"
    maps_file.write_text(generate_maps_kotlin([item["slug"] for item in maps]), encoding="utf-8")
    print(f"Список карт записан: {maps_file}")
    print(f"Картинки записаны в {args.res}")
    print("Теперь в Android Studio: Build -> Make Project, затем запускайте приложение.")


if __name__ == "__main__":
    main()
