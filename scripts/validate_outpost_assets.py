"""Resource references, both languages and unique survival recipes for field equipment."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
a=root/'assets/wayaround';d=root/'data/wayaround'
blocks=['contact_mine','standalone_mine','spike_barrage','fixed_machine_gun','passage_alarm','field_barricade','remote_charge']
items=blocks+['outpost_gun_barrel','field_camo','drone_controller','camera_drone','impact_drone','smoke_canister','field_flare','clockwork_propeller','drone_chassis']
langs=[json.loads((a/f'lang/{l}.json').read_text()) for l in ['pt_br','en_us']]
def sig(j):
 if j.get('type')!='minecraft:crafting_shaped':return None
 rows=[list(r) for r in j['pattern']]
 while rows and all(c==' ' for c in rows[0]):rows.pop(0)
 while rows and all(c==' ' for c in rows[-1]):rows.pop()
 while rows and all(r[0]==' ' for r in rows):rows=[r[1:] for r in rows]
 while rows and all(r[-1]==' ' for r in rows):rows=[r[:-1] for r in rows]
 grid=tuple(tuple(json.dumps(j['key'][c],sort_keys=True) if c!=' ' else '' for c in r) for r in rows)
 return min(grid,tuple(tuple(reversed(r)) for r in grid))
allrecipes=[(p,json.loads(p.read_text())) for p in (d/'recipe').glob('*.json')]
for name in items:
 model=json.loads((a/f'models/item/{name}.json').read_text())
 recipe=json.loads((d/f'recipe/{name}.json').read_text())
 assert recipe['result']['id']=='wayaround:'+name,name
 signature=sig(recipe)
 dup=[p.stem for p,j in allrecipes if p.stem!=name and sig(j)==signature]
 assert not dup,(name,'duplicate recipe',dup)
 key=('block' if name in blocks else 'item')+'.wayaround.'+name
 assert all(key in lang for lang in langs),key
 if name in blocks:
  state=json.loads((a/f'blockstates/{name}.json').read_text())
  assert len(state['variants'])==4
  json.loads((d/f'loot_table/blocks/{name}.json').read_text())
  model=json.loads((a/f'models/block/{name}.json').read_text())
 for element in model.get('elements',[]):
  assert all(element['from'][i]<=element['to'][i] for i in range(3)),name
  for face in element['faces'].values():assert face['texture'][1:] in model['textures'],name
print('Outpost: all 16 equipment recipes are unique; models, blockstates, loot and both languages resolve')
