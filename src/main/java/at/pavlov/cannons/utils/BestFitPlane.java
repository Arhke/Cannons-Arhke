package at.pavlov.cannons.utils;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;

public class BestFitPlane {
//
//    public static void main(String[] args) {
//        // Example points
//
//
//        // Print the plane normal and equation
//        System.out.println("Plane normal: " + normal);
//        double d = normal.get(0) * centroid.get(0, 0) + normal.get(1) * centroid.get(0, 1) + normal.get(2) * centroid.get(0, 2);
//        System.out.println("Plane equation: " + normal.get(0) + "x + " + normal.get(1) + "y + " + normal.get(2) + "z = " + d);
//    }

    private static DMatrixRMaj computeCentroid(DMatrixRMaj X) {
        int numPoints = X.numRows;
        DMatrixRMaj centroid = new DMatrixRMaj(1, 3);

        for (int i = 0; i < numPoints; i++) {
            centroid.add(0, 0, X.get(i, 0));
            centroid.add(0, 1, X.get(i, 1));
            centroid.add(0, 2, X.get(i, 2));
        }

        CommonOps_DDRM.scale(1.0 / numPoints, centroid);
        return centroid;
    }

    private static DMatrixRMaj centerPoints(DMatrixRMaj X, DMatrixRMaj centroid) {
        int numPoints = X.numRows;
        DMatrixRMaj centeredX = new DMatrixRMaj(numPoints, 3);

        for (int i = 0; i < numPoints; i++) {
            centeredX.set(i, 0, X.get(i, 0) - centroid.get(0, 0));
            centeredX.set(i, 1, X.get(i, 1) - centroid.get(0, 1));
            centeredX.set(i, 2, X.get(i, 2) - centroid.get(0, 2));
        }

        return centeredX;
    }
}
