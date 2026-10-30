"""Generate the public survival recipes (standard library only)."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
MOD = 'monsoonstory_billiards'
DATA = ROOT / 'common/src/main/resources/data' / MOD

def item(name):
    return {'item': 'minecraft:' + name}

def recipe(name, pattern, key):
    value = {'type': 'minecraft:crafting_shaped', 'category': 'misc',
             'pattern': pattern, 'key': key, 'result': {'item': MOD + ':' + name, 'count': 1}}
    output = DATA / 'recipes' / (name + '.json')
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    criteria = {}
    for entry in key.values():
        if 'item' in entry:
            ingredient = entry['item']
            criteria[ingredient.replace(':', '_')] = {'trigger': 'minecraft:inventory_changed',
                'conditions': {'items': [{'items': [ingredient]}]}}
    recipe_id = MOD + ':' + name
    criteria['has_the_recipe'] = {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': recipe_id}}
    advancement = {'parent': 'minecraft:recipes/root', 'criteria': criteria,
        'requirements': [list(criteria)], 'rewards': {'recipes': [recipe_id]}}
    output = DATA / 'advancements/recipes' / (name + '.json')
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(advancement, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

for suffix, color in (('', 'green'), ('_pink', 'pink'), ('_deep_blue', 'blue')):
    recipe('pool_table' + suffix, ['CCC', 'PIP', 'P P'],
           {'C': item(color + '_carpet'), 'P': {'tag': 'minecraft:planks'}, 'I': item('iron_ingot')})
    recipe('cue' + suffix, ['  I', ' S ', 'SC '],
           {'I': item('iron_nugget'), 'S': item('stick'), 'C': item(color + '_carpet')})
    recipe('panel_table_lamp_8x2' + suffix, ['PPP', 'ICI', 'LLL'],
           {'P': {'tag': 'minecraft:planks'}, 'I': item('iron_ingot'),
            'C': item(color + '_carpet'), 'L': item('lantern')})
recipe('cue_rack', ['S S', 'S S', 'PPP'], {'S': item('stick'), 'P': {'tag': 'minecraft:planks'}})
recipe('floor_scoreboard', ['PPP', 'R B', 'PPP'],
       {'P': {'tag': 'minecraft:planks'}, 'R': item('red_dye'), 'B': item('blue_dye')})
print('Generated 11 survival recipes and their recipe-book unlocks.')
