import java.util.ArrayList;
import java.util.List;

/**
 * F2L Corner Solver for Rubik's Cube - Second layer corner insertion.
 * 
 * Convention:
 * - U (face 0) = White (W)
 * - D (face 3) = Yellow (Y)
 * - F (face 2) = Green (G)
 * - R (face 1) = Red (R)
 * - B (face 4) = Blue (B)
 * - L (face 5) = Orange (O)
 */
public class F2LCornerSolver {
    private final RubiksCube cube;
    private final List<String> solveMoves;

    private static final int[] U_CORNER_INDICES = {0, 2, 6, 8};

    // ULF : U[6], L[2], F[0]
    private static final int[] ULF_POS = {RubiksCube.U, 6, RubiksCube.L, 2, RubiksCube.F, 0};
    // URF : U[8], R[0], F[2]
    private static final int[] URF_POS = {RubiksCube.U, 8, RubiksCube.R, 0, RubiksCube.F, 2};
    // URB : U[2], R[2], B[0]
    private static final int[] URB_POS = {RubiksCube.U, 2, RubiksCube.R, 2, RubiksCube.B, 0};
    // ULB : U[0], L[0], B[2]
    private static final int[] ULB_POS = {RubiksCube.U, 0, RubiksCube.L, 0, RubiksCube.B, 2};

    public F2LCornerSolver(RubiksCube cube) {
        this.cube = cube;
        this.solveMoves = new ArrayList<>();
    }

    /**
     * Generates a F2L-ready test state:
     * 1) scramble with 20 random moves
     * 2) resolve yellow cross so F2L can be validated from that baseline
     * @return the combined list of moves applied (scramble + yellow cross solve)
     */
    public List<String> scrambleForTest() {
        cube.reset();
        cube.clearHistory();

        List<String> scrambleMoves = cube.scramble(20);

        CFOPYellowCrossSolver yellowCrossSolver = new CFOPYellowCrossSolver(cube);
        List<String> yellowCrossMoves = yellowCrossSolver.solveYellowCross();

        List<String> setupMoves = new ArrayList<>();
        setupMoves.addAll(scrambleMoves);
        setupMoves.addAll(yellowCrossMoves);
        return setupMoves;
    }

    /**
     * Solves the F2L corners (second layer).
     * For each detected yellow corner:
     * 1) Identify corner with identifyCorner
     * 2) Align with U/U'/U2 to its target U index
     * 3) Apply associated algorithm up to 5 times until yellow reaches D target index
     * 4) Repeat until no yellow corner remains on U/lateral tracked positions
     * @return the list of moves applied to solve F2L corners
     */
    public List<String> solve() {
        solveMoves.clear();

        int passGuard = 0;
        while (passGuard < 40) {
            passGuard++;

            List<String> yellowCorners = findYellowCorners();
            if (yellowCorners.isEmpty()) {
                if (areAllYellowCornersSolvedOnD()) {
                    break;
                }

                boolean ejected = ejectOneMisorientedDCorner();
                if (!ejected) {
                    break;
                }
                continue;
            }

            // Important: process one corner at a time, then re-detect.
            // After any algorithm, previously detected entries are stale.
            String entry = yellowCorners.get(0);
            int[] parsed = parseEntryPosition(entry);
            if (parsed == null) {
                continue;
            }

            int face = parsed[0];
            int index = parsed[1];
            String cornerName = identifyCorner(face, index);
            if ("UNKNOWN".equals(cornerName)) {
                continue;
            }

            alignCornerToTargetUIndex(cornerName, face, index);

            applyCornerAlgorithmUntilYellowOnD(cornerName, 5);
        }

        return solveMoves;
    }

    /**
     * Solves F2L middle-layer colored edges using the requested right/left insertion families.
     *
     * Logic:
     * 1) Find a non-white edge on lateral index [1] and odd indices of U.
     * 2) Align lateral [1] color with its matching center and note that face.
     * 3) Turn U or U' away from the edge's two colors, then apply insertion pair:
     *    - If U' : right algorithm of noted face, then left algorithm of right face.
     *    - If U  : left algorithm of noted face, then right algorithm of left face.
     */
    public List<String> solveEdges() {
        List<String> edgeMoves = new ArrayList<>();

        int guard = 0;
        while (guard < 40) {
            guard++;

            int[] candidate = findTopNonWhiteEdgeCandidate();
            if (candidate == null) {
                // No insertable top edge found: verify middle layer is really solved.
                if (areMiddleLayerEdgesSolved()) {
                    break;
                }

                // Eject one mis-slotted middle edge to U using:
                // right algorithm of face1, then left algorithm of face2 (face2 = right of face1).
                int face1 = findUnsolvedMiddleEdgeFaceOnRight();
                if (face1 == -1) {
                    break;
                }
                int face2 = rightFace(face1);

                applyMoveAndCollect(edgeMoves, rightAlgorithmForFace(face1));
                applyMoveAndCollect(edgeMoves, leftAlgorithmForFace(face2));
                continue;
            }

            int currentFace = candidate[0];
            char sideColor = (char) candidate[2];
            char uColor = (char) candidate[3];

            int targetFace = findSideFaceByCenterColor(sideColor);
            if (targetFace == -1) {
                break;
            }

            // Step 2: align edge so side index [1] color matches its center face.
            int rotationDir = alignTopEdgeToFace(currentFace, sideColor, targetFace, edgeMoves);

            // Re-read other edge color from U after alignment.
            char[][] matrix = cube.getMatrix();
            int alignedUIndex = uIndexForSideFace(targetFace);
            char otherColor = matrix[RubiksCube.U][alignedUIndex];
            int otherFace = findSideFaceByCenterColor(otherColor);

            // Decide U or U' according to side relation (away from the two edge colors).
            int right = rightFace(targetFace);
            int left = leftFace(targetFace);

            if (otherFace == right) {
                // Rule: if U-edge color matches right-center -> do U.
                applyMoveAndCollect(edgeMoves, "U");
                applyMoveAndCollect(edgeMoves, rightAlgorithmForFace(targetFace));
                applyMoveAndCollect(edgeMoves, leftAlgorithmForFace(right));
            } else if (otherFace == left) {
                // Rule: if U-edge color matches left-center -> do U'.
                applyMoveAndCollect(edgeMoves, "U'");
                applyMoveAndCollect(edgeMoves, leftAlgorithmForFace(targetFace));
                applyMoveAndCollect(edgeMoves, rightAlgorithmForFace(left));
            } else {
                // Fallback for unusual states: keep progress with opposite branch from alignment direction.
                if (rotationDir <= 0) {
                    applyMoveAndCollect(edgeMoves, "U'");
                    applyMoveAndCollect(edgeMoves, leftAlgorithmForFace(targetFace));
                    applyMoveAndCollect(edgeMoves, rightAlgorithmForFace(left));
                } else {
                    applyMoveAndCollect(edgeMoves, "U");
                    applyMoveAndCollect(edgeMoves, rightAlgorithmForFace(targetFace));
                    applyMoveAndCollect(edgeMoves, leftAlgorithmForFace(right));
                }
            }
        }
        return edgeMoves;
    }

    private boolean areMiddleLayerEdgesSolved() {
        char[][] matrix = cube.getMatrix();
        int[] sideFaces = {RubiksCube.F, RubiksCube.R, RubiksCube.B, RubiksCube.L};

        for (int face : sideFaces) {
            char center = matrix[face][4];
            if (matrix[face][3] != center || matrix[face][5] != center) {
                return false;
            }
        }
        return true;
    }

    private int findUnsolvedMiddleEdgeFaceOnRight() {
        char[][] matrix = cube.getMatrix();
        int[] sideFaces = {RubiksCube.F, RubiksCube.R, RubiksCube.B, RubiksCube.L};

        // Prefer direct right-slot mismatches [5].
        for (int face : sideFaces) {
            if (matrix[face][5] != matrix[face][4]) {
                return face;
            }
        }

        // If only a left-slot mismatch [3] is visible, the same edge corresponds to
        // the right slot [5] of the left neighbor.
        for (int face : sideFaces) {
            if (matrix[face][3] != matrix[face][4]) {
                return leftFace(face);
            }
        }

        return -1;
    }

    /**
     * Identifies which yellow corner piece this sticker belongs to,
     * based on the 3 colors of the corner where the sticker is currently located.
     * Returns one of: ULF, URF, URB, ULB, or "UNKNOWN".
     */
    public String identifyCorner(int face, int index) {
        String currentSlot = cornerSlotFromPosition(face, index);
        int[] slotTriplet = slotTripletByName(currentSlot);
        if (slotTriplet == null) {
            return "UNKNOWN";
        }

        char[][] matrix = cube.getMatrix();
        String colors = canonicalColors(
                matrix[slotTriplet[0]][slotTriplet[1]],
                matrix[slotTriplet[2]][slotTriplet[3]],
                matrix[slotTriplet[4]][slotTriplet[5]]
        );

        if ("GOY".equals(colors)) {
            return "ULF"; // couleurs YOG
        }
        if ("GRY".equals(colors)) {
            return "URF"; // couleurs YGR
        }
        if ("BRY".equals(colors)) {
            return "URB"; // couleurs YBR
        }
        if ("BOY".equals(colors)) {
            return "ULB"; // couleurs YBO
        }
        return "UNKNOWN";
    }

    /**
     * Finds yellow stickers on the U-corner locations and associated lateral corner locations,
     * then returns yellow corners found with their current sticker position.
     *
     * Returned format examples:
     * - "ULF @ U[6]"
     * - "URB @ B[0]"
     */
    public List<String> findYellowCorners() {
        List<String> yellowCorners = new ArrayList<>();
        char[][] matrix = cube.getMatrix();

        // Scan U corner stickers
        for (int uIndex : U_CORNER_INDICES) {
            if (matrix[RubiksCube.U][uIndex] == 'Y') {
                String cornerName = identifyCorner(RubiksCube.U, uIndex);
                addCornerIfNotPresent(yellowCorners, cornerName, RubiksCube.U, uIndex);
            }
        }

        // Scan lateral corner stickers associated with U-layer corners
        scanCornerTripletForYellow(matrix, yellowCorners, ULF_POS);
        scanCornerTripletForYellow(matrix, yellowCorners, URF_POS);
        scanCornerTripletForYellow(matrix, yellowCorners, URB_POS);
        scanCornerTripletForYellow(matrix, yellowCorners, ULB_POS);

        return yellowCorners;
    }

    private void scanCornerTripletForYellow(char[][] matrix, List<String> yellowCorners, int[] cornerTriplet) {
        addIfYellow(matrix, yellowCorners, cornerTriplet[0], cornerTriplet[1]);
        addIfYellow(matrix, yellowCorners, cornerTriplet[2], cornerTriplet[3]);
        addIfYellow(matrix, yellowCorners, cornerTriplet[4], cornerTriplet[5]);
    }

    private void addIfYellow(char[][] matrix, List<String> yellowCorners, int face, int index) {
        if (matrix[face][index] != 'Y') {
            return;
        }
        String cornerName = identifyCorner(face, index);
        addCornerIfNotPresent(yellowCorners, cornerName, face, index);
    }

    private void addCornerIfNotPresent(List<String> yellowCorners, String cornerName, int face, int index) {
        if ("UNKNOWN".equals(cornerName)) {
            return;
        }
        String entry = cornerName + " @ " + faceName(face) + "[" + index + "]";

        // One entry per corner piece to avoid duplicates while scanning all three stickers.
        for (String existing : yellowCorners) {
            if (existing.startsWith(cornerName + " @ ")) {
                return;
            }
        }
        yellowCorners.add(entry);
    }

    private boolean isPositionInCorner(int face, int index, int[] cornerTriplet) {
        return (cornerTriplet[0] == face && cornerTriplet[1] == index)
                || (cornerTriplet[2] == face && cornerTriplet[3] == index)
                || (cornerTriplet[4] == face && cornerTriplet[5] == index);
    }

    private int[] parseEntryPosition(String entry) {
        // Expected format: "URF @ U[8]"
        int atPos = entry.indexOf("@");
        if (atPos == -1 || atPos + 2 >= entry.length()) {
            return null;
        }

        String pos = entry.substring(atPos + 1).trim(); // U[8]
        if (pos.length() < 4) {
            return null;
        }

        int face = faceFromName(pos.charAt(0));
        int open = pos.indexOf('[');
        int close = pos.indexOf(']');
        if (open == -1 || close == -1 || close <= open + 1) {
            return null;
        }

        try {
            int index = Integer.parseInt(pos.substring(open + 1, close));
            return new int[]{face, index};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int faceFromName(char name) {
        switch (name) {
            case 'U':
                return RubiksCube.U;
            case 'R':
                return RubiksCube.R;
            case 'F':
                return RubiksCube.F;
            case 'D':
                return RubiksCube.D;
            case 'L':
                return RubiksCube.L;
            case 'B':
                return RubiksCube.B;
            default:
                return -1;
        }
    }

    private int targetUIndexForCorner(String cornerName) {
        switch (cornerName) {
            case "ULF":
                return 6;
            case "URF":
                return 8;
            case "URB":
                return 2;
            case "ULB":
                return 0;
            default:
                return -1;
        }
    }

    private void alignCornerToTargetUIndex(String cornerName, int currentFace, int currentIndex) {
        int current = currentUIndexFromPosition(currentFace, currentIndex);
        int target = targetUIndexForCorner(cornerName);
        if (current == -1 || target == -1 || current == target) {
            return;
        }

        // Clockwise U cycle of corner indices: 0 -> 2 -> 8 -> 6 -> 0
        int clockwiseSteps = 0;
        int probe = current;
        while (probe != target && clockwiseSteps < 4) {
            probe = nextClockwiseCornerIndex(probe);
            clockwiseSteps++;
        }

        if (clockwiseSteps == 1) {
            applyMoveAndRecord("U");
        } else if (clockwiseSteps == 2) {
            applyMoveAndRecord("U2");
        } else if (clockwiseSteps == 3) {
            applyMoveAndRecord("U'");
        }
    }

    private int currentUIndexFromPosition(int face, int index) {
        String slot = cornerSlotFromPosition(face, index);
        switch (slot) {
            case "ULF":
                return 6;
            case "URF":
                return 8;
            case "URB":
                return 2;
            case "ULB":
                return 0;
            default:
                return -1;
        }
    }

    private String cornerSlotFromPosition(int face, int index) {
        if (isPositionInCorner(face, index, ULF_POS)) {
            return "ULF";
        }
        if (isPositionInCorner(face, index, URF_POS)) {
            return "URF";
        }
        if (isPositionInCorner(face, index, URB_POS)) {
            return "URB";
        }
        if (isPositionInCorner(face, index, ULB_POS)) {
            return "ULB";
        }
        return "UNKNOWN";
    }

    private int[] slotTripletByName(String slotName) {
        switch (slotName) {
            case "ULF":
                return ULF_POS;
            case "URF":
                return URF_POS;
            case "URB":
                return URB_POS;
            case "ULB":
                return ULB_POS;
            default:
                return null;
        }
    }

    private String canonicalColors(char a, char b, char c) {
        char[] arr = {a, b, c};
        java.util.Arrays.sort(arr);
        return new String(arr);
    }

    private int nextClockwiseCornerIndex(int index) {
        switch (index) {
            case 0:
                return 2;
            case 2:
                return 8;
            case 8:
                return 6;
            case 6:
                return 0;
            default:
                return index;
        }
    }

    private void applyCornerAlgorithmUntilYellowOnD(String cornerName, int maxRepeats) {
        String algorithm;
        int dIndex;

        switch (cornerName) {
            case "ULF":
                algorithm = "L' U' L U";
                dIndex = 0;
                break;
            case "URF":
                algorithm = "R U R' U'";
                dIndex = 2;
                break;
            case "URB":
                algorithm = "R' U' R U";
                dIndex = 8;
                break;
            case "ULB":
                algorithm = "L U L' U'";
                dIndex = 6;
                break;
            default:
                return;
        }

        for (int i = 0; i < maxRepeats; i++) {
            applyMoveAndRecord(algorithm);
            boolean yellowOnTarget = isYellowOnDIndex(dIndex);
            if (yellowOnTarget) {
                return;
            }
        }
    }

    private boolean isYellowOnDIndex(int dIndex) {
        char[][] matrix = cube.getMatrix();
        return matrix[RubiksCube.D][dIndex] == 'Y';
    }

    private boolean areAllYellowCornersSolvedOnD() {
        return isYellowOnDIndex(0)
                && isYellowOnDIndex(2)
                && isYellowOnDIndex(6)
                && isYellowOnDIndex(8);
    }

    private boolean ejectOneMisorientedDCorner() {
        // Priority order follows your D-corner mapping.
        if (!isYellowOnDIndex(0)) {
            applyMoveAndRecord("L' U' L U");
            return true;
        }
        if (!isYellowOnDIndex(2)) {
            applyMoveAndRecord("R U R' U'");
            return true;
        }
        if (!isYellowOnDIndex(8)) {
            applyMoveAndRecord("R' U' R U");
            return true;
        }
        if (!isYellowOnDIndex(6)) {
            applyMoveAndRecord("L U L' U'");
            return true;
        }
        return false;
    }

    private void applyMoveAndRecord(String move) {
        cube.applyAlgorithm(move);
        solveMoves.add(move);
    }

    private void applyMoveAndCollect(List<String> moves, String move) {
        cube.applyAlgorithm(move);
        moves.add(move);
    }

    private int[] findTopNonWhiteEdgeCandidate() {
        // Returns {sideFace, uIndex, sideColor, uColor} or null.
        int[] sideFaces = {RubiksCube.F, RubiksCube.R, RubiksCube.B, RubiksCube.L};
        char[][] matrix = cube.getMatrix();

        for (int sideFace : sideFaces) {
            int uIndex = uIndexForSideFace(sideFace);
            char sideColor = matrix[sideFace][1];
            char uColor = matrix[RubiksCube.U][uIndex];

            // F2L edges must be side colors only (G/B/O/R), excluding W/Y pieces.
            if (isSideColor(sideColor) && isSideColor(uColor)) {
                return new int[]{sideFace, uIndex, sideColor, uColor};
            }
        }
        return null;
    }

    private boolean isSideColor(char color) {
        return color == 'G' || color == 'B' || color == 'O' || color == 'R';
    }

    private int alignTopEdgeToFace(int currentFace, char sideColor, int targetFace, List<String> edgeMoves) {
        // Use the same method as yellow cross case 1: calculate U rotation distance directly
        // and apply the alignment move in one shot, avoiding loops.

        // Calculate how many U rotations needed to align currentFace with targetFace
        int separation = ringDistanceClockwise(currentFace, targetFace);
        String alignmentMove = alignmentMoveForURotation(separation);

        if (!alignmentMove.isEmpty()) {
            applyMoveAndCollect(edgeMoves, alignmentMove);
            return 1;  // Applied a move
        }
        return 0;  // Already aligned (separation == 0)
    }

    private String alignmentMoveForURotation(int separation) {
        switch (separation) {
            case 1:
                return "U'";
            case 2:
                return "U2";
            case 3:
                return "U";
            default:
                return "";
        }
    }

    private int ringDistanceClockwise(int fromFace, int toFace) {
        int from = ringIndex(fromFace);
        int to = ringIndex(toFace);
        if (from == -1 || to == -1) {
            return 0;
        }
        return (to - from + 4) % 4;
    }

    private int ringIndex(int face) {
        switch (face) {
            case RubiksCube.F:
                return 0;
            case RubiksCube.R:
                return 1;
            case RubiksCube.B:
                return 2;
            case RubiksCube.L:
                return 3;
            default:
                return -1;
        }
    }

    private int rightFace(int face) {
        switch (face) {
            case RubiksCube.F:
                return RubiksCube.R;
            case RubiksCube.R:
                return RubiksCube.B;
            case RubiksCube.B:
                return RubiksCube.L;
            case RubiksCube.L:
                return RubiksCube.F;
            default:
                return face;
        }
    }

    private int leftFace(int face) {
        switch (face) {
            case RubiksCube.F:
                return RubiksCube.L;
            case RubiksCube.L:
                return RubiksCube.B;
            case RubiksCube.B:
                return RubiksCube.R;
            case RubiksCube.R:
                return RubiksCube.F;
            default:
                return face;
        }
    }

    private int uIndexForSideFace(int sideFace) {
        switch (sideFace) {
            case RubiksCube.B:
                return 1;
            case RubiksCube.L:
                return 3;
            case RubiksCube.R:
                return 5;
            case RubiksCube.F:
                return 7;
            default:
                return -1;
        }
    }

    private int findSideFaceByCenterColor(char color) {
        char[][] matrix = cube.getMatrix();
        int[] sideFaces = {RubiksCube.F, RubiksCube.R, RubiksCube.B, RubiksCube.L};
        for (int face : sideFaces) {
            if (matrix[face][4] == color) {
                return face;
            }
        }
        return -1;
    }

    private String rightAlgorithmForFace(int face) {
        switch (face) {
            case RubiksCube.F:
                return "R U R' U'";
            case RubiksCube.R:
                return "B U B' U'";
            case RubiksCube.B:
                return "L U L' U'";
            case RubiksCube.L:
                return "F U F' U'";
            default:
                return "";
        }
    }

    private String leftAlgorithmForFace(int face) {
        switch (face) {
            case RubiksCube.F:
                return "L' U' L U";
            case RubiksCube.R:
                return "F' U' F U";
            case RubiksCube.B:
                return "R' U' R U";
            case RubiksCube.L:
                return "B' U' B U";
            default:
                return "";
        }
    }

    private String faceName(int face) {
        switch (face) {
            case RubiksCube.U:
                return "U";
            case RubiksCube.R:
                return "R";
            case RubiksCube.F:
                return "F";
            case RubiksCube.D:
                return "D";
            case RubiksCube.L:
                return "L";
            case RubiksCube.B:
                return "B";
            default:
                return "?";
        }
    }
}
