package at.pavlov.cannons.utils;


public class Vector2D {

	double _x;
	double _z;


	public static String XKey = "x", ZKey = "z";
	public Vector2D(int x, int z) {
		_x = x;
		_z = z;
	}


	public Vector2D(Vector2D vector) {
		_x = vector.getX();
		_z = vector.getZ();
	}


	public double getX() {
		return _x;
	}
	public double getZ() {
		return _z;
	}
	public void setX(double x) {
		_x = x;
	}
	public void setZ (double z){
		_z = z;
	}

}
