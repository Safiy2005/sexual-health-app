package com.sddp.sexualhealthapp.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class SvgIcon {

    private static final Pattern TAG_PATH = Pattern.compile("<path\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_RECT = Pattern.compile("<rect\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR = Pattern.compile("\\b([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(['\"])(.*?)\\2");
    private static final Pattern TAG_CIRCLE = Pattern.compile("<circle\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private SvgIcon() {}

    public static Node load(String resourcePath, String styleClass) {
        return load(resourcePath, styleClass, 22);
    }

    public static Node load(String resourcePath, String styleClass, double targetPx) {
        String svg = readResource(resourcePath);

        Group g = new Group();

        // paths
        Matcher mPathTag = TAG_PATH.matcher(svg);
        while (mPathTag.find()) {
            String tag = mPathTag.group();
            String d = getAttr(tag, "d");
            if (d != null && !d.isBlank()) {
                g.getChildren().add(makePath(d, styleClass));
            }
        }

        // rects
        Matcher mRectTag = TAG_RECT.matcher(svg);
        while (mRectTag.find()) {
            String tag = mRectTag.group();

            double x = parseDoubleOr(getAttr(tag, "x"), 0);
            double y = parseDoubleOr(getAttr(tag, "y"), 0);
            double w = parseDoubleOr(getAttr(tag, "width"), 0);
            double h = parseDoubleOr(getAttr(tag, "height"), 0);

            if (w > 0 && h > 0) {
                
                String d = "M " + x + " " + y +
                        " H " + (x + w) +
                        " V " + (y + h) +
                        " H " + x +
                        " Z";
                g.getChildren().add(makePath(d, styleClass));
            }
        }

        Matcher mCircTag = TAG_CIRCLE.matcher(svg);
        while (mCircTag.find()){
            String tag = mCircTag.group();

            double cx = parseDoubleOr(getAttr(tag,"cx"),0);
            double cy = parseDoubleOr(getAttr(tag,"cy"),0);
            double r = parseDoubleOr(getAttr(tag,"r"),0);

            if (r >0 ){
                String d =  "M " + (cx + r) + " " + cy +
                            " A " + r + " " + r + " 0 1 0 " + (cx - r) + " " + cy +
                            " A " + r + " " + r + " 0 1 0 " + (cx + r) + " " + cy +
                            " Z";
                g.getChildren().add(makePath(d, styleClass));
            }
        }
        

        if (g.getChildren().isEmpty()) {
            throw new IllegalArgumentException("No supported shapes found in " + resourcePath);
        }

        // Normalize so it centers properly
        Bounds b = g.getBoundsInLocal();
        g.getTransforms().add(new Translate(-b.getMinX(), -b.getMinY()));

        // Scale to targetPx
        Bounds b2 = g.getBoundsInLocal();
        double w2 = b2.getWidth();
        double h2 = b2.getHeight();
        if (w2 > 0 && h2 > 0) {
            double scale = targetPx / Math.max(w2, h2);
            g.getTransforms().add(new Scale(scale, scale));
        }

        // Wrap so ToggleButton centers it nicely
        StackPane wrap = new StackPane(g);
        wrap.setMinSize(targetPx, targetPx);
        wrap.setPrefSize(targetPx, targetPx);
        wrap.setMaxSize(targetPx, targetPx);
        return wrap;
    }

    private static SVGPath makePath(String d, String styleClass) {
        SVGPath p = new SVGPath();
        p.setContent(d);

        
        p.setFill(javafx.scene.paint.Color.TRANSPARENT);

        if (styleClass != null && !styleClass.isBlank()) {
            p.getStyleClass().add(styleClass);
        }
        return p;
    }

    private static String getAttr(String tag, String name) {
        Matcher m = ATTR.matcher(tag);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(3);
            if (key.equalsIgnoreCase(name)) return val;
        }
        return null;
    }

    private static double parseDoubleOr(String s, double fallback) {
        if (s == null) return fallback;
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return fallback; }
    }

    private static String readResource(String resourcePath) {
        InputStream is = SvgIcon.class.getResourceAsStream(resourcePath);
        if (is == null) throw new IllegalArgumentException("SVG resource not found: " + resourcePath);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read SVG: " + resourcePath, e);
        }
    }
}
