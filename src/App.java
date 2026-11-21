import javafx.application.Application;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Rotate;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// --- CERVEAU DU CODE --- 
// stocke l'état du cube, gère les mouvements, le mélange et la résolution
class Cube {
    private char[][][] faces; // 6 faces, chacune avec une matrice 3x3 de couleurs
    private List<String> dernierMelange = new ArrayList<>();

    public Cube() {
        reset();
    }

    public void reset() { // Initialise le cube résolu dans sa position originale:
        // 0 = vert (avant)
        // 1 = rouge (droite)
        // 2 = bleu (arrière)
        // 3 = orange (gauche)
        // 4 = blanc (haut)
        // 5 = jaune (bas)
        faces = new char[6][3][3];
        char[] colors = {'G', 'R', 'B', 'O', 'W', 'Y'};
        for (int f = 0; f < 6; f++) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    faces[f][i][j] = colors[f];
                }
            }
        }
    }

    public List<String> melangeAuto() {
        List<String> moves = new ArrayList<>();
        // Génère une séquence de 20 mouvements aléatoires
        String[] possibleMoves = {"F", "F'", "F2", "B", "B'", "B2", "L", "L'", "L2", "R", "R'", "R2", "U", "U'", "U2", "D", "D'", "D2"};
        for (int i = 0; i < 20; i++) {
            // Math.random() → donne un nombre entre 0 et 1
            // ex: 0.23, 0.89, 0.01

            // * possibleMoves.length
            // possibleMoves.length = 18 → nombre entre 0 et 17.999 lorsque multiplié par Math.random()

            // (int)
            // enlève la partie décimale → index entier
            String move = possibleMoves[(int)(Math.random() * possibleMoves.length)];
            moves.add(move);
            applyMove(move); // Applique les mouvements au cube
        }
        dernierMelange = new ArrayList<>(moves);
        return moves;
    }

    public List<String> resolutionAuto() { // "Solveur" très basique qui inverse simplement les mouvements du mélange
        List<String> solution = new ArrayList<>();
        for (int i = dernierMelange.size() - 1; i >= 0; i--) {
            String move = dernierMelange.get(i);
            if (move.endsWith("'")) { // Si F' => F
                solution.add(move.substring(0, move.length() - 1));
            } else if (move.endsWith("2")) { // Si F2 => F2
                solution.add(move);
            } else { // Si F => F'
                solution.add(move + "'");
            }
        }
        return solution;
    }

    public void applyMove(String move) {
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

        if (normalized.isEmpty()) {
            return;
        }

        char faceLetter = normalized.charAt(0);
        boolean clockwise = !prime;
        int turns = doubleTurn ? 2 : 1;

        for (int i = 0; i < turns; i++) {
            applyFaceMove(faceLetter, clockwise);
        }
    }

    private void rotateFace(int face) {
        char[][] temp = new char[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                temp[j][2 - i] = faces[face][i][j]; // rotation 90 degrés
                // MATRICES AVANT ROTATION: a b c
                //                          d e f
                //                          g h i
                // MATRICES APRES ROTATION: g d a
                //                          h e b
                //                          i f c

            }
        }
        faces[face] = temp;
    }

    private void applyFaceMove(char faceLetter, boolean clockwise) {
        int faceIndex = faceIndexFromLetter(faceLetter);
        if (faceIndex < 0) {
            return;
        }

        if (clockwise) {
            rotateFace(faceIndex);
        } else {
            // Anti-horaire = 3 rotations horaires
            for (int i = 0; i < 3; i++) {
                rotateFace(faceIndex);
            }
        }

        int[][] positions = ringPositionsForFace(faceLetter);
        if (positions != null) {
            cyclePositions(positions, clockwise);
        }
    }

    private int faceIndexFromLetter(char face) {
        switch (face) {
            case 'F':
                return 0;
            case 'R':
                return 1;
            case 'B':
                return 2;
            case 'L':
                return 3;
            case 'U':
                return 4;
            case 'D':
                return 5;
            default:
                return -1;
        }
    }

    private int[][] ringPositionsForFace(char faceLetter) {
        // Identique à RubiksCube (6x9) mais remappé sur l'ordre de faces du projet:
        // projet: F=0, R=1, B=2, L=3, U=4, D=5
        switch (faceLetter) {
            case 'U':
                return new int[][]{
                        {0, 0}, {0, 1}, {0, 2},
                        {1, 0}, {1, 1}, {1, 2},
                        {2, 0}, {2, 1}, {2, 2},
                        {3, 0}, {3, 1}, {3, 2}
                };
            case 'D':
                return new int[][]{
                        {0, 6}, {0, 7}, {0, 8},
                        {3, 6}, {3, 7}, {3, 8},
                        {2, 6}, {2, 7}, {2, 8},
                        {1, 6}, {1, 7}, {1, 8}
                };
            case 'L':
                return new int[][]{
                        {4, 0}, {4, 3}, {4, 6},
                        {2, 8}, {2, 5}, {2, 2},
                        {5, 0}, {5, 3}, {5, 6},
                        {0, 0}, {0, 3}, {0, 6}
                };
            case 'R':
                return new int[][]{
                        {4, 2}, {4, 5}, {4, 8},
                        {0, 2}, {0, 5}, {0, 8},
                        {5, 2}, {5, 5}, {5, 8},
                        {2, 6}, {2, 3}, {2, 0}
                };
            case 'F':
                return new int[][]{
                        {4, 6}, {4, 7}, {4, 8},
                        {3, 8}, {3, 5}, {3, 2},
                        {5, 2}, {5, 1}, {5, 0},
                        {1, 0}, {1, 3}, {1, 6}
                };
            case 'B':
                return new int[][]{
                        {4, 2}, {4, 1}, {4, 0},
                        {1, 8}, {1, 5}, {1, 2},
                        {5, 6}, {5, 7}, {5, 8},
                        {3, 0}, {3, 3}, {3, 6}
                };
            default:
                return null;
        }
    }

    private void cyclePositions(int[][] positions, boolean clockwise) {
        char[] buffer = new char[positions.length];
        for (int i = 0; i < positions.length; i++) {
            buffer[i] = getSticker(positions[i][0], positions[i][1]);
        }

        int shift = clockwise ? positions.length - 3 : 3;

        for (int i = 0; i < positions.length; i++) {
            int target = (i + shift) % positions.length;
            int face = positions[target][0];
            int index = positions[target][1];
            setSticker(face, index, buffer[i]);
        }
    }

    private char getSticker(int faceIndex, int index) {
        return faces[faceIndex][index / 3][index % 3];
    }

    private void setSticker(int faceIndex, int index, char value) {
        faces[faceIndex][index / 3][index % 3] = value;
    }

    public char[][][] getFaces() {
        return faces;
    }

    public void chargerDepuisFaces(char[][][] nouvellesFaces) {
        if (nouvellesFaces == null || nouvellesFaces.length != 6) {
            throw new IllegalArgumentException("État invalide: 6 faces requises");
        }

        char[][][] copie = new char[6][3][3];
        for (int f = 0; f < 6; f++) {
            if (nouvellesFaces[f] == null || nouvellesFaces[f].length != 3) {
                throw new IllegalArgumentException("Face invalide: index=" + f);
            }
            for (int i = 0; i < 3; i++) {
                if (nouvellesFaces[f][i] == null || nouvellesFaces[f][i].length != 3) {
                    throw new IllegalArgumentException("Face invalide: index=" + f + ", ligne=" + i);
                }
                System.arraycopy(nouvellesFaces[f][i], 0, copie[f][i], 0, 3);
            }
        }

        faces = copie;
        dernierMelange = new ArrayList<>();
    }

    public boolean estResolu() {
        char[] colors = {'G', 'R', 'B', 'O', 'W', 'Y'};
        for (int f = 0; f < 6; f++) {
            char expected = colors[f];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (faces[f][i][j] != expected) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void definirDernierMelange(List<String> coups) { // Permet de stocker le dernier mélange pour pouvoir le résoudre ensuite
        dernierMelange = new ArrayList<>(coups);
    }
}

// --- CORPS (INTERFACE GRAPHIQUE) ---
// affiche le cube en 3D, gère boutons + clavier, gère interactions utilisateur
public class App extends Application {
    private Cube cube;
    private Group cubeGroup;
    private List<Group> petitsCubes;
    private List<String> coupsMelange = new ArrayList<>();
    private int nombreCoupsMelange = 0;
    private List<String> coupsResolution;
    private int indexResolution = 0;
    private double mousePosX, mousePosY;
    private Rotate rotateX = new Rotate(0, 250, 250, 0, Rotate.X_AXIS);
    private Rotate rotateY = new Rotate(0, 250, 250, 0, Rotate.Y_AXIS);

    private static final double PAS_PETIT_CUBE = 70;
    private static final Duration DUREE_ANIMATION_ROTATION = Duration.millis(250);

    private boolean animationRotationEnCours = false;
    private boolean operationAutomatiqueEnCours = false;
    private Stage primaryStageRef;
    private enum Etat { // 3 STADES (neutre, mélange, résolution)
        // Empêche conflits d'action (ex: pas de mouvements de mélange pendant la résolution, etc.)
        NEUTRE,
        MELANGE,
        RESOLUTION
    }
    private Etat etatActuel = Etat.NEUTRE;
    private enum OperationAttendue {
        MELANGE,
        RESOLUTION
    }
    private OperationAttendue operationAttendue = OperationAttendue.MELANGE;
    private static final String STYLE_BASE_INDICATEUR_MODE =
            "-fx-font-family: 'Candara', 'Segoe UI'; -fx-font-size: 15px; -fx-font-weight: bold; " +
            "-fx-padding: 10 12 10 12; -fx-background-radius: 10; -fx-border-radius: 10;";
    private Label indicateurMode = new Label();
    private Label titreStatsSession = new Label("Stats session:");
    private Label indicateurStats = new Label();
    private TextFlow fluxSolution = new TextFlow();
    private TextFlow fluxMelange = new TextFlow();
    private Label indicateurCFOP = new Label();
    private boolean cfopInteractifEnCours = false;
    private List<CFOPIntegration.EtapeCFOP> etapesCFOPActuelles;
    private long debutSessionMillis = System.currentTimeMillis();
    private long debutMelangeInteractifMillis = -1;
    private long debutResolutionInteractiveMillis = -1;
    private long derniereDureeMelangeMillis = 0;
    private long derniereDureeResolutionMillis = 0;
    private int totalMelanges = 0;
    private int totalResolutions = 0;
    private int totalCoupsMelange = 0;
    private int totalCoupsResolution = 0;

    private void setTexteCFOP(String text) {
        if (text == null || text.isBlank()) {
            indicateurCFOP.setText("");
            indicateurCFOP.setVisible(false);
            indicateurCFOP.setManaged(false);
            return;
        }
        indicateurCFOP.setText(text);
        indicateurCFOP.setVisible(true);
        indicateurCFOP.setManaged(true);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStageRef = primaryStage;
        cube = new Cube();
        petitsCubes = new ArrayList<>();
        cubeGroup = creerCube3D();

        // Title
        Label title = new Label("Solveur interactif d'un cube Rubik de Samy Al-Kanni et Victor Noel");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-font-family: 'Cambria'; -fx-text-fill: #1f2937;");

        // Buttons
        Button melangeAutoButton = new Button("Mélange automatique");
        melangeAutoButton.setStyle("-fx-font-family: 'Bahnschrift', 'Segoe UI'; -fx-font-size: 16px;");
        Button melangeInteractifButton = new Button("Mélange interactif");
        melangeInteractifButton.setStyle("-fx-font-family: 'Bahnschrift', 'Segoe UI'; -fx-font-size: 16px;");
        Button scanButton = new Button("Scanner le cube");
        scanButton.setStyle("-fx-font-family: 'Bahnschrift', 'Segoe UI'; -fx-font-size: 16px;");
        Button resolutionAutoButton = new Button("Résolution automatique");
        resolutionAutoButton.setStyle("-fx-font-family: 'Bahnschrift', 'Segoe UI'; -fx-font-size: 16px;");
        Button resolutionInteractiveButton = new Button("Résolution interactive");
        resolutionInteractiveButton.setStyle("-fx-font-family: 'Bahnschrift', 'Segoe UI'; -fx-font-size: 16px;");
        Button resolutionHumaineCFOPButton = new Button("Résolution humaine (CFOP)");
        resolutionHumaineCFOPButton.setStyle("-fx-font-family: 'Bahnschrift', 'Segoe UI'; -fx-font-size: 16px;");

        // VBox boutons
        VBox buttonBox = new VBox(10, melangeAutoButton, melangeInteractifButton, scanButton, resolutionAutoButton, resolutionInteractiveButton, resolutionHumaineCFOPButton);
        buttonBox.setPadding(new javafx.geometry.Insets(10));
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        indicateurMode.setWrapText(true);
        indicateurMode.setMaxWidth(250);
        indicateurMode.setStyle(STYLE_BASE_INDICATEUR_MODE);
        mettreAJourIndicateurMode();

        indicateurStats.setWrapText(true);
        indicateurStats.setMaxWidth(250);
        titreStatsSession.setStyle("-fx-font-family: 'Candara', 'Segoe UI'; -fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        indicateurStats.setStyle("-fx-font-family: 'Candara', 'Segoe UI'; -fx-font-size: 14px; " +
            "-fx-padding: 10 12 10 12; -fx-background-radius: 10; -fx-border-radius: 10; " +
            "-fx-text-fill: #0f172a; -fx-background-color: #f8fafc; -fx-border-color: #cbd5e1;");
        mettreAJourIndicateurStats();

        VBox blocStats = new VBox(5, titreStatsSession, indicateurStats);
        blocStats.setAlignment(Pos.CENTER_LEFT);
        blocStats.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; " +
            "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10;");

        VBox leftBox = new VBox(12, indicateurMode, blocStats, buttonBox);
        leftBox.setPadding(new javafx.geometry.Insets(10));
        leftBox.setAlignment(Pos.TOP_LEFT);

        // 3D SubScene
        SubScene subScene = new SubScene(cubeGroup, 500, 500, true, null);
        PerspectiveCamera camera = new PerspectiveCamera();
        camera.setTranslateX(0);
        camera.setTranslateY(0);
        camera.setTranslateZ(0);
        camera.getTransforms().addAll(rotateX, rotateY);
        subScene.setCamera(camera);

        // Mouse controls
        subScene.setOnMousePressed(event -> {
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
        });
        // Vitesse caméra
        subScene.setOnMouseDragged(event -> {
            double dx = event.getSceneX() - mousePosX;
            double dy = event.getSceneY() - mousePosY;
            rotateY.setAngle(rotateY.getAngle() + dx * 0.5);
            rotateX.setAngle(rotateX.getAngle() - dy * 0.5);
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
        });

        // Labels
        fluxSolution.setStyle("-fx-font-size: 20px; -fx-padding: 10px; -fx-font-family: 'Palatino Linotype', 'Segoe UI';");
        fluxMelange.setStyle("-fx-font-size: 20px; -fx-padding: 10px; -fx-font-family: 'Palatino Linotype', 'Segoe UI';");

        setTexteCFOP("");
        indicateurCFOP.setWrapText(true);
        indicateurCFOP.setTextAlignment(TextAlignment.CENTER);
        indicateurCFOP.setAlignment(Pos.CENTER);
        indicateurCFOP.setPrefWidth(500);
        indicateurCFOP.setMaxWidth(500);
        indicateurCFOP.setStyle("-fx-font-family: 'Candara', 'Segoe UI'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 6 10 6 10;");

        fluxSolution.getChildren().add(new Text("Solution: "));
        fluxMelange.getChildren().add(new Text("Mélange: "));

        // Layout
        BorderPane root = new BorderPane();
        root.setTop(title);
        BorderPane.setAlignment(title, javafx.geometry.Pos.CENTER);
        root.setLeft(leftBox);
        VBox centerBox = new VBox(8, subScene, indicateurCFOP);
        centerBox.setAlignment(Pos.CENTER);
        root.setCenter(centerBox);
        BorderPane.setAlignment(centerBox, javafx.geometry.Pos.CENTER);

        // VBox pour les solutions et mélanges
        VBox bottomBox = new VBox(5, fluxMelange, fluxSolution);
        bottomBox.setPadding(new javafx.geometry.Insets(10));
        root.setBottom(bottomBox);

        Label guideTitle = new Label("Guide de notation officiel:");
        guideTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Candara', 'Segoe UI';");
        Label guideContenu = new Label("F: Face avant\n" +
            "B: Face arrière\n" +
            "L: Face gauche\n" +
            "R: Face droite\n" +
            "U: Face supérieure\n" +
            "D: Face inférieure\n" +
                "' : Mouvement inverse (prime)\n" +
            "2: Double mouvement");
        guideContenu.setWrapText(true);
        guideContenu.setPrefWidth(250);
        guideContenu.setMaxWidth(250);
        guideContenu.setStyle("-fx-font-family: 'Candara', 'Segoe UI'; -fx-font-size: 14px; " +
            "-fx-padding: 10 12 10 12; -fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-background-color: #ffffff; -fx-border-color: #cbd5e1;");
        // VBox pour guide de notation
        VBox guideBox = new VBox(5, guideTitle, guideContenu);
        guideBox.setAlignment(Pos.CENTER);
        guideBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; " +
            "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10;");

        // Shortcuts
        Label shortcutsTitle = new Label("Raccourcis clavier:");
        shortcutsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Candara', 'Segoe UI';");
        Label raccourcisContenu = new Label("Mélange interactif:\n" +
                "F: F    G: F'    S: F2\n" +
                "B: B    V: B'    C: B2\n" +
                "L: L    K: L'    J: L2\n" +
                "R: R    E: R'    W: R2\n" +
                "U: U    Q: U'    A: U2\n" +
                "D: D    O: D'    I: D2\n" +
                "(Limite 20 coups)\n\n" +
                "Résolution interactive:\n" +
                "N : coup suivant\n" +
                "B : coup précédent\n" +
                "(à la fin: N pour terminer)");
            raccourcisContenu.setWrapText(true);
            raccourcisContenu.setPrefWidth(250);
            raccourcisContenu.setMaxWidth(250);
            raccourcisContenu.setStyle("-fx-font-family: 'Candara', 'Segoe UI'; -fx-font-size: 14px; " +
                "-fx-padding: 10 12 10 12; -fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-background-color: #ffffff; -fx-border-color: #cbd5e1;");
        // VBox pour accourcis
            VBox shortcutsBox = new VBox(5, shortcutsTitle, raccourcisContenu);
        shortcutsBox.setAlignment(Pos.CENTER);
        shortcutsBox.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; " +
            "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10;");

        // VBox de 2 VBox (guide + raccourcis)
        VBox rightVBox = new VBox(10, guideBox, shortcutsBox);
        rightVBox.setPadding(new javafx.geometry.Insets(10));
        root.setRight(rightVBox);

        // Event handlers
        melangeAutoButton.setOnAction(e -> melangeAuto());
        melangeInteractifButton.setOnAction(e -> melangeInteractif());
        scanButton.setOnAction(e -> scanCube());
        resolutionAutoButton.setOnAction(e -> resolutionAuto());
        resolutionInteractiveButton.setOnAction(e -> resolutionInteractive());
        resolutionHumaineCFOPButton.setOnAction(e -> resolutionHumaineCFOP());

        Scene scene = new Scene(root,1200,700);
        primaryStage.setTitle("Solveur Rubik's Cube");
        primaryStage.setScene(scene);

        // Keyboard event handler for interactive modes
        scene.setOnKeyPressed(event -> {
            if (operationAutomatiqueEnCours || animationRotationEnCours) {
                return;
            }
            if (etatActuel == Etat.MELANGE) {
                gererToucheMelange(event.getCode());
            } else if (etatActuel == Etat.RESOLUTION) {
                gererToucheResolution(event.getCode());
            }
        });

        primaryStage.show();
    }

    private Group creerCube3D() { // Création du cube 3D avec des Box pour chaque face et des bordures noires pour les démarquer
        Group group = new Group();
        double size = 70;
        double spacing = 1;
        double step = size * spacing;
        double faceOffset = (size * spacing) / 2;
        char[][][] faces = cube.getFaces();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Group smallGroup = new Group();

                    // +X face (droite)
                    if (x == 1) {
                        Box faceX = new Box(size, size, 0.1);
                        faceX.setTranslateX(faceOffset);
                        char colorX = faces[1][y + 1][z + 1]; // +1 car convertion monde 3D à tableau
                        faceX.setMaterial(new PhongMaterial(obtenirCouleur(colorX)));
                        // faces[1] = 'R'
                        // → 'R' = rouge
                        // → affiché en rouge POUR TOUT faces[1]
                        faceX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(faceX);
                        // Borders
                        Box topX = new Box(size, 1, 0.11);
                        topX.setTranslateX(faceOffset);
                        topX.setTranslateY(size / 2 - 0.5);
                        topX.setMaterial(new PhongMaterial(Color.BLACK));
                        topX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(topX);
                        Box bottomX = new Box(size, 1, 0.11);
                        bottomX.setTranslateX(faceOffset);
                        bottomX.setTranslateY(-size / 2 + 0.5);
                        bottomX.setMaterial(new PhongMaterial(Color.BLACK));
                        bottomX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(bottomX);
                        Box leftX = new Box(1, size, 0.11);
                        leftX.setTranslateX(faceOffset);
                        leftX.setTranslateZ(size / 2 - 0.5);
                        leftX.setMaterial(new PhongMaterial(Color.BLACK));
                        leftX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(leftX);
                        Box rightX = new Box(1, size, 0.11);
                        rightX.setTranslateX(faceOffset);
                        rightX.setTranslateZ(-size / 2 + 0.5);
                        rightX.setMaterial(new PhongMaterial(Color.BLACK));
                        rightX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(rightX);
                    }

                    // -X face (gauche)
                    if (x == -1) {
                        Box faceMX = new Box(size, size, 0.1);
                        faceMX.setTranslateX(-faceOffset);
                        char colorMX = faces[3][y + 1][2 - (z + 1)];
                        faceMX.setMaterial(new PhongMaterial(obtenirCouleur(colorMX)));
                        faceMX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(faceMX);
                        // Borders
                        Box topMX = new Box(size, 1, 0.11);
                        topMX.setTranslateX(-faceOffset);
                        topMX.setTranslateY(size / 2 - 0.5);
                        topMX.setMaterial(new PhongMaterial(Color.BLACK));
                        topMX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(topMX);
                        Box bottomMX = new Box(size, 1, 0.11);
                        bottomMX.setTranslateX(-faceOffset);
                        bottomMX.setTranslateY(-size / 2 + 0.5);
                        bottomMX.setMaterial(new PhongMaterial(Color.BLACK));
                        bottomMX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(bottomMX);
                        Box leftMX = new Box(1, size, 0.11);
                        leftMX.setTranslateX(-faceOffset);
                        leftMX.setTranslateZ(size / 2 - 0.5);
                        leftMX.setMaterial(new PhongMaterial(Color.BLACK));
                        leftMX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(leftMX);
                        Box rightMX = new Box(1, size, 0.11);
                        rightMX.setTranslateX(-faceOffset);
                        rightMX.setTranslateZ(-size / 2 + 0.5);
                        rightMX.setMaterial(new PhongMaterial(Color.BLACK));
                        rightMX.getTransforms().add(new Rotate(90, Rotate.Y_AXIS));
                        smallGroup.getChildren().add(rightMX);
                    }

                    // +Y face (BAS car JavaFX a l'axe Y inversé)
                    if (y == 1) {
                        Box faceY = new Box(size, 0.1, size);
                        faceY.setTranslateY(faceOffset);
                        char colorY = faces[5][z + 1][x + 1];
                        faceY.setMaterial(new PhongMaterial(obtenirCouleur(colorY)));
                        smallGroup.getChildren().add(faceY);
                        // Borders
                        Box topY = new Box(size, 0.11, 1);
                        topY.setTranslateY(faceOffset);
                        topY.setTranslateZ(size / 2 - 0.5);
                        topY.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(topY);
                        Box bottomY = new Box(size, 0.11, 1);
                        bottomY.setTranslateY(faceOffset);
                        bottomY.setTranslateZ(-size / 2 + 0.5);
                        bottomY.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(bottomY);
                        Box leftY = new Box(1, 0.11, size);
                        leftY.setTranslateY(faceOffset);
                        leftY.setTranslateX(-size / 2 + 0.5);
                        leftY.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(leftY);
                        Box rightY = new Box(1, 0.11, size);
                        rightY.setTranslateY(faceOffset);
                        rightY.setTranslateX(size / 2 - 0.5);
                        rightY.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(rightY);
                    }

                    // -Y face (HAUT car JavaFX a l'axe Y inversé)
                    if (y == -1) {
                        Box faceMY = new Box(size, 0.1, size);
                        faceMY.setTranslateY(-faceOffset);
                        char colorMY = faces[4][1 - z][x + 1];
                        faceMY.setMaterial(new PhongMaterial(obtenirCouleur(colorMY)));
                        smallGroup.getChildren().add(faceMY);
                        // Borders
                        Box topMY = new Box(size, 0.11, 1);
                        topMY.setTranslateY(-faceOffset);
                        topMY.setTranslateZ(size / 2 - 0.5);
                        topMY.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(topMY);
                        Box bottomMY = new Box(size, 0.11, 1);
                        bottomMY.setTranslateY(-faceOffset);
                        bottomMY.setTranslateZ(-size / 2 + 0.5);
                        bottomMY.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(bottomMY);
                        Box leftMY = new Box(1, 0.11, size);
                        leftMY.setTranslateY(-faceOffset);
                        leftMY.setTranslateX(-size / 2 + 0.5);
                        leftMY.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(leftMY);
                        Box rightMY = new Box(1, 0.11, size);
                        rightMY.setTranslateY(-faceOffset);
                        rightMY.setTranslateX(size / 2 - 0.5);
                        rightMY.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(rightMY);
                    }

                    // +Z face (arrière dans JavaFX, on affiche la face 'B' ici)
                    if (z == 1) {
                        Box faceZ = new Box(size, size, 0.1);
                        faceZ.setTranslateZ(faceOffset);
                        // Afficher la face arrière (projet index 2)
                        char colorZ = faces[2][y + 1][1 - x];
                        faceZ.setMaterial(new PhongMaterial(obtenirCouleur(colorZ)));
                        smallGroup.getChildren().add(faceZ);
                        // Borders
                        Box topZ = new Box(size, 1, 0.11);
                        topZ.setTranslateZ(faceOffset);
                        topZ.setTranslateY(size / 2 - 0.5);
                        topZ.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(topZ);
                        Box bottomZ = new Box(size, 1, 0.11);
                        bottomZ.setTranslateZ(faceOffset);
                        bottomZ.setTranslateY(-size / 2 + 0.5);
                        bottomZ.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(bottomZ);
                        Box leftZ = new Box(1, size, 0.11);
                        leftZ.setTranslateZ(faceOffset);
                        leftZ.setTranslateX(-size / 2 + 0.5);
                        leftZ.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(leftZ);
                        Box rightZ = new Box(1, size, 0.11);
                        rightZ.setTranslateZ(faceOffset);
                        rightZ.setTranslateX(size / 2 - 0.5);
                        rightZ.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(rightZ);
                    }

                    // -Z face (avant dans JavaFX, on affiche la face 'F' ici)
                    if (z == -1) {
                        Box faceMZ = new Box(size, size, 0.1);
                        faceMZ.setTranslateZ(-faceOffset);
                        // Afficher la face avant (projet index 0)
                        char colorMZ = faces[0][y + 1][x + 1];
                        faceMZ.setMaterial(new PhongMaterial(obtenirCouleur(colorMZ)));
                        smallGroup.getChildren().add(faceMZ);
                        // Borders
                        Box topMZ = new Box(size, 1, 0.11);
                        topMZ.setTranslateZ(-faceOffset);
                        topMZ.setTranslateY(size / 2 - 0.5);
                        topMZ.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(topMZ);
                        Box bottomMZ = new Box(size, 1, 0.11);
                        bottomMZ.setTranslateZ(-faceOffset);
                        bottomMZ.setTranslateY(-size / 2 + 0.5);
                        bottomMZ.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(bottomMZ);
                        Box leftMZ = new Box(1, size, 0.11);
                        leftMZ.setTranslateZ(-faceOffset);
                        leftMZ.setTranslateX(-size / 2 + 0.5);
                        leftMZ.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(leftMZ);
                        Box rightMZ = new Box(1, size, 0.11);
                        rightMZ.setTranslateZ(-faceOffset);
                        rightMZ.setTranslateX(size / 2 - 0.5);
                        rightMZ.setMaterial(new PhongMaterial(Color.BLACK));
                        smallGroup.getChildren().add(rightMZ);
                    }

                    smallGroup.setTranslateX(x * step);
                    smallGroup.setTranslateY(y * step);
                    smallGroup.setTranslateZ(z * step);
                    petitsCubes.add(smallGroup);
                    group.getChildren().add(smallGroup);
                }
            }
        }
        // Center the group
        group.setTranslateX(250);
        group.setTranslateY(250);
        group.setTranslateZ(0);
        return group;
    }

    private void mettreAJourCube3D() { // Met à jour les couleurs du cube 3D en fonction de l'état actuel du cube logique
        char[][][] faces = cube.getFaces();

        for (Group smallGroup : petitsCubes) {
            int x = (int) Math.round(smallGroup.getTranslateX() / 70); // On divise par 70 (taille du cube) pour obtenir les coordonnées logiques (-1, 0, 1) du petit cube
            int y = (int) Math.round(smallGroup.getTranslateY() / 70);
            int z = (int) Math.round(smallGroup.getTranslateZ() / 70);

            List<javafx.scene.Node> children = smallGroup.getChildren();
            int idx = 0;

            // +X (RIGHT)
            if (x == 1) {
                char c = faces[1][y + 1][z + 1];
                ((Box) children.get(idx)).setMaterial(new PhongMaterial(obtenirCouleur(c)));
                idx += 5; // On saute les 5 éléments suivants qui sont les bordures de cette face (1 face + 4 bordures = 5 éléments) pour passer à la face suivante
            }

            // -X (LEFT)
            if (x == -1) {
                char c = faces[3][y + 1][1 - z];
                ((Box) children.get(idx)).setMaterial(new PhongMaterial(obtenirCouleur(c)));
                idx += 5;
            }

            // +Y (DOWN)
            if (y == 1) {
                char c = faces[5][z + 1][x + 1];
                ((Box) children.get(idx)).setMaterial(new PhongMaterial(obtenirCouleur(c)));
                idx += 5;
            }

            // -Y (UP)
            if (y == -1) {
                char c = faces[4][1 - z][x + 1];
                ((Box) children.get(idx)).setMaterial(new PhongMaterial(obtenirCouleur(c)));
                idx += 5;
            }

            // +Z (FRONT)
            if (z == 1) {
                // JavaFX camera regarde vers -Z, donc +Z est l'arrière: afficher la face arrière (index 2)
                char c = faces[2][y + 1][1 - x];
                ((Box) children.get(idx)).setMaterial(new PhongMaterial(obtenirCouleur(c)));
                idx += 5;
            }

            // -Z (BACK)
            if (z == -1) {
                // -Z est l'avant visible: afficher la face avant (index 0)
                char c = faces[0][y + 1][x + 1];
                ((Box) children.get(idx)).setMaterial(new PhongMaterial(obtenirCouleur(c)));
                idx += 5;
            }
        }
    }

    private Color obtenirCouleur(char c) {
        switch (c) {
            case 'W': return Color.WHITE;
            case 'R': return Color.RED;
            case 'B': return Color.BLUE;
            case 'O': return Color.ORANGE;
            case 'G': return Color.GREEN;
            case 'Y': return Color.YELLOW;
            default: return Color.GRAY;
        }
    }

    private static final String[] COUPS_POSSIBLES = {
            "F", "F'", "F2",
            "B", "B'", "B2",
            "L", "L'", "L2",
            "R", "R'", "R2",
            "U", "U'", "U2",
            "D", "D'", "D2"
    };

    private List<String> genererMelangeAleatoire(int longueur) {
        List<String> moves = new ArrayList<>();
        for (int i = 0; i < longueur; i++) {
            String move = COUPS_POSSIBLES[(int) (Math.random() * COUPS_POSSIBLES.length)];
            moves.add(move);
        }
        return moves;
    }

    private char extraireFaceCoup(String coup) {
        if (coup == null) return 0;
        String base = coup.trim();
        if (base.endsWith("2")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("'")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) return 0;
        return base.charAt(0);
    }

    private javafx.geometry.Point3D axePourFace(char face) {
        switch (face) {
            case 'F':
            case 'B':
                return Rotate.Z_AXIS;
            case 'L':
            case 'R':
                return Rotate.X_AXIS;
            case 'U':
            case 'D':
                return Rotate.Y_AXIS;
            default:
                return null;
        }
    }

    private int sensHorairePourFace(char face) {
        // Sens (en degrés positifs) pour un coup "horaire" selon la notation officielle.
        // Table calibrée pour la scène actuelle:
        // - +Z = avant, -Z = arrière
        // - +X = droite, -X = gauche
        // - +Y = bas (JavaFX), -Y = haut
        switch (face) {
            case 'F': return 1;
            case 'B': return -1;
            case 'R': return -1;
            case 'L': return 1;
            case 'U': return 1;
            case 'D': return -1;
            default: return 1;
        }
    }

    private double anglePourCoup(String coup) {
        if (coup == null || coup.isBlank()) return 0;

        String base = coup.trim();
        boolean doubleCoup = base.endsWith("2");
        if (doubleCoup) {
            base = base.substring(0, base.length() - 1);
        }
        boolean prime = base.endsWith("'");
        if (prime) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) return 0;

        char face = base.charAt(0);
        double amplitude = doubleCoup ? 180.0 : 90.0;
        double angle = amplitude * sensHorairePourFace(face);
        if (prime) angle = -angle;
        return angle;
    }

    private List<Group> selectionnerCouche(char face) {
        List<Group> couche = new ArrayList<>();
        for (Group petitCube : petitsCubes) {
            int x = (int) Math.round(petitCube.getTranslateX() / PAS_PETIT_CUBE);
            int y = (int) Math.round(petitCube.getTranslateY() / PAS_PETIT_CUBE);
            int z = (int) Math.round(petitCube.getTranslateZ() / PAS_PETIT_CUBE);

            switch (face) {
                case 'F':
                    if (z == -1) couche.add(petitCube);
                    break;
                case 'B':
                    if (z == 1) couche.add(petitCube);
                    break;
                case 'R':
                    if (x == 1) couche.add(petitCube);
                    break;
                case 'L':
                    if (x == -1) couche.add(petitCube);
                    break;
                case 'U':
                    if (y == -1) couche.add(petitCube);
                    break;
                case 'D':
                    if (y == 1) couche.add(petitCube);
                    break;
            }
        }
        return couche;
    }

    private Group clonerPetitCube(Group original) {
        Group clone = new Group();
        clone.setTranslateX(original.getTranslateX());
        clone.setTranslateY(original.getTranslateY());
        clone.setTranslateZ(original.getTranslateZ());

        for (Node child : original.getChildren()) {
            if (!(child instanceof Box)) continue;
            Box boxOriginale = (Box) child;
            Box boxClone = new Box(boxOriginale.getWidth(), boxOriginale.getHeight(), boxOriginale.getDepth());
            boxClone.setTranslateX(boxOriginale.getTranslateX());
            boxClone.setTranslateY(boxOriginale.getTranslateY());
            boxClone.setTranslateZ(boxOriginale.getTranslateZ());
            boxClone.setMaterial(boxOriginale.getMaterial());
            boxClone.getTransforms().addAll(boxOriginale.getTransforms());
            clone.getChildren().add(boxClone);
        }

        return clone;
    }

    private void jouerCoupAvecAnimation(String coup, Runnable apresCoup) {
        if (coup == null || coup.isBlank()) {
            if (apresCoup != null) apresCoup.run();
            return;
        }
        if (animationRotationEnCours) {
            return;
        }

        char face = extraireFaceCoup(coup);
        javafx.geometry.Point3D axe = axePourFace(face);
        double angle = anglePourCoup(coup);
        if (axe == null || angle == 0) {
            cube.applyMove(coup);
            mettreAJourCube3D();
            if (apresCoup != null) apresCoup.run();
            return;
        }

        List<Group> couche = selectionnerCouche(face);
        if (couche.isEmpty()) {
            cube.applyMove(coup);
            mettreAJourCube3D();
            if (apresCoup != null) apresCoup.run();
            return;
        }

        animationRotationEnCours = true;

        Group overlayCouche = new Group();
        for (Group petitCube : couche) {
            petitCube.setVisible(false);
            overlayCouche.getChildren().add(clonerPetitCube(petitCube));
        }

        Rotate rotation = new Rotate(0, 0, 0, 0, axe);
        overlayCouche.getTransforms().add(rotation);
        cubeGroup.getChildren().add(overlayCouche);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(rotation.angleProperty(), 0, Interpolator.LINEAR)),
                new KeyFrame(DUREE_ANIMATION_ROTATION, new KeyValue(rotation.angleProperty(), angle, Interpolator.LINEAR))
        );
        timeline.setOnFinished(evt -> {
            // On applique le coup à la fin : l'overlay montre la rotation, puis on met à jour les couleurs du cube "fixe".
            cube.applyMove(coup);
            mettreAJourCube3D();

            for (Group petitCube : couche) {
                petitCube.setVisible(true);
            }
            cubeGroup.getChildren().remove(overlayCouche);

            animationRotationEnCours = false;
            if (apresCoup != null) apresCoup.run();
        });
        timeline.play();
    }

    private void jouerSequenceCoupsAvecAnimation(List<String> coups, int index, Runnable aLaFin) {
        if (coups == null || index >= coups.size()) {
            if (aLaFin != null) aLaFin.run();
            return;
        }

        String coup = coups.get(index);
        jouerCoupAvecAnimation(coup, () -> jouerSequenceCoupsAvecAnimation(coups, index + 1, aLaFin));
    }

    private String etapeCFOPPourIndex(int moveIndex) {
        if (!cfopInteractifEnCours || etapesCFOPActuelles == null || moveIndex < 0) {
            return null;
        }

        int cursor = 0;
        for (CFOPIntegration.EtapeCFOP etape : etapesCFOPActuelles) {
            int size = etape == null || etape.coups == null ? 0 : etape.coups.size();
            if (moveIndex < cursor + size) {
                return etape.nom;
            }
            cursor += size;
        }
        return null;
    }

    private void afficherAlerte(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void mettreAJourIndicateurMode() {
        if (etatActuel == Etat.MELANGE) {
            if (operationAutomatiqueEnCours) {
                indicateurMode.setText("Mode actif: Mélange automatique\nAnimation en cours...");
            } else {
                indicateurMode.setText("Mode actif: Mélange interactif\nProchaine étape: Résolution");
            }
            indicateurMode.setStyle(STYLE_BASE_INDICATEUR_MODE + "-fx-text-fill: #7c2d12; -fx-background-color: #ffedd5; -fx-border-color: #fdba74;");
            return;
        }

        if (etatActuel == Etat.RESOLUTION) {
            if (operationAutomatiqueEnCours) {
                indicateurMode.setText("Mode actif: Résolution automatique\nAnimation en cours...");
            } else {
                indicateurMode.setText("Mode actif: Résolution interactive\nTouches: N pour avancer");
            }
            indicateurMode.setStyle(STYLE_BASE_INDICATEUR_MODE + "-fx-text-fill: #1d4ed8; -fx-background-color: #dbeafe; -fx-border-color: #93c5fd;");
            return;
        }

        if (operationAttendue == OperationAttendue.MELANGE) {
            indicateurMode.setText("Prêt\nProchaine étape: Mélange");
            indicateurMode.setStyle(STYLE_BASE_INDICATEUR_MODE + "-fx-text-fill: #166534; -fx-background-color: #dcfce7; -fx-border-color: #86efac;");
        } else {
            indicateurMode.setText("Prêt\nProchaine étape: Résolution");
            indicateurMode.setStyle(STYLE_BASE_INDICATEUR_MODE + "-fx-text-fill: #854d0e; -fx-background-color: #fef9c3; -fx-border-color: #fde047;");
        }
    }

    private String formaterDuree(long dureeMillis) {
        long secondes = Math.max(0, dureeMillis / 1000);
        long minutes = secondes / 60;
        long resteSecondes = secondes % 60;
        return String.format("%02d:%02d", minutes, resteSecondes);
    }

    private void mettreAJourIndicateurStats() {
        String texte = "Temps session: " + formaterDuree(System.currentTimeMillis() - debutSessionMillis) + "\n" +
                "Mélanges: " + totalMelanges + " (coups: " + totalCoupsMelange + ")\n" +
                "Résolutions: " + totalResolutions + " (coups: " + totalCoupsResolution + ")\n" +
                "Dernier mélange: " + formaterDuree(derniereDureeMelangeMillis) + "\n" +
                "Dernière résolution: " + formaterDuree(derniereDureeResolutionMillis);
        indicateurStats.setText(texte);
    }

    private void finaliserMelangeInteractifSiActif() {
        if (etatActuel == Etat.MELANGE && nombreCoupsMelange > 0) {
            totalMelanges++;
            totalCoupsMelange += nombreCoupsMelange;
            if (debutMelangeInteractifMillis > 0) {
                derniereDureeMelangeMillis = System.currentTimeMillis() - debutMelangeInteractifMillis;
            }
            mettreAJourIndicateurStats();
        }
        if (etatActuel == Etat.MELANGE) {
            debutMelangeInteractifMillis = -1;
        }
    }
    
    private void mettreAJourFluxMelange(String text, boolean highlight) { // Highlight le dernier coup du mélange en jaune et gras
        fluxMelange.getChildren().clear();

        if (text == null || text.isEmpty()) {
            fluxMelange.getChildren().add(new Text("Mélange: "));
            return;
        }

        String[] moves = text.trim().split(" ");

        for (int i = 0; i < moves.length; i++) {
            Text t = new Text(moves[i] + " ");

            if (highlight && i == moves.length - 1) {
                t.setFill(Color.YELLOW);
                t.setStyle("-fx-font-weight: bold;");
            }

            fluxMelange.getChildren().add(t);
        }

        fluxMelange.getChildren().add(0, new Text("Mélange: "));
    }
    
    private void mettreAJourFluxSolution(String text, boolean highlight) { // Highlight le dernier coup de la solution en jaune et gras
        fluxSolution.getChildren().clear();

        if (text == null || text.isEmpty()) {
            fluxSolution.getChildren().add(new Text("Solution: "));
            return;
        }

        String[] moves = text.trim().split(" ");

        for (int i = 0; i < moves.length; i++) {
            Text t = new Text(moves[i] + " ");

            if (highlight && i == moves.length - 1) {
                t.setFill(Color.YELLOW);
                t.setStyle("-fx-font-weight: bold;");
            }

            fluxSolution.getChildren().add(t);
        }

        fluxSolution.getChildren().add(0, new Text("Solution: "));
    }
    
    private StringBuilder progressionMelange = new StringBuilder();
    
    private void melangeAuto() {
        if (operationAutomatiqueEnCours || animationRotationEnCours) {
            afficherAlerte("Veuillez attendre la fin de l'animation en cours !");
            return;
        }
        if (etatActuel != Etat.NEUTRE) {
            afficherAlerte("Une opération est déjà en cours !");
            return;
        }
        if (operationAttendue != OperationAttendue.MELANGE) {
            afficherAlerte("Vous devez résoudre avant de remélanger !");
            return;
        }

        etatActuel = Etat.MELANGE;
        operationAutomatiqueEnCours = true;
        mettreAJourIndicateurMode();
        setTexteCFOP("");
        cfopInteractifEnCours = false;
        etapesCFOPActuelles = null;

        long debutMelangeAuto = System.currentTimeMillis();
        List<String> moves = genererMelangeAleatoire(20);
        
        coupsMelange = new ArrayList<>(moves);
        nombreCoupsMelange = moves.size();
        cube.definirDernierMelange(coupsMelange); // On sauvegarde les coups de mélange
        
        mettreAJourFluxMelange(String.join(" ", moves), false);
        
        progressionSolution.setLength(0);
        mettreAJourFluxSolution("", false);

        jouerSequenceCoupsAvecAnimation(moves, 0, () -> {
            totalMelanges++;
            totalCoupsMelange += moves.size();
            derniereDureeMelangeMillis = System.currentTimeMillis() - debutMelangeAuto;
            mettreAJourIndicateurStats();

            operationAttendue = OperationAttendue.RESOLUTION;
            etatActuel = Etat.NEUTRE;
            operationAutomatiqueEnCours = false;
            mettreAJourIndicateurMode();
        });
    }

    private void melangeInteractif() {
        if (operationAutomatiqueEnCours || animationRotationEnCours) {
            afficherAlerte("Veuillez attendre la fin de l'animation en cours !");
            return;
        }
        if (etatActuel != Etat.NEUTRE) {
            afficherAlerte("Une opération est déjà en cours !");
            return;
        }
        if (operationAttendue != OperationAttendue.MELANGE) {
            afficherAlerte("Vous devez résoudre avant de remélanger !");
            return;
        }

        etatActuel = Etat.MELANGE;
        setTexteCFOP("");
        cfopInteractifEnCours = false;
        etapesCFOPActuelles = null;
        // Dès l'entrée en mélange interactif, le prochain type autorisé devient la résolution.
        operationAttendue = OperationAttendue.RESOLUTION;
        mettreAJourIndicateurMode();
        debutMelangeInteractifMillis = System.currentTimeMillis();
        coupsMelange.clear();
        nombreCoupsMelange = 0;
        
        progressionMelange.setLength(0);
        coupsMelange.clear();
        
        cube.definirDernierMelange(new ArrayList<>());
        
        progressionSolution.setLength(0);
        mettreAJourFluxSolution("", false);
        
        fluxMelange.getChildren().clear();
        fluxMelange.getChildren().add(new Text("Mélange interactif: faites vos mouvements"));
    }

    private void scanCube() {
        if (operationAutomatiqueEnCours || animationRotationEnCours) {
            afficherAlerte("Veuillez attendre la fin de l'animation en cours !");
            return;
        }
        if (etatActuel != Etat.NEUTRE) {
            afficherAlerte("Une opération est déjà en cours !");
            return;
        }
        if (operationAttendue != OperationAttendue.MELANGE) {
            afficherAlerte("Vous devez résoudre avant de scanner un nouveau cube !");
            return;
        }

        setTexteCFOP("");
        cfopInteractifEnCours = false;
        etapesCFOPActuelles = null;

        ButtonType choisirDossier = new ButtonType("Choisir un dossier");
        ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Scanner le cube");
        alert.setHeaderText("Charger les 6 faces depuis des images");
        alert.setContentText(
                "Veuillez charger les 6 images (une par face) dans un même dossier.\n" +
                "Formats supportés: JPG / PNG.\n" +
                "Les faces sont détectées automatiquement par la couleur du centre.\n\n" +
            "Convention projet: face du haut = Blanc (W), face du bas = Jaune (Y).\n" +
            "Astuce: gardez le même sens sur chaque photo (le bord du haut de la face doit rester en haut de l'image)."
        );
        alert.getButtonTypes().setAll(choisirDossier, annuler);

        Optional<ButtonType> reponse = alert.showAndWait();
        if (reponse.isEmpty() || reponse.get() != choisirDossier) {
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Sélectionner le dossier contenant les 6 faces");
        File folder = chooser.showDialog(primaryStageRef);
        if (folder == null) {
            return;
        }

        try {
            CubeFaceImageLoader loader = new CubeFaceImageLoader();
            RubiksCube cubeImages = new RubiksCube();
            String rapport = loader.loadAllFacesFromFolder(cubeImages, folder.getAbsolutePath());

            if (!rapport.contains("All 6 faces loaded successfully!")) {
                afficherAlerte(rapport);
                return;
            }

            // Sanity check: each color should appear exactly 9 times on a valid scan.
            char[][] matrix = cubeImages.getMatrix();
            java.util.Map<Character, Integer> counts = new java.util.HashMap<>();
            for (int f = 0; f < 6; f++) {
                for (int i = 0; i < 9; i++) {
                    char c = matrix[f][i];
                    counts.put(c, counts.getOrDefault(c, 0) + 1);
                }
            }
            char[] expectedColors = {'W', 'R', 'G', 'Y', 'O', 'B'};
            StringBuilder issues = new StringBuilder();
            for (char ec : expectedColors) {
                int n = counts.getOrDefault(ec, 0);
                if (n != 9) {
                    issues.append(ec).append("=").append(n).append(" ");
                }
            }
            if (issues.length() > 0) {
                afficherAlerte(
                        "Scan invalide: chaque couleur doit apparaître 9 fois.\n" +
                                "Comptage: " + issues.toString().trim() + "\n\n" +
                                "Essayez de reprendre les photos (éclairage homogène, cube bien cadré)."
                );
                return;
            }

            char[][][] facesProjet = CFOPIntegration.convertirRubiksCubeVersFacesProjet(cubeImages);
            cube.chargerDepuisFaces(facesProjet);
            mettreAJourCube3D();

            coupsMelange.clear();
            nombreCoupsMelange = 0;
            progressionMelange.setLength(0);
            progressionSolution.setLength(0);
            cube.definirDernierMelange(new ArrayList<>());
            mettreAJourFluxMelange("Scan du cube", false);
            mettreAJourFluxSolution("", false);

            operationAttendue = OperationAttendue.RESOLUTION;
            etatActuel = Etat.NEUTRE;
            operationAutomatiqueEnCours = false;
            mettreAJourIndicateurMode();

            Alert ok = new Alert(Alert.AlertType.INFORMATION, rapport);
            ok.setTitle("Scanner le cube");
            ok.setHeaderText("Chargement terminé");
            ok.showAndWait();
        } catch (Exception e) {
            afficherAlerte("Erreur lors du scan: " + e.getMessage());
        }
    }

    private void resolutionHumaineCFOP() {
        if (operationAutomatiqueEnCours || animationRotationEnCours) {
            afficherAlerte("Veuillez attendre la fin de l'animation en cours !");
            return;
        }
        if (etatActuel == Etat.RESOLUTION) {
            afficherAlerte("Une résolution est déjà en cours !");
            return;
        }
        if (operationAttendue != OperationAttendue.RESOLUTION) {
            afficherAlerte("Vous devez faire un mélange ou scanner avant de résoudre !");
            return;
        }

        finaliserMelangeInteractifSiActif();

        // Mode CFOP interactif: l'utilisateur appuie sur N pour jouer chaque coup.
        etatActuel = Etat.RESOLUTION;
        operationAutomatiqueEnCours = false;
        mettreAJourIndicateurMode();
        debutResolutionInteractiveMillis = System.currentTimeMillis();

        List<CFOPIntegration.EtapeCFOP> etapes;
        List<String> solution;
        try {
            etapes = CFOPIntegration.genererEtapesCFOP(cube);
            solution = new ArrayList<>();
            for (CFOPIntegration.EtapeCFOP etape : etapes) {
                solution.addAll(etape.coups);
            }
        } catch (Exception e) {
            etatActuel = Etat.NEUTRE;
            mettreAJourIndicateurMode();
            setTexteCFOP("");
            afficherAlerte("Erreur CFOP: " + e.getMessage());
            return;
        }

        if (solution.isEmpty()) {
            operationAutomatiqueEnCours = false;
            etatActuel = Etat.NEUTRE;
            if (cube.estResolu()) {
                operationAttendue = OperationAttendue.MELANGE;
            }
            mettreAJourIndicateurMode();

            Alert info = new Alert(Alert.AlertType.INFORMATION, "Aucun mouvement nécessaire (cube déjà résolu, ou aucun coup trouvé).\n\nVous pouvez mélanger ou scanner un autre cube.");
            info.setTitle("Résolution Humaine (CFOP)");
            info.setHeaderText("CFOP: 0 mouvement");
            info.showAndWait();
            return;
        }

        // Prépare la résolution interactive (N)
        cfopInteractifEnCours = true;
        etapesCFOPActuelles = etapes;
        coupsResolution = solution;
        indexResolution = 0;
        progressionSolution.setLength(0);

        fluxSolution.getChildren().clear();
        fluxSolution.getChildren().add(new Text("Utilisez N pour jouer le coup suivant, B pour revenir en arrière (CFOP). À la fin: N pour terminer."));

        // Afficher l'étape initiale
        String etape0 = etapeCFOPPourIndex(0);
        if (etape0 != null && !etape0.isBlank()) {
            setTexteCFOP("CFOP: " + etape0);
        } else {
            setTexteCFOP("CFOP");
        }
    }

    private void resolutionAuto() {
        if (operationAutomatiqueEnCours || animationRotationEnCours) {
            afficherAlerte("Veuillez attendre la fin de l'animation en cours !");
            return;
        }
        if (etatActuel == Etat.RESOLUTION) {
            afficherAlerte("Une résolution est déjà en cours !");
            return;
        }
        if (operationAttendue != OperationAttendue.RESOLUTION) {
            afficherAlerte("Vous devez faire un mélange avant de résoudre !");
            return;
        }
        if (coupsMelange.isEmpty()) {
            afficherAlerte("Veuillez faire au moins 1 mouvement de mélange !");
            return;
        }

        finaliserMelangeInteractifSiActif();
        etatActuel = Etat.RESOLUTION;
        operationAutomatiqueEnCours = true;
        mettreAJourIndicateurMode();

        long debutResolutionAuto = System.currentTimeMillis();
        List<String> solution = cube.resolutionAuto();

        mettreAJourFluxSolution(String.join(" ", solution), false);

        jouerSequenceCoupsAvecAnimation(solution, 0, () -> {
            totalResolutions++;
            totalCoupsResolution += solution.size();
            derniereDureeResolutionMillis = System.currentTimeMillis() - debutResolutionAuto;
            mettreAJourIndicateurStats();

            operationAttendue = OperationAttendue.MELANGE;
            etatActuel = Etat.NEUTRE;
            operationAutomatiqueEnCours = false;
            mettreAJourIndicateurMode();
        });
    }

    private void resolutionInteractive() {
        if (operationAutomatiqueEnCours || animationRotationEnCours) {
            afficherAlerte("Veuillez attendre la fin de l'animation en cours !");
            return;
        }
        if (etatActuel == Etat.RESOLUTION) {
            afficherAlerte("Une résolution est déjà en cours !");
            return;
        }
        if (operationAttendue != OperationAttendue.RESOLUTION) {
            afficherAlerte("Vous devez faire un mélange avant de résoudre !");
            return;
        }
        if (coupsMelange.isEmpty()) {
            afficherAlerte("Veuillez faire au moins 1 mouvement !");
            return;
        }

        finaliserMelangeInteractifSiActif();
        etatActuel = Etat.RESOLUTION;
        mettreAJourIndicateurMode();
        debutResolutionInteractiveMillis = System.currentTimeMillis();

        progressionSolution.setLength(0);

        coupsResolution = new ArrayList<>(cube.resolutionAuto());
        indexResolution = 0;
        
        fluxSolution.getChildren().clear();
        fluxSolution.getChildren().add(new Text("Utilisez N pour jouer le coup suivant, B pour revenir en arrière. À la fin: N pour terminer."));
    }

    private void gererToucheMelange(KeyCode key) {
        if (etatActuel != Etat.MELANGE) return; // DOIT être en stade MELANGE pour pouvoir faire 
        // des mouvements de mélange interactif, sinon on ignore les touches
        if (operationAutomatiqueEnCours) return;

        String move = null;
        switch (key) {
            case F: move = "F"; break;
            case G: move = "F'"; break;
            case S: move = "F2"; break;
            case B: move = "B"; break;
            case V: move = "B'"; break;
            case C: move = "B2"; break;
            case L: move = "L"; break;
            case K: move = "L'"; break;
            case J: move = "L2"; break;
            case R: move = "R"; break;
            case E: move = "R'"; break;
            case W: move = "R2"; break;
            case U: move = "U"; break;
            case Q: move = "U'"; break;
            case A: move = "U2"; break;
            case D: move = "D"; break;
            case O: move = "D'"; break;
            case I: move = "D2"; break;
        }

        if (move != null) {
            if (animationRotationEnCours) return;
        	if (nombreCoupsMelange >= 20) {
        	    afficherAlerte("Limite de 20 coups atteinte !");
        	    return;
        	}
            final String coupFinal = move;
            jouerCoupAvecAnimation(coupFinal, () -> {
                coupsMelange.add(coupFinal);
                nombreCoupsMelange++;

                progressionMelange.append(coupFinal).append(" ");
                mettreAJourFluxMelange(progressionMelange.toString(), true);

                // IMPORTANT : mélange valide dès 1 coup
                cube.definirDernierMelange(coupsMelange);
            });
        }
    }

    private StringBuilder progressionSolution = new StringBuilder();

    private void gererToucheResolution(KeyCode key) {
        if (etatActuel != Etat.RESOLUTION) return; // DOIT être en stade SOLVING pour pouvoir 
        // faire des mouvements de résolution interactive, sinon on ignore les touches
        if (operationAutomatiqueEnCours) return;
        if (animationRotationEnCours) return;
        if (coupsResolution == null) return;

        boolean next = (key == KeyCode.N);
        boolean prev = (key == KeyCode.B);

        if (!next && !prev) {
            return;
        }

        // Si on est à la fin et que l'utilisateur appuie sur N, on termine et on sort du mode RESOLUTION.
        if (next && indexResolution >= coupsResolution.size()) {
            finaliserResolutionInteractive();
            return;
        }

        if (next && indexResolution < coupsResolution.size()) {
            if (cfopInteractifEnCours) {
                String etape = etapeCFOPPourIndex(indexResolution);
                if (etape != null && !etape.isBlank()) {
                    setTexteCFOP("CFOP: " + etape);
                } else {
                    setTexteCFOP("CFOP");
                }
            }

            String move = coupsResolution.get(indexResolution);
            jouerCoupAvecAnimation(move, () -> {
                indexResolution++;
                mettreAJourProgressionSolutionDepuisIndex();

                if (cfopInteractifEnCours) {
                    if (indexResolution >= coupsResolution.size()) {
                        setTexteCFOP("CFOP: Terminé");
                    } else {
                        String prochaineEtape = etapeCFOPPourIndex(indexResolution);
                        if (prochaineEtape != null && !prochaineEtape.isBlank()) {
                            setTexteCFOP("CFOP: " + prochaineEtape);
                        } else {
                            setTexteCFOP("CFOP");
                        }
                    }
                }
            });
            return;
        }

        if (prev && indexResolution > 0) {
            // Annule le dernier coup joué (inverse du coup à l'index - 1)
            String lastPlayed = coupsResolution.get(indexResolution - 1);
            String inverse = inverserCoup(lastPlayed);
            if (inverse == null) {
                return;
            }

            jouerCoupAvecAnimation(inverse, () -> {
                indexResolution--;
                mettreAJourProgressionSolutionDepuisIndex();

                if (cfopInteractifEnCours) {
                    String etape = etapeCFOPPourIndex(indexResolution);
                    if (etape != null && !etape.isBlank()) {
                        setTexteCFOP("CFOP: " + etape);
                    } else {
                        setTexteCFOP("CFOP");
                    }
                }
            });
        }
    }

    private void mettreAJourProgressionSolutionDepuisIndex() {
        progressionSolution.setLength(0);
        for (int i = 0; i < indexResolution && coupsResolution != null && i < coupsResolution.size(); i++) {
            String m = coupsResolution.get(i);
            if (m != null && !m.isBlank()) {
                progressionSolution.append(m).append(" ");
            }
        }
        mettreAJourFluxSolution(progressionSolution.toString(), true);
    }

    private void finaliserResolutionInteractive() {
        // Fin explicite (→ quand il n'y a plus de coups)
        totalResolutions++;
        if (coupsResolution != null) {
            totalCoupsResolution += coupsResolution.size();
        }
        if (debutResolutionInteractiveMillis > 0) {
            derniereDureeResolutionMillis = System.currentTimeMillis() - debutResolutionInteractiveMillis;
        }
        debutResolutionInteractiveMillis = -1;
        mettreAJourIndicateurStats();

        operationAttendue = OperationAttendue.MELANGE;
        etatActuel = Etat.NEUTRE;
        mettreAJourIndicateurMode();

        if (cfopInteractifEnCours) {
            cfopInteractifEnCours = false;
            etapesCFOPActuelles = null;
            setTexteCFOP("");

            if (!cube.estResolu()) {
                javafx.application.Platform.runLater(() ->
                        afficherAlerte("Attention : le cube n'est pas complètement résolu après la séquence CFOP.\nVérifiez l'orientation des photos (si scan) et l'éclairage.")
                );
            }
        }

        fluxSolution.getChildren().clear();
        fluxSolution.getChildren().add(new Text("Solution: " + progressionSolution.toString()));
    }

    private static String inverserCoup(String move) {
        if (move == null) {
            return null;
        }

        String m = move.trim();
        if (m.isEmpty()) {
            return null;
        }
        m = m.replace('\u2019', '\'');

        if (m.endsWith("2")) {
            return m;
        }
        if (m.endsWith("'")) {
            return m.substring(0, m.length() - 1);
        }
        return m + "'";
    }

    public static void main(String[] args) {
        launch(args);
    }
}

