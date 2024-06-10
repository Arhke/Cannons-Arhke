package at.pavlov.cannons.utils;


public enum Direction3D {
    POSITIVEX(1,0,0),NEGATIVEX(-1,0,0),
    POSITIVEY(0,1,0), NEGATIVEY(0,-1,0),
    POSITIVEZ(0,0,1), NEGATIVEZ(0,0,-1);

    public final int x,y,z;
    Direction3D(int x,int y, int z){
        this.x = x; this.y = y; this.z = z;
    }
    public Direction3D opposite() {
        if(this == POSITIVEX){
            return NEGATIVEX;
        }else if(this == POSITIVEY){
            return NEGATIVEY;
        }else if(this == POSITIVEZ){
            return NEGATIVEZ;
        }else if(this==NEGATIVEX){
            return POSITIVEX;
        }else if(this == NEGATIVEY){
            return POSITIVEY;
        }else{
            return POSITIVEZ;
        }
    }
    public boolean isPositive(){
        return x+y+z > 0;
    }
    public double getVectorX(Vector3D v) {
        if (Math.abs(this.x) == 1){
            return v.getY();
        }else{
            return v.getX();
        }
    }
    public double getVectorY(Vector3D v) {
        if (Math.abs(this.z) == 1){
            return v.getY();
        }else{
            return v.getZ();
        }
    }
    public double getVectorZ(Vector3D v) {
        return Math.abs(this.x)*v.getX()
                +Math.abs(this.y)*v.getY()
                +Math.abs(this.z)*v.getZ();
    }
    public Vector3D getVectorFromDirectionVector(double x, double y, double z){
        if (Math.abs(this.x) == 1){
            return new Vector3D(z, x, y);
        }else if (Math.abs(this.y) == 1){
            return new Vector3D(x, z, y);
        }else{
            return new Vector3D(x,y, z);
        }
    }
    public double[] getDoubleArrayFromDirectionVector(double x, double y, double z){
        if (Math.abs(this.x) == 1){
            return new double[]{z, x, y};
        }else if (Math.abs(this.y) == 1){
            return new double[]{x, z, y};
        }else{
            return new double[]{x,y, z};
        }
    }
    public static Direction3D getDirectionFromCentroid(double x, double y, double z){
        double max = Math.max(Math.max(Math.abs(x),Math.abs(y)),Math.abs(z));
        if (Math.abs(x) == max){
            if (x >= 0){
                return POSITIVEX;
            }
            return NEGATIVEX;
        }else if (Math.abs(y) == max){
            if (y >= 0){
                return POSITIVEY;
            }
            return NEGATIVEY;
        }else{
            if (z >= 0){
                return POSITIVEZ;
            }
            return NEGATIVEZ;
        }
    }
}
