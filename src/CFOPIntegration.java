import java.util.ArrayList;
import java.util.List;

public class CFOPIntegration {

    public static final class EtapeCFOP {
        public final String nom;
        public final List<String> coups; // coups individuels (tokens)

        EtapeCFOP(String nom, List<String> coups) {
            this.nom = nom;
            this.coups = coups;
        }
    }

    /**
     * Mapping des indices de faces:
     *   Cube (projet 3D): F=0, R=1, B=2, L=3, U=4, D=5
     *   RubiksCube (CFOP): U=0, R=1, F=2, D=3, L=4, B=5
     *
     * Conversion Cube → RubiksCube:
     *   projet[0] (F) → cfop[2] (F)
     *   projet[1] (R) → cfop[1] (R)
     *   projet[2] (B) → cfop[5] (B)
     *   projet[3] (L) → cfop[4] (L)
     *   projet[4] (U) → cfop[0] (U)
     *   projet[5] (D) → cfop[3] (D)
     */
    private static final int[] PROJET_TO_CFOP = {2, 1, 5, 4, 0, 3};

    /**
     * Conversion inverse RubiksCube → Cube:
     *   cfop[0] (U) → projet[4]
     *   cfop[1] (R) → projet[1]
     *   cfop[2] (F) → projet[0]
     *   cfop[3] (D) → projet[5]
     *   cfop[4] (L) → projet[3]
     *   cfop[5] (B) → projet[2]
     */
    private static final int[] CFOP_TO_PROJET = {4, 1, 0, 5, 3, 2};

    /**
     * Convertit le RubiksCube scanné (format CFOP 6x9 flat)
     * vers le format projet (char[6][3][3]).
     *
     * Le format flat 6x9 est en lecture ordre ligne par ligne:
     * index 0=ligne0col0, 1=ligne0col1, 2=ligne0col2,
     *       3=ligne1col0, 4=ligne1col1, 5=ligne1col2,
     *       6=ligne2col0, 7=ligne2col1, 8=ligne2col2
     */
    public static char[][][] convertirRubiksCubeVersFacesProjet(RubiksCube cubeImages) {
        char[][] cfopMatrix = cubeImages.getMatrix(); // [6][9] flat
        char[][][] facesProjet = new char[6][3][3];

        for (int cfopFace = 0; cfopFace < 6; cfopFace++) {
            int projetFace = CFOP_TO_PROJET[cfopFace];
            for (int i = 0; i < 9; i++) {
                int row = i / 3;
                int col = i % 3;
                facesProjet[projetFace][row][col] = cfopMatrix[cfopFace][i];
            }
        }

        return facesProjet;
    }

    /**
     * Convertit le Cube projet (char[6][3][3]) vers un RubiksCube CFOP (6x9 flat)
     * pour pouvoir lancer les solveurs CFOP.
     */
    private static RubiksCube convertirCubeProjetVersRubiksCube(Cube cube) {
        char[][][] facesProjet = cube.getFaces(); // [6][3][3]
        RubiksCube cfopCube = new RubiksCube();

        for (int projetFace = 0; projetFace < 6; projetFace++) {
            int cfopFace = PROJET_TO_CFOP[projetFace];
            char[] flat = new char[9];
            for (int i = 0; i < 9; i++) {
                flat[i] = facesProjet[projetFace][i / 3][i % 3];
            }
            cfopCube.setFace(cfopFace, flat);
        }

        return cfopCube;
    }

    /**
     * Génère la solution CFOP complète pour le Cube projet donné.
     * Retourne la liste de tous les coups à jouer.
     */
    public static List<String> genererSolutionCFOP(Cube cube) {
        RubiksCube cfopCube = convertirCubeProjetVersRubiksCube(cube);

        List<String> solution = new ArrayList<>();

        CFOPYellowCrossSolver yellowCross = new CFOPYellowCrossSolver(cfopCube);
        solution.addAll(yellowCross.solveYellowCross());

        F2LCornerSolver f2l = new F2LCornerSolver(cfopCube);
        solution.addAll(f2l.solve());
        solution.addAll(f2l.solveEdges());

        OLL2LookCrossSolver oll = new OLL2LookCrossSolver(cfopCube);
        solution.addAll(oll.solveCross());
        solution.addAll(oll.solveCorners());

        PLL2LookSolver pll = new PLL2LookSolver(cfopCube);
        solution.addAll(pll.solveCorners());
        solution.addAll(pll.solveEdges());

        // Les solveurs peuvent renvoyer des algorithmes multi-tokens: on aplatit puis on simplifie.
        return RubiksCube.simplifyMoves(aplatirCoups(solution));
    }

    /**
     * Génère les étapes CFOP (Croix/F2L/OLL/PLL) avec des coups aplatis (tokens).
     * Utile pour l'UI (afficher l'étape en cours, animation coup par coup).
     */
    public static List<EtapeCFOP> genererEtapesCFOP(Cube cube) {
        RubiksCube cfopCube = convertirCubeProjetVersRubiksCube(cube);
        List<EtapeCFOP> etapes = new ArrayList<>();

        CFOPYellowCrossSolver yellowCross = new CFOPYellowCrossSolver(cfopCube);
        etapes.add(new EtapeCFOP("Croix", RubiksCube.simplifyMoves(aplatirCoups(yellowCross.solveYellowCross()))));

        F2LCornerSolver f2l = new F2LCornerSolver(cfopCube);
        List<String> f2lRaw = new ArrayList<>();
        f2lRaw.addAll(f2l.solve());
        f2lRaw.addAll(f2l.solveEdges());
        etapes.add(new EtapeCFOP("F2L", RubiksCube.simplifyMoves(aplatirCoups(f2lRaw))));

        OLL2LookCrossSolver oll = new OLL2LookCrossSolver(cfopCube);
        List<String> ollRaw = new ArrayList<>();
        ollRaw.addAll(oll.solveCross());
        ollRaw.addAll(oll.solveCorners());
        etapes.add(new EtapeCFOP("OLL", RubiksCube.simplifyMoves(aplatirCoups(ollRaw))));

        PLL2LookSolver pll = new PLL2LookSolver(cfopCube);
        List<String> pllRaw = new ArrayList<>();
        pllRaw.addAll(pll.solveCorners());
        pllRaw.addAll(pll.solveEdges());
        etapes.add(new EtapeCFOP("PLL", RubiksCube.simplifyMoves(aplatirCoups(pllRaw))));

        // Compacte aussi les doublons à la jonction entre étapes (ex: ... U | U ...).
        simplifierTransitionsEntreEtapes(etapes);

        return etapes;
    }

    private static void simplifierTransitionsEntreEtapes(List<EtapeCFOP> etapes) {
        if (etapes == null || etapes.size() < 2) {
            return;
        }

        for (int i = 0; i < etapes.size() - 1; i++) {
            List<String> gauche = etapes.get(i).coups;
            List<String> droite = etapes.get(i + 1).coups;
            if (gauche == null || droite == null) {
                continue;
            }

            // Tant que la fin de l'étape i et le début de l'étape i+1 sont sur la même face,
            // on les fusionne via simplifyMoves([last, first]).
            while (!gauche.isEmpty() && !droite.isEmpty()) {
                String last = gauche.get(gauche.size() - 1);
                String first = droite.get(0);
                if (last == null || first == null || last.isEmpty() || first.isEmpty()) {
                    break;
                }
                if (Character.toUpperCase(last.charAt(0)) != Character.toUpperCase(first.charAt(0))) {
                    break;
                }

                List<String> combined = RubiksCube.simplifyMoves(java.util.Arrays.asList(last, first));
                gauche.remove(gauche.size() - 1);
                droite.remove(0);

                if (!combined.isEmpty()) {
                    droite.add(0, combined.get(0));
                }
            }
        }
    }

    private static List<String> aplatirCoups(List<String> rawMoves) {
        List<String> flat = new ArrayList<>();
        if (rawMoves == null) {
            return flat;
        }

        for (String move : rawMoves) {
            if (move == null) {
                continue;
            }

            String trimmed = move.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] tokens = trimmed.split("\\s+");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    flat.add(token);
                }
            }
        }

        return flat;
    }
}