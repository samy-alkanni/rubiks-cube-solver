import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Solver for 2-look OLL step 1 (edge orientation only / U-face cross).
 * Convention in this project: U is White (W), so the last layer is face U.
 */
public class OLL2LookCrossSolver {
    private static final String OLL_CROSS_ALGO = "F R U R' U' F'";
    private static final String OLL_CORNER_ALGO = "R' D' R D";

    private final RubiksCube cube;
    private final Random random = new Random();

    public OLL2LookCrossSolver(RubiksCube cube) {
        this.cube = cube;
    }

    /**
     * Generates a valid demonstration state from solved cube:
     * repeatedly applies the OLL cross algorithm and interleaves random U turns.
     */
    public List<String> generateTestState() {
        cube.reset();
        cube.clearHistory();

        List<String> generatedMoves = new ArrayList<>();
        int applications = random.nextInt(3) + 2; // 2..4

        for (int i = 0; i < applications; i++) {
            if (i > 0) {
                String[] uMoves = {"U", "U'", "U2"};
                String uMove = uMoves[random.nextInt(uMoves.length)];
                cube.applyAlgorithm(uMove);
                generatedMoves.add(uMove);
            }

            cube.applyAlgorithm(OLL_CROSS_ALGO);
            generatedMoves.add(OLL_CROSS_ALGO);
        }

        return generatedMoves;
    }

    /**
     * Solves only the U-face cross (indices 1,3,5,7 on face U).
     */
    public List<String> solveCross() {
        List<String> moves = new ArrayList<>();

        int[] edgeIndices = {1, 3, 5, 7};
        int guard = 0;
        while (!isCrossSolved() && guard < 6) {
            guard++;

            boolean w1 = isWhiteAt(1);
            boolean w3 = isWhiteAt(3);
            boolean w5 = isWhiteAt(5);
            boolean w7 = isWhiteAt(7);

            int whiteCount = countWhiteEdges(edgeIndices);

            // Case 2: opposite pair ([1,7] or [3,5])
            if ((w1 && w7) || (w3 && w5)) {
                if (w1 && w7) {
                    applyMove("U", moves); // Align to [3,5]
                }
                applyMove(OLL_CROSS_ALGO, moves);
                continue;
            }

            // Case 3: adjacent pair (L-shape), place at [5,7], then algo, U, algo
            if (whiteCount == 2) {
                rotateUntilWhiteAt57(moves);
                applyMove(OLL_CROSS_ALGO, moves);
                applyMove("U", moves);
                applyMove(OLL_CROSS_ALGO, moves);
                continue;
            }

            // Case 4: dot pattern (no white edges)
            if (whiteCount == 0) {
                applyMove(OLL_CROSS_ALGO, moves);
                applyMove(OLL_CROSS_ALGO, moves);
                applyMove("U", moves);
                applyMove(OLL_CROSS_ALGO, moves);
                continue;
            }

            // Safety fallback for unexpected states (e.g., manual edits)
            applyMove(OLL_CROSS_ALGO, moves);
        }

        return moves;
    }

    /**
     * Solves U-face corner orientation only (indices 0,2,6,8 on face U)
     * using the algorithm R' D' R D.
     */
    public List<String> solveCorners() {
        List<String> moves = new ArrayList<>();

        if (areUCornersSolved()) {
            return moves;
        }

        int maxCornerIterations = 4;
        for (int i = 0; i < maxCornerIterations; i++) {
            if (areUCornersSolved()) {
                break;
            }

            // Place a non-white corner on U[8]
            rotateUntilU8NonWhite(moves);

            // Apply R' D' R D until U[8] becomes white
            int guard = 0;
            while (!isWhiteAt(8) && guard < 8) {
                applyMove(OLL_CORNER_ALGO, moves);
                guard++;
            }

            if (areUCornersSolved()) {
                break;
            }

            // Turn U until a new unsolved corner reaches U[8]
            int uGuard = 0;
            while (isWhiteAt(8) && uGuard < 4 && !areUCornersSolved()) {
                applyMove("U", moves);
                uGuard++;
            }
        }

        return moves;
    }

    private boolean isCrossSolved() {
        return isWhiteAt(1) && isWhiteAt(3) && isWhiteAt(5) && isWhiteAt(7);
    }

    private boolean areUCornersSolved() {
        return isWhiteAt(0) && isWhiteAt(2) && isWhiteAt(6) && isWhiteAt(8);
    }

    private boolean isWhiteAt(int index) {
        char[][] matrix = cube.getMatrix();
        return matrix[RubiksCube.U][index] == 'W';
    }

    private int countWhiteEdges(int[] edgeIndices) {
        int count = 0;
        for (int idx : edgeIndices) {
            if (isWhiteAt(idx)) {
                count++;
            }
        }
        return count;
    }

    private void rotateUntilU8NonWhite(List<String> moves) {
        if (!isWhiteAt(8)) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            applyMove("U", moves);
            if (!isWhiteAt(8)) {
                return;
            }
        }
    }

    private void rotateUntilWhiteAt57(List<String> moves) {
        for (int i = 0; i < 4; i++) {
            if (isWhiteAt(5) && isWhiteAt(7)) {
                return;
            }
            applyMove("U", moves);
        }
    }

    private void applyMove(String algorithm, List<String> moves) {
        cube.applyAlgorithm(algorithm);
        moves.add(algorithm);
    }
}
