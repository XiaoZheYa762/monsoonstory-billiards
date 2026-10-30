"""Regenerate table geometry and three style models from table_geometry.json."""
from pathlib import Path
import json, math, sys
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'common/src/main/resources/assets/monsoonstory_billiards'
S=json.loads((ROOT/'table_geometry.json').read_text())
L,W=S['length'],S['width'];R=S['ball_radius'];I=S['rail_inset']
TABLE_ONLY=True
P=S['pocket_radius'];C=S['corner_mouth'];M=S['side_mouth'];B=S['pocket_setback']
def save(path,value):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8')
g=ROOT/'common/src/main/java/dev/monsoonstory/billiards/core/Geometry.java'
g.write_text(f"""package dev.monsoonstory.billiards.core;
/** Generated from table_geometry.json by tools/generate_table.py. */
public final class Geometry {{
    public static final int LENGTH={L}, WIDTH={W};
    public static final double SURFACE={S['surface']}, R={R}, INSET={I}, POCKET_R={S['pocket_radius']}, JAW_R={S['jaw_radius']};
    public static final double CORNER_MOUTH={C}, SIDE_MOUTH={M};
    public static final double POCKET_SETBACK={B};
    public static final double MIN_X=INSET,MAX_X=LENGTH-INSET,MIN_Z=INSET,MAX_Z=WIDTH-INSET;
    public static final double HEAD=MIN_X+(MAX_X-MIN_X)/4, FOOT=MIN_X+(MAX_X-MIN_X)*3/4, CENTER_Z=WIDTH/2.0;
    public static final double[][] POCKETS={{{{MIN_X-POCKET_SETBACK,MIN_Z-POCKET_SETBACK}},{{LENGTH/2.0,MIN_Z-.12-POCKET_SETBACK}},{{MAX_X+POCKET_SETBACK,MIN_Z-POCKET_SETBACK}},{{MAX_X+POCKET_SETBACK,MAX_Z+POCKET_SETBACK}},{{LENGTH/2.0,MAX_Z+.12+POCKET_SETBACK}},{{MIN_X-POCKET_SETBACK,MAX_Z+POCKET_SETBACK}}}};
    public static final double[][] RAILS={{{{0,MIN_Z,MIN_X+CORNER_MOUTH,LENGTH/2.0-SIDE_MOUTH,1}},{{0,MIN_Z,LENGTH/2.0+SIDE_MOUTH,MAX_X-CORNER_MOUTH,1}},{{0,MAX_Z,MIN_X+CORNER_MOUTH,LENGTH/2.0-SIDE_MOUTH,-1}},{{0,MAX_Z,LENGTH/2.0+SIDE_MOUTH,MAX_X-CORNER_MOUTH,-1}},{{1,MIN_X,MIN_Z+CORNER_MOUTH,MAX_Z-CORNER_MOUTH,1}},{{1,MAX_X,MIN_Z+CORNER_MOUTH,MAX_Z-CORNER_MOUTH,-1}}}};
    private Geometry() {{}}
}}
""",encoding='utf-8')
textures={'felt':'monsoonstory_billiards:block/felt','wood':'monsoonstory_billiards:block/walnut','gold':'monsoonstory_billiards:block/brass','black':'minecraft:block/black_concrete','leather':'monsoonstory_billiards:block/leather','particle':'monsoonstory_billiards:block/walnut'}
boxes=[]
def box(a,b,t):boxes.append((a,b,t))
pockets=[(I-B,I-B),(L/2,I-.12-B),(L-I+B,I-B),(L-I+B,W-I+B),(L/2,W-I+.12+B),(I-B,W-I+B)]
corner_radius=S['table_corner_radius']
def rounded(x,z,x0,z0,x1,z1,r):
    if not (x0<=x<=x1 and z0<=z<=z1):return False
    dx=max(x0+r-x,0,x-(x1-r));dz=max(z0+r-z,0,z-(z1-r))
    return dx*dx+dz*dz<=r*r
def outline(x,z,inset):
    return rounded(x,z,inset,inset,L-inset,W-inset,max(0,corner_radius-inset))
def raster(bounds,y0,y1,material,predicate):
    """Fine curved silhouettes, merged into long strips instead of thousands of cubes."""
    grid=64;x0,z0,x1,z1=bounds;active={}
    for ix in range(math.floor(x0*grid),math.ceil(x1*grid)+1):
        runs=[];start=None
        for iz in range(math.floor(z0*grid),math.ceil(z1*grid)+1):
            included=ix<math.ceil(x1*grid) and iz<math.ceil(z1*grid) and predicate((ix+.5)/grid,(iz+.5)/grid)
            if included and start is None:start=iz
            if not included and start is not None:runs.append((start,iz));start=None
        current=set(runs)
        for span,first in list(active.items()):
            if span not in current:
                box((first/grid,y0,span[0]/grid),(ix/grid,y1,span[1]/grid),material);del active[span]
        for span in runs:active.setdefault(span,ix)
directions=[(1/math.sqrt(2),1/math.sqrt(2)),(0,1),(-1/math.sqrt(2),1/math.sqrt(2)),
            (-1/math.sqrt(2),-1/math.sqrt(2)),(0,-1),(1/math.sqrt(2),-1/math.sqrt(2))]
def pocket_distance(x,z,index):
    px,pz=pockets[index];dx,dz=directions[index];u=x-px;v=z-pz
    # A short diagonal throat joins each corner cup to the cloth, as on a real table.
    throat=.06 if index not in (1,4) else 0
    along=max(0,min(throat,u*dx+v*dz))
    return math.hypot(u-along*dx,v-along*dz)
def cutout(x,z):return any(pocket_distance(x,z,i)<P+.03 for i in range(6))
bounds=(0,0,L,W)
raster(bounds,.66,.8,'felt',lambda x,z:outline(x,z,.4) and not cutout(x,z))
raster(bounds,.40,.62,'wood',lambda x,z:outline(x,z,.18) and not outline(x,z,.65))
frame=lambda x,z:outline(x,z,0) and not outline(x,z,.4) and not cutout(x,z)
for y0,y1,material in [(.57,.82,'wood'),(.82,.85,'gold'),(.85,.94,'wood')]:
    raster(bounds,y0,y1,material,frame)
# Rounded cushion noses share the exact cap centers and radius used by Physics.RAILS.
jaw=S['jaw_radius']
for z in [.4,W-.8]:
    for a,b in [(I+C,L/2-M),(L/2+M,L-I-C)]:
        raster((a-jaw,z,b+jaw,z+.4),.75,.96,'felt',
               lambda x,q,a=a,b=b,z=z:rounded(x,q,a-jaw,z,b+jaw,z+.4,jaw))
for x in [.4,L-.8]:
    a=I+C;b=W-I-C
    raster((x,a-jaw,x+.4,b+jaw),.75,.96,'felt',
           lambda q,z,x=x:rounded(q,z,x,a-jaw,x+.4,b+jaw,jaw))
for index,(px,pz) in enumerate(pockets):
    dx,dz=directions[index];area=(px-P-.14,pz-P-.14,px+P+.14,pz+P+.14)
    distance=lambda x,z,index=index:pocket_distance(x,z,index)
    back=lambda x,z,px=px,pz=pz,dx=dx,dz=dz:(x-px)*dx+(z-pz)*dz<=.018
    raster(area,.625,.64,'black',lambda x,z:distance(x,z)<P+.03)
    raster(area,.64,.77,'black',lambda x,z:P<=distance(x,z)<P+.03)
    raster(area,.77,.8,'leather',lambda x,z:P<=distance(x,z)<P+.03 and not back(x,z))
    # The brass collar is only behind the cup. Its front is open to the playing field.
    raster(area,.77,.935,'leather',lambda x,z:P<=distance(x,z)<P+.12 and back(x,z))
    raster(area,.935,.965,'gold',lambda x,z:P<=distance(x,z)<P+.12 and back(x,z))
for x in [.45,L/2-.3,L-1.05]:
    for z in [.45,W-1.05]:
        box((x,0,z),(x+.6,.6,z+.6),'wood');box((x-.02,.06,z-.02),(x+.62,.14,z+.62),'gold')
for x in [L*.2,L*.35,L*.65,L*.8]:
    for z in [.18,W-.18]:box((x-.045,.942,z-.045),(x+.045,.95,z+.045),'gold')
variants={}
modeldir=A/'models/block';modeldir.mkdir(parents=True,exist_ok=True)
for stale in modeldir.glob('table_*_*.json'):
    if not stale.resolve().is_relative_to(modeldir.resolve()):raise ValueError('Unexpected model path')
    stale.unlink()
for col in range(L):
    for row in range(W):
        elements=[]
        for a,b,t in boxes:
            lo=[max(a[0],col),a[1],max(a[2],row)];hi=[min(b[0],col+1),b[1],min(b[2],row+1)]
            if hi[0]-lo[0]<1e-6 or hi[2]-lo[2]<1e-6:continue
            low=[round((lo[0]-col)*16,4),round(lo[1]*16,4),round((lo[2]-row)*16,4)]
            high=[round((hi[0]-col)*16,4),round(hi[1]*16,4),round((hi[2]-row)*16,4)]
            faces={f:{'texture':'#'+t} for f in ['up','down','north','south','east','west']}
            elements.append({'from':low,'to':high,'faces':faces})
        save(modeldir/f'table_{col}_{row}.json',{'ambientocclusion':True,'textures':textures,'elements':elements})
        for r in range(4):variants[f'col={col},rotation={r},row={row}']={'model':f'monsoonstory_billiards:block/table_{col}_{row}','y':r*90}
save(A/'blockstates/table_part.json',{'variants':variants})

if TABLE_ONLY:
    styles={'green':('felt','walnut'),'pink':('felt_pink','wood_ivory'),'deep_blue':('felt_deep_blue','wood_dark')}
    variants={}
    for col in range(L):
        for row in range(W):
            for style,(felt,wood) in styles.items():
                base=f'table_{col}_{row}';path=base if style=='green' else f'{style}/{base}'
                if style!='green':save(modeldir/f'{path}.json',{'parent':f'monsoonstory_billiards:block/{base}','textures':{'felt':f'monsoonstory_billiards:block/{felt}','wood':f'monsoonstory_billiards:block/{wood}','particle':f'monsoonstory_billiards:block/{wood}'}})
                for rot in range(4):variants[f'col={col},rotation={rot},row={row},style={style}']={'model':f'monsoonstory_billiards:block/{path}','y':rot*90}
    save(A/'blockstates/table_part.json',{'variants':variants})
    print('Generated pocket geometry and all three table styles; other assets preserved.')
    sys.exit(0)