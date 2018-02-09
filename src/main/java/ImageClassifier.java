import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ImageClassifier {
    private final File[] inputFiles;
    private int currentFileIndex = 0;
    private File currentFile;

    private JPanel panel1;
    private JButton validButton;
    private JButton invalidButton;
    private JLabel imageLabel;
    private JLabel titleImage;
    private static String outputDir = "./files/output/";
    private static String validOutputDir = "valid/";
    private static String invalidOutputDir = "invalid/";
    private static String inputDir = "./files/input/";


    public static void main(String[] args) {

        JFrame frame = new JFrame("ImageClassifier");

        frame.setContentPane(new ImageClassifier().panel1);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

    public ImageClassifier() {

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

                System.out.println("Hello");
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

    private void createOutputDirs() {
        new File(outputDir).mkdirs();
        new File(outputDir + validOutputDir).mkdirs();
        new File(outputDir + invalidOutputDir).mkdirs();
    }

    private void moveToNextImage() {
        currentFileIndex++;

        ImageClassifier.this.currentFile = inputFiles[currentFileIndex];

        updateDisplayedImage();
    }

    private void updateDisplayedImage() {
        ImageIcon imageIcon = new ImageIcon(currentFile.getAbsolutePath());

        //scale the image.
        Image image = imageIcon.getImage(); // transform it
        Image newimg = image.getScaledInstance(400, 400,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        imageIcon = new ImageIcon(newimg);

        imageLabel.setIcon(imageIcon);
        int size = inputFiles == null ? 0 : inputFiles.length;
        titleImage.setText(String.format("%s / %s  %s", currentFileIndex + 1, size, currentFile.getName()));
    }


}
