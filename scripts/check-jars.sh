#!/usr/bin/env bash
# Verify Workshop Core dist jars: no leftover Stonecutter comments, valid
# json/toml, mixin metadata, mixin classes, and compatibilityLevel.
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <jar> [jar...]" >&2
  echo "       $0 <dist-dir>" >&2
  exit 2
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PY="$(command -v python3)"

jars=()
for arg in "$@"; do
  if [[ -d "$arg" ]]; then
    while IFS= read -r f; do jars+=("$f"); done < <(find "$arg" -maxdepth 1 -type f -name 'workshopcore-*.jar' | sort)
  else
    jars+=("$arg")
  fi
done

if [[ ${#jars[@]} -eq 0 ]]; then
  echo "no workshopcore jars found" >&2
  exit 2
fi

"$PY" - "$ROOT" "${jars[@]}" << 'PY'
import json, os, re, sys, tempfile, zipfile
from pathlib import Path

try:
    import tomllib
except ImportError:
    import tomli as tomllib

MIXIN_DESC = b"Lorg/spongepowered/asm/mixin/Mixin;"
JAVA_LEVEL = re.compile(r"JAVA_(\d+)")
JAR_NAME = re.compile(
    r"workshopcore-(?P<loader>fabric|forge|neoforge)-(?P<mc>1\.20\.1|1\.21\.1|26\.\d+(?:\.\d+)?)-"
)

def target_java(mc: str) -> int:
    if mc.startswith("26"):
        return 25
    if mc == "1.21.1":
        return 21
    if mc == "1.20.1":
        return 17
    raise SystemExit(f"unknown minecraft version in jar name: {mc}")

def needs_mixins(loader: str, mc: str) -> bool:
    if loader == "fabric":
        return True
    if loader == "forge":
        return True
    # NeoForge 26.x uses native hooks, not mixins
    return not mc.startswith("26")

def parse_jar_name(path: Path):
    m = JAR_NAME.search(path.name)
    if not m:
        raise SystemExit(f"cannot parse version/loader from {path.name}")
    return m.group("loader"), m.group("mc")

def read_zip(zf: zipfile.ZipFile, name: str) -> bytes | None:
    try:
        return zf.read(name)
    except KeyError:
        return None

def find_entries(zf: zipfile.ZipFile, suffixes):
    for info in zf.infolist():
        if info.is_dir():
            continue
        lower = info.filename.lower()
        if any(lower.endswith(s) for s in suffixes):
            yield info.filename, zf.read(info.filename)

def mixin_names(cfg: dict):
    names = []
    for key in ("mixins", "client", "server"):
        vals = cfg.get(key) or []
        names.extend(vals)
    return names

def has_mixin_annotation(data: bytes) -> bool:
    return MIXIN_DESC in data

def check_json(name: str, raw: bytes, errors: list):
    text = raw.decode("utf-8")
    if "//?" in text:
        errors.append(f"{name}: leftover Stonecutter comment (//?)")
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError as e:
        errors.append(f"{name}: invalid JSON ({e})")
        return None

def check_toml(name: str, raw: bytes, errors: list):
    text = raw.decode("utf-8")
    if "//?" in text:
        errors.append(f"{name}: leftover Stonecutter comment (//?)")
        return None
    try:
        return tomllib.loads(text)
    except Exception as e:
        errors.append(f"{name}: invalid TOML ({e})")
        return None

def check_jar(path: Path) -> list[str]:
    errors = []
    loader, mc = parse_jar_name(path)
    java = target_java(mc)
    with zipfile.ZipFile(path) as zf:
        names = {i.filename for i in zf.infolist() if not i.is_dir()}

        # leftover //? and validity in resource files
        mixin_cfg = None
        for name, raw in find_entries(zf, (".json", ".toml", ".mcmeta")):
            if name.lower().endswith(".toml"):
                data = check_toml(name, raw, errors)
                continue
            data = check_json(name, raw, errors)
            if name.endswith("workshopcore.mixins.json"):
                mixin_cfg = data

        # loader mixin metadata
        if needs_mixins(loader, mc):
            found = False
            if loader == "fabric":
                raw = read_zip(zf, "fabric.mod.json")
                if raw is None:
                    errors.append("missing fabric.mod.json")
                else:
                    meta = None
                    try:
                        meta = json.loads(raw.decode("utf-8"))
                    except json.JSONDecodeError:
                        pass
                    mix = (meta or {}).get("mixins") or []
                    if "workshopcore.mixins.json" in mix:
                        found = True
                    else:
                        errors.append('fabric.mod.json is missing a "mixins" entry for workshopcore.mixins.json')
            else:
                toml_name = "META-INF/mods.toml" if loader == "forge" else "META-INF/neoforge.mods.toml"
                raw = read_zip(zf, toml_name)
                if raw is None:
                    errors.append(f"missing {toml_name}")
                else:
                    text = raw.decode("utf-8")
                    if re.search(r'\[\[mixins\]\]\s*config\s*=\s*"workshopcore\.mixins\.json"', text):
                        found = True
                    else:
                        errors.append(f"{toml_name} is missing a [[mixins]] entry for workshopcore.mixins.json")
            if found and "workshopcore.mixins.json" not in names:
                errors.append("loader metadata points at workshopcore.mixins.json, but it is not in the jar")
        else:
            if "workshopcore.mixins.json" in names:
                errors.append("26.x NeoForge jar should not ship workshopcore.mixins.json")

        if mixin_cfg is not None:
            level = mixin_cfg.get("compatibilityLevel")
            if not isinstance(level, str):
                errors.append("workshopcore.mixins.json is missing compatibilityLevel")
            else:
                m = JAVA_LEVEL.fullmatch(level)
                if not m:
                    errors.append(f"unrecognised compatibilityLevel {level!r}")
                else:
                    n = int(m.group(1))
                    if n > java:
                        errors.append(
                            f"compatibilityLevel {level} is above Java {java} for {mc}"
                        )
            pkg = mixin_cfg.get("package") or "com.workshopcraft.workshopcore.mixin"
            for cls in mixin_names(mixin_cfg):
                entry = pkg.replace(".", "/") + "/" + cls + ".class"
                raw = read_zip(zf, entry)
                if raw is None:
                    errors.append(f"mixin {cls} is listed in workshopcore.mixins.json but {entry} is not in the jar")
                    continue
                if not has_mixin_annotation(raw):
                    errors.append(f"mixin {cls} ({entry}) has no @Mixin annotation")
    return errors

root = sys.argv[1]
jars = [Path(p) for p in sys.argv[2:]]
failed = 0
for jar in jars:
    errors = check_jar(jar)
    if errors:
        failed += 1
        print(f"FAIL  {jar.name}")
        for e in errors:
            print(f"      - {e}")
    else:
        print(f"OK    {jar.name}")
print()
print(f"{len(jars) - failed} passed, {failed} failed, {len(jars)} checked")
sys.exit(1 if failed else 0)
PY
