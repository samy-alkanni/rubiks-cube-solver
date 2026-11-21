public class RingDebugger {
    public static void main(String[] args) {
        Cube cube = new Cube();
        System.out.println("Initial face F:");
        printFace(cube.getFaces()[0]);
        System.out.println("Initial face B:");
        printFace(cube.getFaces()[2]);

        System.out.println("Applying F");
        cube.applyMove("F");
        System.out.println("After F - face F:");
        printFace(cube.getFaces()[0]);
        System.out.println("After F - face B:");
        printFace(cube.getFaces()[2]);

        System.out.println("Applying B");
        cube.applyMove("B");
        System.out.println("After B - face F:");
        printFace(cube.getFaces()[0]);
        System.out.println("After B - face B:");
        printFace(cube.getFaces()[2]);
    }

    private static void printFace(char[][] face) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                System.out.print(face[r][c]);
            }
            System.out.println();
        }
    }
}
