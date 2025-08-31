package br.com.dti.drone_delivery_sim.model;

public final class NoFlyZone {
    private final double minX, minY, maxX, maxY;

    public NoFlyZone(double x1, double y1, double x2, double y2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        if (minX == maxX || minY == maxY) throw new IllegalArgumentException("Formato de mapa (retângulo) inválido");
    }

    public double getMinX(){ return minX; }
    public double getMinY(){ return minY; }
    public double getMaxX(){ return maxX; }
    public double getMaxY(){ return maxY; }

    public boolean contem(double[] p) {
        return p[0] >= minX && p[0] <= maxX && p[1] >= minY && p[1] <= maxY;
    }

    public boolean intersectaSegmento(double[] a, double[] b){
        if (contem(a) || contem(b)) return true;
        double[] r1 = new double[]{minX,minY}, r2=new double[]{maxX,minY},
                r3 = new double[]{maxX,maxY}, r4=new double[]{minX,maxY};
        return segInter(a,b,r1,r2) || segInter(a,b,r2,r3) || segInter(a,b,r3,r4) || segInter(a,b,r4,r1);
    }

    private static boolean segInter(double[] p1, double[] p2, double[] q1, double[] q2){
        int o1 = orient(p1,p2,q1), o2 = orient(p1,p2,q2), o3 = orient(q1,q2,p1), o4 = orient(q1,q2,p2);
        if (o1 != o2 && o3 != o4) return true;
        return (o1==0 && onSeg(p1,q1,p2)) || (o2==0 && onSeg(p1,q2,p2))
                || (o3==0 && onSeg(q1,p1,q2)) || (o4==0 && onSeg(q1,p2,q2));
    }

    private static int orient(double[] a, double[] b, double[] c){
        double v = (b[1]-a[1])*(c[0]-b[0]) - (b[0]-a[0])*(c[1]-b[1]);
        if (Math.abs(v) < 1e-9) return 0;
        return v > 0 ? 1 : 2;
    }

    private static boolean onSeg(double[] a, double[] b, double[] c){
        return b[0] <= Math.max(a[0],c[0]) && b[0] >= Math.min(a[0],c[0]) &&
                b[1] <= Math.max(a[1],c[1]) && b[1] >= Math.min(a[1],c[1]);
    }
}

