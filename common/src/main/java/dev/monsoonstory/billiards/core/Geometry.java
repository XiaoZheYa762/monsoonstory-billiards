package dev.monsoonstory.billiards.core;
/** Generated from table_geometry.json by tools/generate_assets.py. */
public final class Geometry {
    public static final int LENGTH=10, WIDTH=6;
    public static final double SURFACE=0.8, R=0.16, INSET=0.8, POCKET_R=0.3, JAW_R=0.07;
    public static final double CORNER_MOUTH=0.375, SIDE_MOUTH=0.36;
    public static final double POCKET_SETBACK=0.08;
    public static final double MIN_X=INSET,MAX_X=LENGTH-INSET,MIN_Z=INSET,MAX_Z=WIDTH-INSET;
    public static final double HEAD=MIN_X+(MAX_X-MIN_X)/4, FOOT=MIN_X+(MAX_X-MIN_X)*3/4, CENTER_Z=WIDTH/2.0;
    public static final double[][] POCKETS={{MIN_X-POCKET_SETBACK,MIN_Z-POCKET_SETBACK},{LENGTH/2.0,MIN_Z-.12-POCKET_SETBACK},{MAX_X+POCKET_SETBACK,MIN_Z-POCKET_SETBACK},{MAX_X+POCKET_SETBACK,MAX_Z+POCKET_SETBACK},{LENGTH/2.0,MAX_Z+.12+POCKET_SETBACK},{MIN_X-POCKET_SETBACK,MAX_Z+POCKET_SETBACK}};
    public static final double[][] RAILS={{0,MIN_Z,MIN_X+CORNER_MOUTH,LENGTH/2.0-SIDE_MOUTH,1},{0,MIN_Z,LENGTH/2.0+SIDE_MOUTH,MAX_X-CORNER_MOUTH,1},{0,MAX_Z,MIN_X+CORNER_MOUTH,LENGTH/2.0-SIDE_MOUTH,-1},{0,MAX_Z,LENGTH/2.0+SIDE_MOUTH,MAX_X-CORNER_MOUTH,-1},{1,MIN_X,MIN_Z+CORNER_MOUTH,MAX_Z-CORNER_MOUTH,1},{1,MAX_X,MIN_Z+CORNER_MOUTH,MAX_Z-CORNER_MOUTH,-1}};
    private Geometry() {}
}
