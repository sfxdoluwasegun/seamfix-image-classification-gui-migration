import kotlin.collections.ArraysKt;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;

public class ImageClassifier {
    private final File[] inputFiles;
    private Session tfSession;
    private int currentFileIndex = 0;
    private File currentFile;

    private JPanel panel1;
    private JButton validButton;
    private JButton invalidButton;
    private JLabel imageLabel;
    private JLabel titleImage;
    private JLabel originalImage;
    private static String outputDir = "./files/output/";
    private static String validOutputDir = "valid/";
    private static String originalOutputDir = "original/";
    private static String invalidOutputDir = "invalid/";
    private static String inputDir = "./files/input/";
    private static String modelInputDir = "./files/model";

    private static int IMAGE_SCALE_SIZE = 224;
    private float[] predictionResults;
    private float improvementLevel = 0.5f;


    public static void main(String[] args) {

        JFrame frame = new JFrame("ImageClassifier");

        frame.setContentPane(new ImageClassifier().panel1);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

    public ImageClassifier() {

        try {
            byte[] model = Files.readAllBytes(Paths.get(modelInputDir, "model.pb"));

            Graph graph = new Graph();
            graph.importGraphDef(model);

            tfSession = new Session(graph);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        createOutputDirs();

        System.out.println(new File(inputDir).getAbsolutePath());

        this.inputFiles = new File(inputDir).listFiles();

        this.currentFile = inputFiles != null ? inputFiles[currentFileIndex] : null;

        if (currentFile == null) {

            System.out.println("Input file dir is empty.");

            return;

        }

        updateDisplayedImage();

        validButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {


                try {
                    //copy original
                    Files.copy(currentFile.toPath(), new File(outputDir + originalOutputDir + currentFile.getName()).toPath());

                    //Copy optimized
                    saveCurrentOptimizedImageToFile();
                    Files.move(currentFile.toPath(), new File(outputDir + validOutputDir + currentFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                if (currentFileIndex < inputFiles.length - 1) {

                    moveToNextImage();

                }
            }
        });


        invalidButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try {
                    saveCurrentOptimizedImageToFile();
                    Files.move(currentFile.toPath(), new File(outputDir + invalidOutputDir + currentFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                if (currentFileIndex < inputFiles.length - 1) {

                    moveToNextImage();

                }


            }
        });


        KeyAdapter shortcutKeyListeners = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_I) {

                    invalidButton.doClick();

                } else if (e.getKeyCode() == KeyEvent.VK_V) {

                    validButton.doClick();

                }

            }
        };

        validButton.addKeyListener(shortcutKeyListeners);
        invalidButton.addKeyListener(shortcutKeyListeners);
    }

    private void saveCurrentOptimizedImageToFile() {

        Image img = ((ImageIcon) imageLabel.getIcon()).getImage();

        BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g2 = bi.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();

        try {
            ImageIO.write(bi, "jpg", new File(currentFile.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createOutputDirs() {

        new File(outputDir).mkdirs();

        new File(outputDir + validOutputDir).mkdirs();
        new File(outputDir + invalidOutputDir).mkdirs();
        new File(outputDir + originalOutputDir).mkdirs();

    }

    private void moveToNextImage() {
        currentFileIndex++;

        ImageClassifier.this.currentFile = inputFiles[currentFileIndex];

        updateDisplayedImage();
    }

    private void updateDisplayedImage() {

        ImageIcon originalImage = new ImageIcon(currentFile.getAbsolutePath());

        ImageIcon resizedImage = scaleImage(originalImage, IMAGE_SCALE_SIZE, IMAGE_SCALE_SIZE);

        float[] resizedImageFloat = convertImageIconToFloatArray(resizedImage);

        normalizeFloatArray(resizedImageFloat);

        this.predictionResults = makePrediction(resizedImageFloat);

        BufferedImage removedBackground = removeBackground(resizedImage);

        resizedImage.setImage(removedBackground);

        resizedImage.setImage(scaleUpImage(400, 400, resizedImage));

        this.originalImage.setIcon(scaleImage(originalImage, 170, 170));
        imageLabel.setIcon(resizedImage);
        int size = inputFiles == null ? 0 : inputFiles.length;
        titleImage.setText(String.format("%s / %s  %s", currentFileIndex + 1, size, currentFile.getName()));
    }

    private BufferedImage scaleUpImage(int width, int height, ImageIcon resizedImage) {

        Image image = resizedImage.getImage(); // transform it
        Image upScaledImage = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way

        BufferedImage bi = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        // paint the Icon to the BufferedImage.
        new ImageIcon(upScaledImage).paintIcon(null, g, 0, 0);
        g.dispose();

        Color color;

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                color = new Color(bi.getRGB(x, y), true);

                if (color.equals(Color.white)) {

                    bi.setRGB(x, y, Color.white.getRGB());

                } else {

                    bi.setRGB(x, y, Color.black.getRGB());

                }
            }
        }

        return bi;

    }

    private BufferedImage removeBackground(ImageIcon imageIcon) {

        BufferedImage bi = new BufferedImage(
                imageIcon.getIconWidth(),
                imageIcon.getIconHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        // paint the Icon to the BufferedImage.
        imageIcon.paintIcon(null, g, 0, 0);
        g.dispose();

        int index = 0;

        for (int y = 0; y < imageIcon.getIconHeight(); y++) {

            for (int x = 0; x < imageIcon.getIconWidth(); x++) {


                if (this.predictionResults[index++] > this.improvementLevel) {

                    bi.setRGB(x, y, Color.white.getRGB());

                }
            }
        }

        return bi;
    }

    private float[] makePrediction(float[] resizedImageFloat) {


        float[] output = new float[100352];

        String outputName = "activation_98/truediv:0";

        tfSession.runner()
                .feed("input_1_1:0", Tensor.create(new long[]{1, IMAGE_SCALE_SIZE, IMAGE_SCALE_SIZE, 3}, FloatBuffer.wrap(resizedImageFloat)))
                .fetch(outputName)
                .run()
                .get(0)
                .writeTo(FloatBuffer.wrap(output));

        float[] foregroundProbability = new float[50176];

        for (int i = 0, j = 0; i < output.length; i += 2, j++) {

            foregroundProbability[j] = output[i];

        }

        return foregroundProbability;

    }

    private void normalizeFloatArray(float[] imageFloats) {

        for (int i = 0; i < imageFloats.length; i++) {

            imageFloats[i] = imageFloats[i] / 255;

        }

    }

    private float[] convertImageIconToFloatArray(ImageIcon imageIcon) {

        BufferedImage bi = new BufferedImage(
                imageIcon.getIconWidth(),
                imageIcon.getIconHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.createGraphics();
        // paint the Icon to the BufferedImage.
        imageIcon.paintIcon(null, g, 0, 0);
        g.dispose();

        float[] imageBytesFloat = new float[imageIcon.getIconHeight() * imageIcon.getIconWidth() * 3];

        Color color;

        int index = 0;

        for (int y = 0; y < imageIcon.getIconHeight(); y++) {

            for (int x = 0; x < imageIcon.getIconWidth(); x++) {

                color = new Color(bi.getRGB(x, y), true);

                imageBytesFloat[index * 3] = color.getRed();
                imageBytesFloat[index * 3 + 1] = color.getGreen();
                imageBytesFloat[index * 3 + 2] = color.getBlue();

                index++;
            }

        }

        return imageBytesFloat;
    }

    private ImageIcon scaleImage(ImageIcon imageIcon, int width, int height) {

        Image image = imageIcon.getImage(); // transform it
        Image newimg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH); // scale it the smooth way

        return new ImageIcon(newimg);

    }


}
