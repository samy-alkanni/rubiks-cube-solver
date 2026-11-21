import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Processes a static image of a single Rubik's Cube face and converts it to
 * the internal 9-sticker color representation used by this project.
 *
 * Returned face format is reading order:
 * [0,1,2,
 *  3,4,5,
 *  6,7,8]
 */
public class CubeImageProcessor {

    static {
        // Loads OpenCV native binaries from the Maven dependency.
        nu.pattern.OpenCV.loadLocally();
    }

    private static final int EXPECTED_STICKERS = 9;

    /**
     * Main API: reads one face image and returns the detected 9 stickers.
     *
     * @param imagePath path to a static .jpg image of one cube face
     * @return 9-character array in top-to-bottom / left-to-right order
     */
    public char[] processFaceImage(String imagePath) {
        Mat bgr = Imgcodecs.imread(imagePath);
        if (bgr.empty()) {
            throw new IllegalArgumentException("Cannot load image: " + imagePath);
        }

        Mat threshold = preprocessForContours(bgr);
        List<StickerDetection> stickers = detectStickerQuadrilaterals(threshold);

        if (stickers.size() != EXPECTED_STICKERS) {
            stickers = detectStickerGridFallback(bgr);
        }

        if (stickers.size() != EXPECTED_STICKERS) {
            throw new IllegalStateException(
                    "Expected 9 sticker squares, but found " + stickers.size()
                            + ". Try adjusting lighting, camera framing, or image crop.");
        }

        List<StickerDetection> sorted = sortStickersTopToBottomLeftToRight(stickers);

        Mat hsv = new Mat();
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);

        char[] face = new char[EXPECTED_STICKERS];
        for (int i = 0; i < EXPECTED_STICKERS; i++) {
            face[i] = detectStickerColor(hsv, sorted.get(i).center);
        }
        return face;
    }

    /**
     * Detects and returns the center sticker color (index 4) of a cube face image.
     * Useful for determining which face an image represents without relying on filename.
     *
     * @param imagePath path to the face image
     * @return the character representing the center sticker color (W, R, O, Y, G, B)
     */
    public char detectCenterColor(String imagePath) {
        Mat bgr = Imgcodecs.imread(imagePath);
        if (bgr.empty()) {
            throw new IllegalArgumentException("Cannot load image: " + imagePath);
        }

        Mat threshold = preprocessForContours(bgr);
        List<StickerDetection> stickers = detectStickerQuadrilaterals(threshold);
        if (stickers.size() != EXPECTED_STICKERS) {
            stickers = detectStickerGridFallback(bgr);
        }
        List<StickerDetection> sorted = sortStickersTopToBottomLeftToRight(stickers);

        if (sorted.size() < 5) {
            throw new IllegalStateException("Could not detect center sticker in image: " + imagePath);
        }

        Mat hsv = new Mat();
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);

        // Index 4 is the center sticker
        return detectStickerColor(hsv, sorted.get(4).center);
    }

    /**
     * Runs color diagnostics on one face image and saves an annotated image.
     *
     * The report includes, for each sticker (index 0..8): sampled H,S,V and
     * final classified color. Use this to tune HSV ranges when colors are wrong.
     *
     * @param imagePath input face image
     * @param outputPath output debug image path
     * @return multi-line text report with HSV values and color decision
     */
    public String debugColorDecisions(String imagePath, String outputPath) {
        Mat bgr = Imgcodecs.imread(imagePath);
        if (bgr.empty()) {
            throw new IllegalArgumentException("Cannot load image: " + imagePath);
        }

        Mat threshold = preprocessForContours(bgr);
        List<StickerDetection> stickers = detectStickerQuadrilaterals(threshold);
        if (stickers.size() != EXPECTED_STICKERS) {
            stickers = detectStickerGridFallback(bgr);
        }
        List<StickerDetection> sorted = sortStickersTopToBottomLeftToRight(stickers);

        Mat hsv = new Mat();
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);

        Mat debug = bgr.clone();
        StringBuilder report = new StringBuilder();
        report.append("Color diagnostics for ").append(imagePath).append("\n");
        report.append("OpenCV HSV ranges: H in [0,179], S in [0,255], V in [0,255]\n");

        for (int i = 0; i < sorted.size(); i++) {
            StickerDetection sticker = sorted.get(i);
            HsvSample sample = sampleHsv(hsv, sticker.center, 5);
            char detected = classifyColor(sample.h, sample.s, sample.v);

            report.append("index ").append(i)
                    .append(" -> H=").append((int) Math.round(sample.h))
                    .append(" S=").append((int) Math.round(sample.s))
                    .append(" V=").append((int) Math.round(sample.v))
                    .append(" => ").append(detected)
                    .append("\n");

            Imgproc.polylines(debug, List.of(sticker.polygon), true, new Scalar(0, 255, 0), 2);
            Imgproc.circle(debug, sticker.center, 3, new Scalar(0, 0, 255), -1);
            Imgproc.putText(
                    debug,
                    i + ":" + detected,
                    new Point(sticker.center.x + 6, sticker.center.y - 8),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.5,
                    new Scalar(255, 255, 255),
                    2);
            Imgproc.putText(
                    debug,
                    "H" + (int) Math.round(sample.h) + " S" + (int) Math.round(sample.s) + " V"
                            + (int) Math.round(sample.v),
                    new Point(sticker.center.x + 6, sticker.center.y + 12),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.4,
                    new Scalar(255, 255, 0),
                    1);
        }

        // Validate and prepare output path
        outputPath = validateAndPrepareOutputPath(outputPath);

        boolean ok = Imgcodecs.imwrite(outputPath, debug);
        if (!ok) {
            throw new IllegalStateException("Could not write debug image to: " + outputPath + ". Verifiez extension (.jpg, .png, .bmp) et que le repertoire parent existe.");
        }

        return report.toString();
    }

    /**
     * Saves a debug image where detected quadrilaterals are drawn and indexed 0..8
     * after geometric sorting.
     *
     * @param imagePath input image
     * @param outputPath output file path (e.g., face_debug.jpg)
     */
    public void debugContours(String imagePath, String outputPath) {
        Mat bgr = Imgcodecs.imread(imagePath);
        if (bgr.empty()) {
            throw new IllegalArgumentException("Cannot load image: " + imagePath);
        }

        Mat threshold = preprocessForContours(bgr);
        List<StickerDetection> stickers = detectStickerQuadrilaterals(threshold);
        if (stickers.size() != EXPECTED_STICKERS) {
            stickers = detectStickerGridFallback(bgr);
        }
        List<StickerDetection> sorted = sortStickersTopToBottomLeftToRight(stickers);

        Mat debug = bgr.clone();
        Scalar green = new Scalar(0, 255, 0);
        Scalar red = new Scalar(0, 0, 255);

        for (int i = 0; i < sorted.size(); i++) {
            StickerDetection s = sorted.get(i);
            Imgproc.polylines(debug, List.of(s.polygon), true, green, 2);
            Imgproc.circle(debug, s.center, 3, red, -1);
            Imgproc.putText(
                    debug,
                    String.valueOf(i),
                    new Point(s.center.x + 6, s.center.y - 6),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.5,
                    red,
                    2);
        }

        // Validate and prepare output path
        outputPath = validateAndPrepareOutputPath(outputPath);

        boolean ok = Imgcodecs.imwrite(outputPath, debug);
        if (!ok) {
            throw new IllegalStateException("Could not write debug image to: " + outputPath + ". Verifiez extension (.jpg, .png, .bmp) et que le repertoire parent existe.");
        }
    }

    /**
     * Validates output path and ensures parent directory exists.
     * Ensures the path has a valid image extension.
     * 
     * @param path the output file path
     * @return the validated and corrected path
     */
    private static String validateAndPrepareOutputPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be empty");
        }

        String normalizedPath = path.replace("\\", "/");
        String lowerPath = normalizedPath.toLowerCase();

        // Check for valid extension
        if (!lowerPath.endsWith(".jpg") && !lowerPath.endsWith(".jpeg") && 
            !lowerPath.endsWith(".png") && !lowerPath.endsWith(".bmp") &&
            !lowerPath.endsWith(".tiff")) {
            // If no valid extension, append .jpg
            normalizedPath = normalizedPath + ".jpg";
        }

        // Create parent directory if it doesn't exist
        File file = new File(normalizedPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            try {
                Files.createDirectories(Paths.get(parentDir.getAbsolutePath()));
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create output directory: " + parentDir.getAbsolutePath(), e);
            }
        }

        return normalizedPath;
    }

    private Mat preprocessForContours(Mat bgr) {
        Mat gray = new Mat();
        Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY);

        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        Mat threshold = new Mat();
        Imgproc.adaptiveThreshold(
                blurred,
                threshold,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                31,
                5);

        return threshold;
    }

    private List<StickerDetection> detectStickerQuadrilaterals(Mat threshold) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<StickerDetection> candidates = new ArrayList<>();

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area < 300) {
                continue;
            }

            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double perimeter = Imgproc.arcLength(contour2f, true);

            MatOfPoint2f approx2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approx2f, 0.03 * perimeter, true);

            if (approx2f.rows() != 4) {
                continue;
            }

            MatOfPoint approx = new MatOfPoint(approx2f.toArray());
            if (!Imgproc.isContourConvex(approx)) {
                continue;
            }

            Rect r = Imgproc.boundingRect(approx);
            double ratio = (double) r.width / Math.max(1.0, r.height);
            if (ratio < 0.65 || ratio > 1.35) {
                continue;
            }

            Point center = new Point(r.x + r.width / 2.0, r.y + r.height / 2.0);
            candidates.add(new StickerDetection(approx, center, area));
        }

        // Keep the 9 strongest square-like detections if extra contours are present.
        candidates.sort(Comparator.comparingDouble((StickerDetection s) -> s.area).reversed());
        if (candidates.size() > EXPECTED_STICKERS) {
            candidates = new ArrayList<>(candidates.subList(0, EXPECTED_STICKERS));
        }

        return candidates;
    }

    private List<StickerDetection> detectStickerGridFallback(Mat bgr) {
        List<StickerDetection> fallback = new ArrayList<>(EXPECTED_STICKERS);

        int width = bgr.cols();
        int height = bgr.rows();

        if (width <= 0 || height <= 0) {
            return fallback;
        }

        // Use a centered square region that is smaller than the full image.
        // The fallback is only an approximation when contour detection fails, so
        // it should stay conservative rather than covering the whole frame.
        int gridSize = Math.max(3, (int) Math.round(Math.min(width, height) * 0.90));
        int centerX = width / 2;
        int centerY = height / 2 + (int) Math.round(height * 0.04);

        int left = clamp(centerX - gridSize / 2, 0, Math.max(0, width - gridSize));
        int top = clamp(centerY - gridSize / 2, 0, Math.max(0, height - gridSize));

        int usableWidth = Math.max(3, Math.min(gridSize, width - left));
        int usableHeight = Math.max(3, Math.min(gridSize, height - top));

        double cellWidth = usableWidth / 3.0;
        double cellHeight = usableHeight / 3.0;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cellLeft = (int) Math.round(left + col * cellWidth);
                int cellTop = (int) Math.round(top + row * cellHeight);
                int cellRight = (int) Math.round(left + (col + 1) * cellWidth);
                int cellBottom = (int) Math.round(top + (row + 1) * cellHeight);

                int cellWidthInt = Math.max(1, cellRight - cellLeft);
                int cellHeightInt = Math.max(1, cellBottom - cellTop);
                Rect rect = new Rect(cellLeft, cellTop, cellWidthInt, cellHeightInt);
                Point center = new Point(cellLeft + cellWidthInt / 2.0, cellTop + cellHeightInt / 2.0);

                MatOfPoint polygon = new MatOfPoint(
                        new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Point(rect.x, rect.y + rect.height));

                fallback.add(new StickerDetection(polygon, center, rect.area()));
            }
        }

        return fallback;
    }

    private List<StickerDetection> sortStickersTopToBottomLeftToRight(List<StickerDetection> stickers) {
        List<StickerDetection> sorted = new ArrayList<>(stickers);

        // Global Y sort first.
        sorted.sort(Comparator.comparingDouble(s -> s.center.y));

        // Then split into three rows of three, and sort each row by X.
        int rowSize = 3;
        for (int rowStart = 0; rowStart + rowSize <= sorted.size(); rowStart += rowSize) {
            sorted.subList(rowStart, rowStart + rowSize).sort(Comparator.comparingDouble(s -> s.center.x));
        }

        return sorted;
    }

    private char detectStickerColor(Mat hsv, Point center) {
        HsvSample sample = sampleHsv(hsv, center, 5);
        return classifyColor(sample.h, sample.s, sample.v);
    }

    private HsvSample sampleHsv(Mat hsv, Point center, int radius) {
        int x = clamp((int) Math.round(center.x), 0, hsv.cols() - 1);
        int y = clamp((int) Math.round(center.y), 0, hsv.rows() - 1);

        // Sample a neighborhood around the center to reduce single-pixel noise.
        // Median is more robust than mean when a few border pixels are off-color.
        List<Double> hueValues = new ArrayList<>();
        List<Double> saturationValues = new ArrayList<>();
        List<Double> valueValues = new ArrayList<>();

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int sx = clamp(x + dx, 0, hsv.cols() - 1);
                int sy = clamp(y + dy, 0, hsv.rows() - 1);
                double[] pixel = hsv.get(sy, sx);
                hueValues.add(pixel[0]); // Hue in OpenCV: 0..179
                saturationValues.add(pixel[1]); // Saturation: 0..255
                valueValues.add(pixel[2]); // Value/Brightness: 0..255
            }
        }

        return new HsvSample(
                median(hueValues),
                median(saturationValues),
                median(valueValues));
    }

    private char classifyColor(double h, double s, double v) {
        /*
         * Adjusted thresholds based on user diagnostics:
         * - Narrower red hue to avoid capturing orange at the boundary (H around 6).
         * - Added a high-value, moderate-saturation white rule so bright but slightly
         *   desaturated stickers are recognized as white (reduces dark-spot misclass).
         */

        // Strong white detection: low saturation and high brightness
        if (s < 50 && v > 140) {
            return 'W';
        }

        // Additional white for very bright but slightly desaturated stickers
        if (v > 175 && s < 100) {
            return 'W';
        }

        // STRICT RED: keep red narrow so H≈6 is not misread as red
        if ((h >= 0 && h <= 4) || (h >= 176 && h <= 179)) {
            if (s > 85 && v > 35) {
                return 'R';
            }
        }

        // ORANGE: begin immediately after the red band
        if (h > 4 && h <= 24 && s > 60 && v > 50) {
            return 'O';
        }

        // YELLOW
        if (h > 24 && h <= 36 && s > 70 && v > 80) {
            return 'Y';
        }

        // GREEN
        if (h > 36 && h <= 90 && s > 60 && v > 50) {
            return 'G';
        }

        // BLUE
        if (h > 90 && h <= 140 && s > 60 && v > 50) {
            return 'B';
        }

        // Fallbacks: conservative defaults
        if (v > 175 && s < 100) {
            return 'W';
        }
        if (h <= 4 || h >= 176) {
            return 'R';
        }
        if (h <= 24) {
            return 'O';
        }
        if (h <= 40) {
            return 'Y';
        }
        if (h <= 95) {
            return 'G';
        }
        return 'B';
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        values.sort(Double::compareTo);
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle);
        }

        return (values.get(middle - 1) + values.get(middle)) / 2.0;
    }

    private static final class StickerDetection {
        private final MatOfPoint polygon;
        private final Point center;
        private final double area;

        private StickerDetection(MatOfPoint polygon, Point center, double area) {
            this.polygon = polygon;
            this.center = center;
            this.area = area;
        }
    }

    private static final class HsvSample {
        private final double h;
        private final double s;
        private final double v;

        private HsvSample(double h, double s, double v) {
            this.h = h;
            this.s = s;
            this.v = v;
        }
    }
}
