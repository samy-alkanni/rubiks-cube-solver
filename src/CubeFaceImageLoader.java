

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads all 6 faces of a Rubik's Cube from image files in a folder.
 * 
 * Expected folder structure:
 * <pre>
 * cube_images/
 *   ├── White.jpg     (or U.jpg)        - face U (top/up)
 *   ├── Red.jpg       (or R.jpg)        - face R (right)
 *   ├── Green.jpg     (or F.jpg)        - face F (front)
 *   ├── Yellow.jpg    (or D.jpg)        - face D (bottom/down)
 *   ├── Orange.jpg    (or L.jpg)        - face L (left)
 *   └── Blue.jpg      (or B.jpg)        - face B (back)
 * </pre>
 * 
 * This loader supports both naming conventions:
 * - Color names: White, Red, Green, Yellow, Orange, Blue
 * - Face letters: U, R, F, D, L, B
 */
public class CubeFaceImageLoader {
    private final CubeImageProcessor processor;

    // Canonical face order in this project.
    private static final String[] CANONICAL_FACE_NAMES = {"U", "R", "F", "D", "L", "B"};
    private static final String[] CANONICAL_FACE_COLORS = {"white", "red", "green", "yellow", "orange", "blue"};

        // Keywords accepted in filenames for each face. The first token match wins.
    private static final String[][] FACE_KEYWORDS = {
            {"u", "up", "white", "blanc"},
            {"r", "right", "red", "rouge"},
            {"f", "front", "green", "vert"},
            {"d", "down", "yellow", "jaune"},
            {"l", "left", "orange", "orange"},
            {"b", "back", "blue", "bleu"}
    };
    
    public CubeFaceImageLoader() {
        this.processor = new CubeImageProcessor();
    }
    
    /**
     * Loads all 6 face images from a folder and populates the given cube.
     * 
     * This version automatically detects which face each image represents by reading
     * the center sticker color. No filename conventions needed!
     * 
     * @param cube the RubiksCube instance to populate
     * @param folderPath path to the folder containing the face images
     * @return a summary string describing what was loaded
     * @throws IllegalArgumentException if required images are missing or cannot be read
     */
    public String loadAllFacesFromFolder(RubiksCube cube, String folderPath) {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + folderPath);
        }
        
        File[] images = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg") ||
                name.toLowerCase().endsWith(".png"));
        
        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("No image files found in: " + folderPath);
        }
        
        Map<Character, Integer> colorToFaceIndex = new HashMap<>();
        colorToFaceIndex.put('W', 0); // White -> U (up)
        colorToFaceIndex.put('R', 1); // Red -> R (right)
        colorToFaceIndex.put('G', 2); // Green -> F (front)
        colorToFaceIndex.put('Y', 3); // Yellow -> D (down)
        colorToFaceIndex.put('O', 4); // Orange -> L (left)
        colorToFaceIndex.put('B', 5); // Blue -> B (back)
        
        boolean[] facesLoaded = new boolean[6];
        Map<Integer, String> loadedFaces = new LinkedHashMap<>();
        StringBuilder summary = new StringBuilder();
        summary.append("Loading cube faces from: ").append(folderPath).append("\n");
        summary.append("(Auto-detecting faces by center sticker color)\n\n");

        for (File imageFile : images) {
            try {
                // Detect the center sticker color
                char centerColor = processor.detectCenterColor(imageFile.getAbsolutePath());
                
                // Map color to face index
                Integer faceIndex = colorToFaceIndex.get(centerColor);
                if (faceIndex == null) {
                    summary.append("⚠ WARNING: Could not map color '").append(centerColor)
                            .append("' from ").append(imageFile.getName()).append(" to a cube face\n");
                    continue;
                }
                
                if (facesLoaded[faceIndex]) {
                    summary.append("⚠ WARNING: Face ").append(CANONICAL_FACE_NAMES[faceIndex])
                            .append(" already loaded. Skipping duplicate: ")
                            .append(imageFile.getName()).append("\n");
                    continue;
                }
                
                // Process and load the face
                char[] faceData = processor.processFaceImage(imageFile.getAbsolutePath());
                cube.setFace(faceIndex, faceData);
                facesLoaded[faceIndex] = true;
                
                summary.append("✓ Loaded face ").append(CANONICAL_FACE_NAMES[faceIndex]).append(" (")
                        .append(CANONICAL_FACE_COLORS[faceIndex]).append(", center color: ")
                        .append(centerColor).append(") from ")
                        .append(imageFile.getName()).append("\n");
                
                loadedFaces.put(faceIndex, CANONICAL_FACE_NAMES[faceIndex]);
            } catch (Exception e) {
                summary.append("✗ ERROR processing ").append(imageFile.getName()).append(": ")
                        .append(e.getMessage()).append("\n");
            }
        }
        
        // Check which faces are missing
        int loadedCount = loadedFaces.size();
        if (loadedCount < 6) {
            summary.append("\n⚠ Only ").append(loadedCount)
                    .append("/6 faces loaded. Missing faces:");
            for (int i = 0; i < 6; i++) {
                if (!facesLoaded[i]) {
                    summary.append(" ").append(CANONICAL_FACE_NAMES[i]);
                }
            }
        } else {
            summary.append("\n✓ All 6 faces loaded successfully!");
        }
        
        return summary.toString();
    }
    
    /**
     * Processes a single face image and returns the char array.
     * Useful for testing or manual processing.
     * 
     * @param imagePath absolute or relative path to the image
     * @return 9-char array representing the face
     */
    public char[] processSingleFace(String imagePath) {
        return processor.processFaceImage(imagePath);
    }
    
    /**
     * Generates debug images for all face files in a folder.
     * 
     * @param folderPath input folder path
     * @param outputFolderPath output folder where debug images will be saved
     * @return a summary string describing debug image generation
     */
    public String generateDebugImagesForFolder(String folderPath, String outputFolderPath) {
        File inputFolder = new File(folderPath);
        File outputFolder = new File(outputFolderPath);
        
        if (!inputFolder.isDirectory()) {
            throw new IllegalArgumentException("Input is not a directory: " + folderPath);
        }
        
        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            throw new IllegalArgumentException("Cannot create output folder: " + outputFolderPath);
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Generating debug images in: ").append(outputFolderPath).append("\n");
        
        for (File imageFile : inputFolder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".jpg") || 
                name.toLowerCase().endsWith(".png"))) {
            
            String baseName = imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.'));
            String outputPath = new File(outputFolder, baseName + "_debug.jpg").getAbsolutePath();
            
            try {
                processor.debugContours(imageFile.getAbsolutePath(), outputPath);
                summary.append("✓ Debug image saved: ").append(baseName).append("_debug.jpg\n");
            } catch (Exception e) {
                summary.append("✗ ERROR generating debug for ").append(baseName)
                        .append(": ").append(e.getMessage()).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Lists all image files found in the given folder and their recognized face names.
     * 
     * @param folderPath path to search
     * @return a formatted string listing recognized images
     */
    public String listFacesInFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            return "Not a directory: " + folderPath;
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Images found in ").append(folderPath).append(":\n");
        
        File[] images = folder.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".jpg") ||
            name.toLowerCase().endsWith(".jpeg") ||
            name.toLowerCase().endsWith(".png"));
        
        if (images == null || images.length == 0) {
            result.append("  (no images found)");
        } else {
            for (File img : images) {
                String nameLower = img.getName().toLowerCase();
                String faceName = extractFaceName(nameLower);
                if (faceName != null) {
                    result.append("  ").append(img.getName()).append(" -> face ").append(faceName).append("\n");
                } else {
                    result.append("  ").append(img.getName()).append(" (unrecognized)\n");
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Finds an image file in the folder by face name (case-insensitive).
     * Supports both color names (white, red, green, etc.) and face letters (u, r, f, etc.).
     */
    private File findImageFile(File folder, int faceIndex) {
        File[] images = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jpg") ||
                name.toLowerCase().endsWith(".jpeg") ||
                name.toLowerCase().endsWith(".png"));

        if (images == null) {
            return null;
        }

        // Prefer an exact match first, then fall back to keyword matching.
        String canonicalName = CANONICAL_FACE_NAMES[faceIndex].toLowerCase();
        String canonicalColor = CANONICAL_FACE_COLORS[faceIndex];

        for (File image : images) {
            String normalized = stripExtension(image.getName()).toLowerCase();
            if (matchesToken(normalized, canonicalName) || matchesToken(normalized, canonicalColor)) {
                return image;
            }
        }

        for (File image : images) {
            String normalized = stripExtension(image.getName()).toLowerCase();
            for (String keyword : FACE_KEYWORDS[faceIndex]) {
                if (matchesToken(normalized, keyword)) {
                    return image;
                }
            }
        }

        return null;
    }
    
    /**
     * Extracts the face name from a filename by checking known patterns.
     */
    private String extractFaceName(String filenameLower) {
        String name = stripExtension(filenameLower);

        for (int faceIndex = 0; faceIndex < FACE_KEYWORDS.length; faceIndex++) {
            if (matchesAnyToken(name, CANONICAL_FACE_NAMES[faceIndex].toLowerCase())
                    || matchesAnyToken(name, CANONICAL_FACE_COLORS[faceIndex])) {
                return CANONICAL_FACE_NAMES[faceIndex];
            }
            for (String keyword : FACE_KEYWORDS[faceIndex]) {
                if (matchesToken(name, keyword)) {
                    return CANONICAL_FACE_NAMES[faceIndex];
                }
            }
        }
        return null;
    }

    private boolean matchesAnyToken(String normalizedName, String keyword) {
        return matchesToken(normalizedName, keyword);
    }

    private boolean matchesToken(String normalizedName, String keyword) {
        String[] tokens = normalizedName.split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String stripExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) {
            return filename;
        }
        return filename.substring(0, lastDot);
    }
}
