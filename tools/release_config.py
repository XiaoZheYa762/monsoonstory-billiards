"""Release matrix shared by validation and packaging (no runtime dependencies)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# module, loader, exact Minecraft version, Java class-file version
TARGETS = (
    ('fabric', 'fabric', '1.20.1', 61),
    ('forge', 'forge', '1.20.1', 61),
    ('neoforge', 'neoforge', '1.21.1', 65),
    ('neoforge-26.1', 'neoforge', '26.1', 69),
)


def version(module='fabric'):
    for line in (ROOT/module/'gradle.properties').read_text(encoding='utf-8').splitlines():
        if line.startswith('mod_version='):
            return line.split('=', 1)[1].strip()
    raise ValueError(f'Missing mod_version: {module}')


def release_jar(target):
    module, loader, game, _ = target
    return ROOT/module/'build/libs'/f'monsoonstory_billiards-{loader}-{game}-{version(module)}.jar'
