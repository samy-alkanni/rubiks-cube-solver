import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TestsUnitairesCube {
    private static class TestNomme {
        final String name;
        final Runnable test;

        TestNomme(String name, Runnable test) {
            this.name = name;
            this.test = test;
        }
    }

    public static void main(String[] args) {
        List<TestNomme> tests = Arrays.asList(
                new TestNomme("Centres initiaux et état résolu", TestsUnitairesCube::testerCentresInitiaux),
                new TestNomme("Mouvement + inverse restaure le cube", TestsUnitairesCube::testerMouvementEtInverse),
                new TestNomme("Quatre quarts de tour restaurent la face", TestsUnitairesCube::testerQuatreQuartsDeTour),
                new TestNomme("Un double mouvement égale deux quarts de tour", TestsUnitairesCube::testerDoubleMouvement),
            new TestNomme("resolutionAuto inverse bien le mélange enregistré", TestsUnitairesCube::testerResolutionAuto),
            new TestNomme("Compatibilité mouvements vs RubiksCube", TestsUnitairesCube::testerCompatibiliteMouvementsRubiksCube),
            new TestNomme("CFOP résout un mélange", TestsUnitairesCube::testerCFOPResoutMelange)
        );

        int passed = 0;
        for (TestNomme t : tests) {
            try {
                t.test.run();
                passed++;
                System.out.println("[PASS] " + t.name);
            } catch (AssertionError ex) {
                System.out.println("[FAIL] " + t.name + " -> " + ex.getMessage());
                throw ex;
            }
        }

        System.out.println("\nTous les tests sont passés: " + passed + "/" + tests.size());
    }

    private static void testerCentresInitiaux() {
        Cube cube = new Cube();
        char[][][] faces = cube.getFaces();

        assertEquals('G', faces[0][1][1], "Le centre de F doit être vert");
        assertEquals('R', faces[1][1][1], "Le centre de R doit être rouge");
        assertEquals('B', faces[2][1][1], "Le centre de B doit être bleu");
        assertEquals('O', faces[3][1][1], "Le centre de L doit être orange");
        assertEquals('W', faces[4][1][1], "Le centre de U doit être blanc");
        assertEquals('Y', faces[5][1][1], "Le centre de D doit être jaune");

        assertTrue(estResolu(cube), "Un nouveau cube doit être résolu");
    }

    private static void testerMouvementEtInverse() {
        String[] mouvementsDeBase = {"F", "R", "B", "L", "U", "D"};

        for (String m : mouvementsDeBase) {
            Cube cube = new Cube();
            cube.applyMove(m);
            cube.applyMove(m + "'");
            assertTrue(estResolu(cube), "Mouvement + inverse doit restaurer l'état pour " + m);
        }
    }

    private static void testerQuatreQuartsDeTour() {
        String[] mouvementsDeBase = {"F", "R", "B", "L", "U", "D"};

        for (String m : mouvementsDeBase) {
            Cube cube = new Cube();
            for (int i = 0; i < 4; i++) {
                cube.applyMove(m);
            }
            assertTrue(estResolu(cube), "Quatre quarts de tour doivent restaurer l'état pour " + m);
        }
    }

    private static void testerDoubleMouvement() {
        String[] mouvementsDeBase = {"F", "R", "B", "L", "U", "D"};

        for (String m : mouvementsDeBase) {
            Cube cubeDouble = new Cube();
            cubeDouble.applyMove(m + "2");

            Cube cubeDeuxSimples = new Cube();
            cubeDeuxSimples.applyMove(m);
            cubeDeuxSimples.applyMove(m);

            assertFacesEqual(cubeDouble.getFaces(), cubeDeuxSimples.getFaces(),
                    "Le double mouvement doit correspondre à deux quarts de tour pour " + m);
        }
    }

    private static void testerResolutionAuto() {
        Cube cube = new Cube();
        List<String> melange = new ArrayList<>(Arrays.asList("F", "R", "U'", "L2", "D", "B2", "R'"));

        for (String m : melange) {
            cube.applyMove(m);
        }

        cube.definirDernierMelange(melange);
        List<String> solution = cube.resolutionAuto();

        for (String m : solution) {
            cube.applyMove(m);
        }

        assertTrue(estResolu(cube), "resolutionAuto doit inverser la séquence de mélange enregistrée");
    }

    private static void testerCompatibiliteMouvementsRubiksCube() {
        char[][][] etat = creerEtatUnique();

        String[] moves = {
                "F", "F'", "F2",
                "R", "R'", "R2",
                "B", "B'", "B2",
                "L", "L'", "L2",
                "U", "U'", "U2",
                "D", "D'", "D2"
        };

        for (String move : moves) {
            Cube cube = new Cube();
            cube.chargerDepuisFaces(copieFaces(etat));
            cube.applyMove(move);

            RubiksCube rc = creerRubiksDepuisEtatProjet(etat);
            rc.move(move);
            char[][][] attendu = convertirRubiksVersEtatProjet(rc);

            assertFacesEqual(attendu, cube.getFaces(), "Mismatch move='" + move + "' entre Cube et RubiksCube");
        }
    }

    private static void testerCFOPResoutMelange() {
        Cube cube = new Cube();

        // Mélange déterministe (doit être résoluble via CFOP)
        List<String> melange = Arrays.asList(
                "R", "U", "R'", "U'",
                "F", "U", "F'",
                "L2", "D", "B2",
                "R", "U2", "R'",
                "D'", "F2"
        );

        for (String m : melange) {
            cube.applyMove(m);
        }

        List<String> rawSolution = CFOPIntegration.genererSolutionCFOP(cube);
        List<String> solution = aplatirCoups(rawSolution);
        assertTrue(solution != null && !solution.isEmpty(), "CFOP doit produire une solution non vide sur un cube mélangé");

        for (String m : solution) {
            cube.applyMove(m);
        }

        assertTrue(estResolu(cube), "CFOP doit résoudre le cube après le mélange");
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

    private static char[][][] creerEtatUnique() {
        char[][][] faces = new char[6][3][3];
        int idx = 0;
        for (int f = 0; f < 6; f++) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    faces[f][i][j] = (char) (0x0100 + idx++);
                }
            }
        }
        return faces;
    }

    private static char[][][] copieFaces(char[][][] src) {
        char[][][] out = new char[6][3][3];
        for (int f = 0; f < 6; f++) {
            for (int i = 0; i < 3; i++) {
                System.arraycopy(src[f][i], 0, out[f][i], 0, 3);
            }
        }
        return out;
    }

    private static RubiksCube creerRubiksDepuisEtatProjet(char[][][] etatProjet) {
        RubiksCube rc = new RubiksCube();
        rc.setFace(RubiksCube.U, face3Vers9(etatProjet[4]));
        rc.setFace(RubiksCube.R, face3Vers9(etatProjet[1]));
        rc.setFace(RubiksCube.F, face3Vers9(etatProjet[0]));
        rc.setFace(RubiksCube.D, face3Vers9(etatProjet[5]));
        rc.setFace(RubiksCube.L, face3Vers9(etatProjet[3]));
        rc.setFace(RubiksCube.B, face3Vers9(etatProjet[2]));
        rc.clearHistory();
        return rc;
    }

    private static char[][][] convertirRubiksVersEtatProjet(RubiksCube rc) {
        char[][] matrix = rc.getMatrix();

        char[][][] etatProjet = new char[6][3][3];
        remplirFace3Depuis9(etatProjet[0], matrix[RubiksCube.F]);
        remplirFace3Depuis9(etatProjet[1], matrix[RubiksCube.R]);
        remplirFace3Depuis9(etatProjet[2], matrix[RubiksCube.B]);
        remplirFace3Depuis9(etatProjet[3], matrix[RubiksCube.L]);
        remplirFace3Depuis9(etatProjet[4], matrix[RubiksCube.U]);
        remplirFace3Depuis9(etatProjet[5], matrix[RubiksCube.D]);
        return etatProjet;
    }

    private static char[] face3Vers9(char[][] face3) {
        char[] out = new char[9];
        int idx = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                out[idx++] = face3[i][j];
            }
        }
        return out;
    }

    private static void remplirFace3Depuis9(char[][] face3, char[] face9) {
        int idx = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                face3[i][j] = face9[idx++];
            }
        }
    }

    private static boolean estResolu(Cube cube) {
        char[][][] faces = cube.getFaces();
        for (int f = 0; f < 6; f++) {
            char center = faces[f][1][1];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (faces[f][i][j] != center) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void assertFacesEqual(char[][][] expected, char[][][] actual, String message) {
        for (int f = 0; f < 6; f++) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (expected[f][i][j] != actual[f][i][j]) {
                        throw new AssertionError(message + " à face=" + f + " ligne=" + i + " colonne=" + j);
                    }
                }
            }
        }
    }

    private static void assertEquals(char expected, char actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
