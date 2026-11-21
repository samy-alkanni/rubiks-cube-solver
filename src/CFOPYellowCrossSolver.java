import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the CFOP yellow cross (first layer cross) of the Rubik's cube.
 * 
 * Convention:
 * - U (face 0) = White (W)
 * - D (face 3) = Yellow (Y) - the bottom/first layer
 * - F (face 2) = Green (G)
 * 
 * The yellow cross is built by recognizing yellow edges on the cube and placing them
 * in the correct positions on the D face.
 * 
 * The algorithm handles 4 cases:
 * - Case 1: Yellow edge on U face
 * - Case 2: Yellow edge on [3] or [5] of side faces
 * - Case 3: Yellow edge on [1] or [7] of side faces
 * - Case 4: Yellow edge on D face but incorrectly placed + final verification
 */
public class CFOPYellowCrossSolver {
    private final RubiksCube cube;
    private final List<String> solveMoves;
    private static final int[] U_EDGE_INDICES = {1, 3, 5, 7};
    private static final int[] SIDE_RING_ORDER = {RubiksCube.B, RubiksCube.R, RubiksCube.F, RubiksCube.L};

    public CFOPYellowCrossSolver(RubiksCube cube) {
        this.cube = cube;
        this.solveMoves = new ArrayList<>();
    }

    /**
     * Generates a test state by scrambling the cube from a solved state.
     * @param numScrambleMoves the number of random moves to apply
     * @return the list of scramble moves applied
     */
    public List<String> generateTestState(int numScrambleMoves) {
        cube.reset();
        return cube.scramble(numScrambleMoves);
    }

    /**
     * Solves the yellow cross on the D face.
     * Runs cases 1->4 with a bounded retry loop until the yellow cross is solved.
     * @return the list of moves applied to solve the yellow cross
     */
    public List<String> solveYellowCross() {
        solveMoves.clear();

        for (int attempt = 0; attempt < 5; attempt++) {
            if (isYellowCrossSolved()) {
                break;
            }

            solveCase1_YellowEdgesOnU();
            solveCase2_YellowEdgesOnSideFaces(-1);
            solveCase3_YellowEdgesOnSideFacesTopBottom();
            solveCase4_YellowEdgesOnDWrongCenter();
        }
        
        return solveMoves;
    }

    /**
     * Case 1: Resolve yellow edges found on the U face.
     * 
     * Logic:
     * - Scan U face for yellow edges at indices [1], [3], [5], [7]
     * - For each yellow edge found:
     *   - Determine which side face it should belong to based on its U position:
     *     * U[1] → should go to B (Back) center
     *     * U[3] → should go to L (Left) center
     *     * U[5] → should go to R (Right) center
     *     * U[7] → should go to F (Front) center
     *   - Find the color of the edge at the opposite side (e.g., if edge is at U[1], 
     *     check B[1] to find which side face has the matching color)
     *   - Align the matching color center with the edge by applying U or U' moves
     *   - Apply the double move (F2, B2, R2, or L2) to place the edge on the D face
     */
    private void solveCase1_YellowEdgesOnU() {
        // On traite au plus 4 edges jaunes sur U (un par branche de la croix)
        for (int processed = 0; processed < 4; processed++) {
            char[][] matrix = cube.getMatrix();
            int yellowUIndex = -1;

            for (int index : U_EDGE_INDICES) {
                char sticker = matrix[RubiksCube.U][index];
                if (sticker == 'Y') {
                    yellowUIndex = index;
                    break;
                }
            }

            if (yellowUIndex == -1) {
                return;
            }

            int currentSideFace = sideFaceFromUEdgeIndex(yellowUIndex);
            char edgeSideColor = matrix[currentSideFace][1];

            int targetFace = findSideFaceByCenterColor(matrix, edgeSideColor);

            int separation = ringDistanceClockwise(currentSideFace, targetFace);
            String alignmentMove = alignmentMoveForSeparation(separation);
            if (!alignmentMove.isEmpty()) {
                applyMoveAndRecord(alignmentMove);
            }

            String insertionMove = faceName(targetFace) + "2";
            applyMoveAndRecord(insertionMove);
        }
    }

    /**
     * Case 2: Yellow edge on side faces F, L, R, B at indices [3] or [5].
     *
     * Logic:
     * 1) Find a yellow edge on side faces at [3] or [5].
     *    If priorityFace is specified (from Case 3), check that face first.
     * 2) Get the other edge color from the adjacent face:
     *    - If yellow at [3], read left face [5]
     *    - If yellow at [5], read right face [3]
     * 3) Compute setup move on D using distance (B->R->F->L->B):
     *    - 0: none, 1: D', 2: D2, 3: D
     * 4) Turn the initial color face:
     *    - color index [3] => prime
     *    - color index [5] => normal
     * 5) Undo setup move from step 3.
     * 
     * @param priorityFace if >= 0, this face (F, L, R, B) is checked first for yellow edges.
     *                     Used by Case 3 to ensure the piece it just moved is solved first.
     *                     If -1, no priority (normal scanning order).
     */
    private void solveCase2_YellowEdgesOnSideFaces(int priorityFace) {
        int[] scanFaces = {RubiksCube.F, RubiksCube.L, RubiksCube.R, RubiksCube.B};

        // Bounded loop: each pass inserts at least one side yellow edge toward D.
        for (int processed = 0; processed < 8; processed++) {
            char[][] matrix = cube.getMatrix();

            int yellowFace = -1;
            int yellowIndex = -1;

            // If priorityFace is specified, check it first
            if (priorityFace >= 0) {
                if (matrix[priorityFace][3] == 'Y') {
                    yellowFace = priorityFace;
                    yellowIndex = 3;
                } else if (matrix[priorityFace][5] == 'Y') {
                    yellowFace = priorityFace;
                    yellowIndex = 5;
                }
            }

            // If no yellow found at priority face, scan normally
            if (yellowFace == -1) {
                for (int face : scanFaces) {
                    if (matrix[face][3] == 'Y') {
                        yellowFace = face;
                        yellowIndex = 3;
                        break;
                    }
                    if (matrix[face][5] == 'Y') {
                        yellowFace = face;
                        yellowIndex = 5;
                        break;
                    }
                }
            }

            if (yellowFace == -1) {
                return;
            }

            int colorFace;
            int colorIndex;
            if (yellowIndex == 3) {
                colorFace = leftFace(yellowFace);
                colorIndex = 5;
            } else {
                colorFace = rightFace(yellowFace);
                colorIndex = 3;
            }

            char edgeOtherColor = matrix[colorFace][colorIndex];
            int finalFace = findSideFaceByCenterColor(matrix, edgeOtherColor);

            int setupDistance = ringDistanceClockwise(colorFace, finalFace);
            String setupMove = setupMoveForCase2Distance(setupDistance);
            if (!setupMove.isEmpty()) {
                applyMoveAndRecord(setupMove);
            }

            String insertMove = faceName(colorFace) + (colorIndex == 3 ? "'" : "");
            applyMoveAndRecord(insertMove);

            String undoSetup = inverseMove(setupMove);
            if (!undoSetup.isEmpty()) {
                applyMoveAndRecord(undoSetup);
            }
        }
    }

    /**
     * Case 3: Yellow edge on side faces F, L, R, B at indices [1] or [7].
     *
     * Logic requested:
     * 1) Find the face of the non-yellow color (same concept as case 2).
     * 2) Rotate that face clockwise once.
     * 3) Re-run case 2.
     */
    private void solveCase3_YellowEdgesOnSideFacesTopBottom() {
        int[] scanFaces = {RubiksCube.F, RubiksCube.L, RubiksCube.R, RubiksCube.B};

        // Bounded loop to avoid infinite processing in unusual states.
        for (int processed = 0; processed < 8; processed++) {
            char[][] matrix = cube.getMatrix();

            int yellowFace = -1;
            int yellowIndex = -1;

            for (int face : scanFaces) {
                if (matrix[face][1] == 'Y') {
                    yellowFace = face;
                    yellowIndex = 1;
                    break;
                }
                if (matrix[face][7] == 'Y') {
                    yellowFace = face;
                    yellowIndex = 7;
                    break;
                }
            }

            if (yellowFace == -1) {
                return;
            }

            char otherColor;
            int faceToTurn;
            if (yellowIndex == 1) {
                int uIndex = uIndexForSideTopEdge(yellowFace);
                otherColor = matrix[RubiksCube.U][uIndex];
                faceToTurn = sideFaceFromUEdgeIndex(uIndex);
            } else {
                int dIndex = dIndexForSideBottomEdge(yellowFace);
                otherColor = matrix[RubiksCube.D][dIndex];
                faceToTurn = sideFaceFromDEdgeIndex(dIndex);
            }

            String moveStr = faceName(faceToTurn);
            applyMoveAndRecord(moveStr);

            // After creating a side [3]/[5] situation, call case 2 with faceToTurn as priority
            // so Case 2 solves the piece we just moved first, avoiding infinite loops.
            solveCase2_YellowEdgesOnSideFaces(faceToTurn);
        }
    }

    /**
     * Case 4: Yellow edge on D but not matched with the correct side center.
     *
     * Logic:
     * 1) Inspect D edges [1,3,5,7], mapped side faces: [1]=F, [3]=L, [5]=R, [7]=B.
     * 2) Compare side face [7] with center [4]. If mismatch, do side face double turn
     *    (F2/L2/R2/B2) to bring that yellow edge to U.
     * 3) Re-run case 1 for reinsertion.
     */
    private void solveCase4_YellowEdgesOnDWrongCenter() {
        int[] dIndices = {1, 3, 5, 7};

        for (int processed = 0; processed < 4; processed++) {
            char[][] matrix = cube.getMatrix();
            boolean movedAny = false;

            for (int dIndex : dIndices) {
                if (matrix[RubiksCube.D][dIndex] != 'Y') {
                    continue;
                }

                int sideFace = sideFaceFromDEdgeIndex(dIndex);
                char edgeColorOnSide = matrix[sideFace][7];
                char centerColor = matrix[sideFace][4];

                if (edgeColorOnSide != centerColor) {
                    String move = faceName(sideFace) + "2";
                    applyMoveAndRecord(move);
                    solveCase1_YellowEdgesOnU();
                    movedAny = true;
                    break;
                }
            }

            if (!movedAny) {
                return;
            }
        }
    }

    private boolean isYellowCrossSolved() {
        char[][] matrix = cube.getMatrix();
        int[] dIndices = {1, 3, 5, 7};

        for (int dIndex : dIndices) {
            if (matrix[RubiksCube.D][dIndex] != 'Y') {
                return false;
            }
        }

        int[] sideFaces = {RubiksCube.F, RubiksCube.L, RubiksCube.R, RubiksCube.B};
        for (int face : sideFaces) {
            if (matrix[face][7] != matrix[face][4]) {
                return false;
            }
        }

        return true;
    }

    private int sideFaceFromDEdgeIndex(int dIndex) {
        switch (dIndex) {
            case 1:
                return RubiksCube.F;
            case 3:
                return RubiksCube.L;
            case 5:
                return RubiksCube.R;
            case 7:
                return RubiksCube.B;
            default:
                throw new IllegalArgumentException("Index D invalide pour un edge: " + dIndex);
        }
    }

    private int uIndexForSideTopEdge(int sideFace) {
        switch (sideFace) {
            case RubiksCube.F:
                return 7;
            case RubiksCube.R:
                return 5;
            case RubiksCube.B:
                return 1;
            case RubiksCube.L:
                return 3;
            default:
                throw new IllegalArgumentException("Face latérale invalide: " + sideFace);
        }
    }

    private int dIndexForSideBottomEdge(int sideFace) {
        switch (sideFace) {
            case RubiksCube.F:
                return 1;
            case RubiksCube.R:
                return 5;
            case RubiksCube.B:
                return 7;
            case RubiksCube.L:
                return 3;
            default:
                throw new IllegalArgumentException("Face latérale invalide: " + sideFace);
        }
    }

    private int sideFaceFromUEdgeIndex(int uIndex) {
        switch (uIndex) {
            case 1:
                return RubiksCube.B;
            case 3:
                return RubiksCube.L;
            case 5:
                return RubiksCube.R;
            case 7:
                return RubiksCube.F;
            default:
                throw new IllegalArgumentException("Index U invalide pour un edge: " + uIndex);
        }
    }

    private int findSideFaceByCenterColor(char[][] matrix, char color) {
        for (int face : SIDE_RING_ORDER) {
            if (matrix[face][4] == color) {
                return face;
            }
        }
        throw new IllegalStateException("Aucun centre latéral trouvé pour la couleur: " + color);
    }

    private int ringDistanceClockwise(int fromFace, int toFace) {
        int from = ringIndex(fromFace);
        int to = ringIndex(toFace);
        return (to - from + 4) % 4;
    }

    private String alignmentMoveForSeparation(int separation) {
        switch (separation) {
            case 1:
                return "U";
            case 2:
                return "U2";
            case 3:
                return "U'";
            default:
                return "";
        }
    }

    private String setupMoveForCase2Distance(int distance) {
        switch (distance) {
            case 1:
                return "D";
            case 2:
                return "D2";
            case 3:
                return "D'";
            default:
                return "";
        }
    }

    private String inverseMove(String move) {
        if (move == null || move.isEmpty()) {
            return "";
        }
        if (move.endsWith("'")) {
            return move.substring(0, move.length() - 1);
        }
        if (move.endsWith("2")) {
            return move;
        }
        return move + "'";
    }

    private int leftFace(int face) {
        int index = ringIndex(face);
        return SIDE_RING_ORDER[(index + 1) % 4];
    }

    private int rightFace(int face) {
        int index = ringIndex(face);
        int next = (index - 1 + 4) % 4;
        return SIDE_RING_ORDER[next];
    }

    private int ringIndex(int face) {
        for (int i = 0; i < SIDE_RING_ORDER.length; i++) {
            if (SIDE_RING_ORDER[i] == face) {
                return i;
            }
        }
        throw new IllegalArgumentException("Face hors anneau latéral: " + face);
    }

    private String faceName(int face) {
        return String.valueOf("URFDLB".charAt(face));
    }

    /**
     * Applies a move and records it in the solve moves list.
     * @param moveStr the move command (e.g., "U", "F2", "R'")
     */
    private void applyMoveAndRecord(String moveStr) {
        cube.move(moveStr);
        solveMoves.add(moveStr);
    }

    /**
     * Returns the list of moves applied during solving.
     * @return the solve moves list
     */
    public List<String> getSolveMoves() {
        return new ArrayList<>(solveMoves);
    }
}
