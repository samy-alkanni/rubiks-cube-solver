import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Represents a 3x3 Rubik's cube using a 6x9 matrix layout.
 *
 * <p>The matrix is organized as follows:
 * <pre>
 *   face 0 (U): row 0
 *   face 1 (R): row 1
 *   face 2 (F): row 2
 *   face 3 (D): row 3
 *   face 4 (L): row 4
 *   face 5 (B): row 5
 * </pre>
 * Each row contains the 9 stickers of that face in reading order (left-to-right, top-to-bottom).
 */
public final class RubiksCube {
    public static final int U = 0;
    public static final int R = 1;
    public static final int F = 2;
    public static final int D = 3;
    public static final int L = 4;
    public static final int B = 5;

    private final char[][] stickers;
    private final List<String> moveHistory = new ArrayList<>();
    private final Random random = new Random();

    /**
     * Creates a solved cube.
     */
    public RubiksCube() {
        stickers = new char[6][9];
        reset();
    }

    /**
     * Returns a deep copy of the internal 6x9 representation.
     */
    public char[][] getMatrix() {
        char[][] copy = new char[6][9];
        for (int i = 0; i < 6; i++) {
            System.arraycopy(stickers[i], 0, copy[i], 0, 9);
        }
        return copy;
    }

    /**
     * Sets all 9 stickers of a single face.
     *
     * @param faceIndex the face index (0-5, corresponding to U, R, F, D, L, B)
     * @param faceData 9-character array in reading order (left-to-right, top-to-bottom)
     */
    public void setFace(int faceIndex, char[] faceData) {
        if (faceIndex < 0 || faceIndex > 5) {
            throw new IllegalArgumentException("Invalid face index: " + faceIndex);
        }
        if (faceData == null || faceData.length != 9) {
            throw new IllegalArgumentException("Face data must be exactly 9 characters");
        }
        System.arraycopy(faceData, 0, stickers[faceIndex], 0, 9);
    }

    /**
     * Returns the face index (0-5) for a move character or throws for invalid values.
     */
    public static int faceIndex(char face) {
        switch (face) {
            case 'U':
                return U;
            case 'R':
                return R;
            case 'F':
                return F;
            case 'D':
                return D;
            case 'L':
                return L;
            case 'B':
                return B;
            default:
                throw new IllegalArgumentException("Unknown face: " + face);
        }
    }

    /**
     * Resets the cube to the solved state.
     */
    public void reset() {
        stickers[U] = fillFace('W');
        stickers[R] = fillFace('R');
        stickers[F] = fillFace('G');
        stickers[D] = fillFace('Y');
        stickers[L] = fillFace('O');
        stickers[B] = fillFace('B');
        moveHistory.clear();
    }

    /**
     * Clears the internal move history without changing cube state.
     */
    public void clearHistory() {
        moveHistory.clear();
    }

    private char[] fillFace(char fill) {
        char[] face = new char[9];
        Arrays.fill(face, fill);
        return face;
    }

    /**
     * Applies a sequence of moves described using standard cube notation (e.g. "R U R' U'").
     */
    public void applyAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            return;
        }
        String[] tokens = algorithm.trim().split("\\s+");
        for (String token : tokens) {
            move(token);
        }
    }

    /**
     * Simplifie une séquence de coups en compactant les occurrences consécutives
     * sur la même face (ex: U U -> U2, U U U -> U', U U' -> supprimé).
     *
     * <p>ATTENTION: la liste doit contenir des coups unitaires (tokens), pas des
     * algorithmes multi-tokens du type "R U R'".
     */
    public static List<String> simplifyMoves(List<String> moves) {
        List<String> simplified = new ArrayList<>();
        if (moves == null) {
            return simplified;
        }

        for (String move : moves) {
            if (move == null || move.trim().isEmpty()) {
                continue;
            }

            String normalizedMove = normalizeMove(move);
            if (normalizedMove.isEmpty()) {
                continue;
            }

            if (simplified.isEmpty()) {
                simplified.add(normalizedMove);
                continue;
            }

            String lastMove = simplified.get(simplified.size() - 1);
            if (sameFace(lastMove, normalizedMove)) {
                simplified.remove(simplified.size() - 1);

                int combinedQuarterTurns = (moveQuarterTurns(lastMove) + moveQuarterTurns(normalizedMove)) % 4;
                if (combinedQuarterTurns == 1) {
                    simplified.add(String.valueOf(faceLetter(lastMove)));
                } else if (combinedQuarterTurns == 2) {
                    simplified.add(String.valueOf(faceLetter(lastMove)) + "2");
                } else if (combinedQuarterTurns == 3) {
                    simplified.add(String.valueOf(faceLetter(lastMove)) + "'");
                }

                continue;
            }

            simplified.add(normalizedMove);
        }

        return simplified;
    }

    private static String normalizeMove(String move) {
        if (move == null) {
            return "";
        }

        String normalized = move.trim().toUpperCase();
        normalized = normalized.replace('\u2019', '\''); // apostrophe typographique

        if (normalized.isEmpty()) {
            return "";
        }

        // Supporte U, U', U2, et tolère U2' / U'2 (équivalent à U2)
        char face = normalized.charAt(0);
        if ("URFDLB".indexOf(face) < 0) {
            return "";
        }

        String suffix = normalized.substring(1);
        suffix = suffix.replace(" ", "");
        if (suffix.isEmpty()) {
            return String.valueOf(face);
        }
        if (suffix.equals("'")) {
            return String.valueOf(face) + "'";
        }
        if (suffix.equals("2") || suffix.equals("2'") || suffix.equals("'2")) {
            return String.valueOf(face) + "2";
        }

        return "";
    }

    private static boolean sameFace(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return faceLetter(a) == faceLetter(b);
    }

    private static int moveQuarterTurns(String move) {
        if (move == null) {
            return 0;
        }
        if (move.endsWith("2")) {
            return 2;
        }
        if (move.endsWith("'")) {
            return 3;
        }
        return 1;
    }

    private static char faceLetter(String move) {
        return move.charAt(0);
    }

    /**
     * Applies a single move. Supported moves: U/D/L/R/F/B with optional prime (') and 2 (double) suffix.
     */
    public void move(String move) {
        if (move == null || move.trim().isEmpty()) {
            return;
        }

        String normalized = move.trim().toUpperCase();
        boolean doubleTurn = normalized.endsWith("2");
        boolean prime = normalized.endsWith("'");

        if (doubleTurn) {
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (prime) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        for (int i = 0; i < (doubleTurn ? 2 : 1); i++) {
            applyFaceMove(normalized, !prime);
        }

        moveHistory.add(move);
    }

    /**
     * Scrambles the cube with a random sequence of moves.
     *
     * @param numMoves the number of random moves to apply
     * @return the list of moves applied during scrambling
     */
    public List<String> scramble(int numMoves) {
        List<String> scrambleMoves = new ArrayList<>();
        String[] possibleMoves = {"U", "U'", "U2", "D", "D'", "D2", "L", "L'", "L2", "R", "R'", "R2", "F", "F'", "F2", "B", "B'", "B2"};
        for (int i = 0; i < numMoves; i++) {
            String randomMove = possibleMoves[random.nextInt(possibleMoves.length)];
            move(randomMove);
            scrambleMoves.add(randomMove);
        }
        return scrambleMoves;
    }

    private String inverseMove(String move) {
        if (move.endsWith("'")) {
            return move.substring(0, move.length() - 1);
        } else if (move.endsWith("2")) {
            return move;
        } else {
            return move + "'";
        }
    }

    /**
     * Solves the cube by applying the inverse of the moves in reverse order.
     * This works only if the scramble was done via the scramble() method or recorded moves.
     */
    public List<String> solve() {
        List<String> solveMoves = new ArrayList<>();
        for (int i = moveHistory.size() - 1; i >= 0; i--) {
            String invMove = inverseMove(moveHistory.get(i));
            applyFaceMoveInternal(invMove);
            solveMoves.add(invMove);
        }
        moveHistory.clear();
        return solveMoves;
    }

    private void applyFaceMoveInternal(String move) {
        String normalized = move.toUpperCase();
        boolean doubleTurn = normalized.endsWith("2");
        boolean prime = normalized.endsWith("'");

        if (doubleTurn) {
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (prime) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        for (int i = 0; i < (doubleTurn ? 2 : 1); i++) {
            applyFaceMove(normalized, !prime);
        }
    }

    private void applyFaceMove(String face, boolean clockwise) {
        switch (face) {
            case "U":
                applyU(clockwise);
                break;
            case "D":
                applyD(clockwise);
                break;
            case "L":
                applyL(clockwise);
                break;
            case "R":
                applyR(clockwise);
                break;
            case "F":
                applyF(clockwise);
                break;
            case "B":
                applyB(clockwise);
                break;
            default:
                throw new IllegalArgumentException("Unknown move: " + face);
        }
    }

    public boolean isSolved() {
        return Arrays.equals(stickers[U], fillFace('W'))
                && Arrays.equals(stickers[R], fillFace('R'))
                && Arrays.equals(stickers[F], fillFace('G'))
                && Arrays.equals(stickers[D], fillFace('Y'))
                && Arrays.equals(stickers[L], fillFace('O'))
                && Arrays.equals(stickers[B], fillFace('B'));
    }

    private void rotateFace(int face, boolean clockwise) {
        // Positions for a 3x3 face in row-major order:
        // 0 1 2
        // 3 4 5
        // 6 7 8
        int[] indices = {
                0, 1, 2,
                5, 8, 7,
                6, 3
        };
        char[] copy = stickers[face].clone();
        for (int i = 0; i < indices.length; i++) {
            int from = indices[i];
            int to = indices[(i + (clockwise ? 2 : indices.length - 2)) % indices.length];
            stickers[face][to] = copy[from];
        }
    }

    private void cyclePositions(int[][] positions, boolean clockwise) {
        char[] buffer = new char[positions.length];
        for (int i = 0; i < positions.length; i++) {
            int face = positions[i][0];
            int index = positions[i][1];
            buffer[i] = stickers[face][index];
        }

        int shift = clockwise ? positions.length - 3 : 3;

        for (int i = 0; i < positions.length; i++) {
            int target = (i + shift) % positions.length;
            int face = positions[target][0];
            int index = positions[target][1];
            stickers[face][index] = buffer[i];
        }
    }

    private void applyU(boolean clockwise) {
        rotateFace(U, clockwise);
        cyclePositions(new int[][]{
                {F, 0}, {F, 1}, {F, 2},
                {R, 0}, {R, 1}, {R, 2},
                {B, 0}, {B, 1}, {B, 2},
                {L, 0}, {L, 1}, {L, 2}
        }, clockwise);
    }

    private void applyD(boolean clockwise) {
        rotateFace(D, clockwise);
        cyclePositions(new int[][]{
                {F, 6}, {F, 7}, {F, 8},
                {L, 6}, {L, 7}, {L, 8},
                {B, 6}, {B, 7}, {B, 8},
                {R, 6}, {R, 7}, {R, 8}
        }, clockwise);
    }

    private void applyL(boolean clockwise) {
        rotateFace(L, clockwise);
        cyclePositions(new int[][]{
                {U, 0}, {U, 3}, {U, 6},
                {B, 8}, {B, 5}, {B, 2},
                {D, 0}, {D, 3}, {D, 6},
                {F, 0}, {F, 3}, {F, 6}
        }, clockwise);
    }

    private void applyR(boolean clockwise) {
        rotateFace(R, clockwise);
        cyclePositions(new int[][]{
                {U, 2}, {U, 5}, {U, 8},
                {F, 2}, {F, 5}, {F, 8},
                {D, 2}, {D, 5}, {D, 8},
                {B, 6}, {B, 3}, {B, 0}
        }, clockwise);
    }

    private void applyF(boolean clockwise) {
        rotateFace(F, clockwise);
        cyclePositions(new int[][]{
                {U, 6}, {U, 7}, {U, 8},
                {L, 8}, {L, 5}, {L, 2},
                {D, 2}, {D, 1}, {D, 0},
                {R, 0}, {R, 3}, {R, 6}
        }, clockwise);
    }

    private void applyB(boolean clockwise) {
        rotateFace(B, clockwise);
        cyclePositions(new int[][]{
                {U, 2}, {U, 1}, {U, 0},
                {R, 8}, {R, 5}, {R, 2},
                {D, 6}, {D, 7}, {D, 8},
                {L, 0}, {L, 3}, {L, 6}
        }, clockwise);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rubik's Cube (6x9 matrix)\n");
        for (int face = 0; face < 6; face++) {
            sb.append(String.format("Face %d: ", face));
            for (int i = 0; i < 9; i++) {
                sb.append(stickers[face][i]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
