import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Solver for the 2-look PLL corners (first step only, ignoring edges).
 * Uses diagonal and T-perm algorithms.
 */
public class PLL2LookSolver {
    private final RubiksCube cube;
    private final Random random = new Random();

    // Algorithms
    private static final String DIAGONAL_ALGO = "F R U' R' U' R U R' F' R U R' U' R' F R F'";
    private static final String T_PERM_ALGO = "R U R' U' R' F R2 U' R' U' R U R' F'";
    private static final String UA_PERM_ALGO = "R U' R U R U R U' R' U' R2";

    public PLL2LookSolver(RubiksCube cube) {
        this.cube = cube;
    }

    /**
     * Generates a test state by applying diagonal and T-perm algorithms randomly,
     * interspersed with U, U', U2 moves.
     *
     * @return the list of moves applied during generation
     */
    public List<String> generateTestState() {
        cube.reset(); // Start from solved state
        List<String> generatedMoves = new ArrayList<>();

        int numApplications = random.nextInt(3) + 2; // 2-4 applications

        for (int i = 0; i < numApplications; i++) {
            // Apply random U moves between algorithms
            if (i > 0) {
                String[] uMoves = {"U", "U'", "U2"};
                String uMove = uMoves[random.nextInt(uMoves.length)];
                cube.applyAlgorithm(uMove);
                generatedMoves.add(uMove);
            }

            // Apply random algorithm
            if (random.nextBoolean()) {
                cube.applyAlgorithm(DIAGONAL_ALGO);
                generatedMoves.add(DIAGONAL_ALGO);
            } else {
                cube.applyAlgorithm(T_PERM_ALGO);
                generatedMoves.add(T_PERM_ALGO);
            }
        }

        return generatedMoves;
    }

    /**
     * Solves the corners using 2-look PLL logic.
     * Returns the list of moves applied.
     */
    public List<String> solveCorners() {
        List<String> moves = new ArrayList<>();
        int maxIterations = 3;
        int iterations = 0;

        while (!areCornersSolved() && iterations < maxIterations) {
            iterations++;

            // Check for adjacent pair on any lateral face
            int faceWithAdjacentPair = findAdjacentPairFace();
            if (faceWithAdjacentPair != -1) {
                // Rotate U to bring this face to L position
                int rotationsNeeded = getRotationsToBringToL(faceWithAdjacentPair);
                for (int i = 0; i < rotationsNeeded; i++) {
                    cube.applyAlgorithm("U");
                    moves.add("U");
                }

                // Apply T-perm
                cube.applyAlgorithm(T_PERM_ALGO);
                moves.add(T_PERM_ALGO);
            } else {
                // No adjacent pair, apply diagonal algorithm
                cube.applyAlgorithm(DIAGONAL_ALGO);
                moves.add(DIAGONAL_ALGO);
            }
        }

        return moves;
    }

    /**
     * Checks if the corner cycle is solved for the 2-look PLL corner step.
     * We consider the step complete when each side-top-corner pair is adjacent
     * (stickers [0] and [2] are same on each lateral face), without comparing to center color.
     */
    private boolean areCornersSolved() {
        int[] lateralFaces = {RubiksCube.R, RubiksCube.F, RubiksCube.L, RubiksCube.B};
        for (int face : lateralFaces) {
            char[][] matrix = cube.getMatrix();
            if (matrix[face][0] != matrix[face][2]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds a face with an adjacent pair (stickers [0] and [2] have the same color).
     * Returns the face index or -1 if none found.
     */
    private int findAdjacentPairFace() {
        int[] lateralFaces = {RubiksCube.R, RubiksCube.F, RubiksCube.L, RubiksCube.B};
        for (int face : lateralFaces) {
            char[][] matrix = cube.getMatrix();
            if (matrix[face][0] == matrix[face][2]) {
                return face;
            }
        }
        return -1;
    }

    /**
     * Calculates how many U rotations are needed to bring the given face to L position.
     */
    private int getRotationsToBringToL(int face) {
        // Face indices: R=1, F=2, L=4, B=5
        // To bring to L (4): R->2, F->1, L->0, B->3 rotations
        switch (face) {
            case RubiksCube.R: return 2;
            case RubiksCube.F: return 1;
            case RubiksCube.L: return 0;
            case RubiksCube.B: return 3;
            default: return 0;
        }
    }

    /**
     * Solves the edges using Ua perm algorithm (2-look PLL second step).
     * Returns the list of moves applied.
     */
    public List<String> solveEdges() {
        List<String> moves = new ArrayList<>();
        int maxIterations = 5;
        int iterations = 0;

        while (!areEdgesSolved() && iterations < maxIterations) {
            iterations++;

            // Check for a face with all three stickers [0], [1], [2] having the same color
            int faceWithMatch = findFaceWithAllThreeStickers();
            if (faceWithMatch != -1) {
                // Rotate this face to B position
                rotateFaceToBack(faceWithMatch, moves);

                // Apply Ua perm until edges are solved (bounded to avoid infinite loops on invalid states)
                int uaGuard = 0;
                while (!areEdgesSolved() && uaGuard < 4) {
                    cube.applyAlgorithm(UA_PERM_ALGO);
                    moves.add(UA_PERM_ALGO);
                    uaGuard++;
                }

                if (!areEdgesSolved()) {
                    throw new IllegalStateException("PLL edges not solved after UA-perm retries (cube state may be invalid / scan mis-oriented)");
                }
            } else {
                // No matching face found, apply Ua perm once and try again
                cube.applyAlgorithm(UA_PERM_ALGO);
                moves.add(UA_PERM_ALGO);
            }
        }

        // Align U face for final solve
        alignUFace(moves);

        return moves;
    }

    /**
     * Checks if the edges are resolved (edge stickers [1] match with corner stickers [0] and [2]).
     */
    private boolean areEdgesSolved() {
        int[] lateralFaces = {RubiksCube.R, RubiksCube.F, RubiksCube.L, RubiksCube.B};
        for (int face : lateralFaces) {
            char[][] matrix = cube.getMatrix();
            char edge = matrix[face][1];
            char corner0 = matrix[face][0];
            char corner2 = matrix[face][2];
            // Edge is resolved if it matches both corners (forms a complete top row)
            if (edge != corner0 || edge != corner2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds a face where stickers [0], [1], [2] all have the same color.
     * Returns the face index or -1 if none found.
     */
    private int findFaceWithAllThreeStickers() {
        int[] lateralFaces = {RubiksCube.R, RubiksCube.F, RubiksCube.L, RubiksCube.B};
        for (int face : lateralFaces) {
            char[][] matrix = cube.getMatrix();
            if (matrix[face][0] == matrix[face][1] && matrix[face][1] == matrix[face][2]) {
                return face;
            }
        }
        return -1;
    }

    /**
     * Rotates a face to the B (back) position using U moves.
     * F -> B: 2 U rotations
     * R -> B: 1 U' rotation
     * L -> B: 1 U rotation
     * B -> B: no rotations
     */
    private void rotateFaceToBack(int face, List<String> moves) {
        switch (face) {
            case RubiksCube.F:
                cube.applyAlgorithm("U U");
                moves.add("U");
                moves.add("U");
                break;
            case RubiksCube.R:
                cube.applyAlgorithm("U'");
                moves.add("U'");
                break;
            case RubiksCube.L:
                cube.applyAlgorithm("U");
                moves.add("U");
                break;
            case RubiksCube.B:
                // Already at back position
                break;
        }
    }

    /**
     * Aligns the U face to complete the cube solve.
     * After resolving corners and edges, we may need to rotate U to align the top layer.
     * Tries up to 3 U rotations to find the correct orientation.
     */
    private void alignUFace(List<String> moves) {
        // Try up to 3 U rotations to find the correct alignment
        // Since U has 4 rotational states (0°, 90°, 180°, 270°), checking 3 times covers all possibilities
        for (int i = 0; i < 3; i++) {
            if (cube.isSolved()) {
                // Cube is already solved, no more rotations needed
                return;
            }
            
            // Try one U rotation
            cube.applyAlgorithm("U");
            moves.add("U");
        }
        
        // If cube is still not solved after 3 attempts, one more U rotation returns to original state
        // This shouldn't happen if corners and edges were properly resolved
        if (!cube.isSolved()) {
            // Fourth U rotation completes the cycle back to start (U, U, U, U = identity)
            cube.applyAlgorithm("U");
            moves.add("U");
        }
    }
}
