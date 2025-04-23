import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.sql.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;


public class HomePage {
    private static JFrame frame;
    private static JPanel panel;
    private static JPanel buttonPanel;
    private static ArrayList<String> apps = new ArrayList<>();  // Liste des applications

    public static void createHomePage(int userId, String username) {
        // Créer la fenêtre principale
        frame = new JFrame("Page d'accueil - " + username);
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setLocationRelativeTo(null); // Centrer la fenêtre

        // Créer le panel principal (vertical)
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Titre
        JLabel titleLabel = new JLabel("Bienvenue " + username + " !");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createVerticalStrut(10)); // Espacement
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10)); // Espacement

        // Barre de saisie avec champ texte + bouton
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JTextField appTextField = new JTextField(20);
        JButton addButton = new JButton("Ajouter Application");
        inputPanel.add(appTextField);
        inputPanel.add(addButton);
        panel.add(inputPanel);

        // Panel pour les boutons d'applications
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(buttonPanel);

        // Action pour ajouter une appli
        addButton.addActionListener(e -> {
            String appName = appTextField.getText().trim();
            if (!appName.isEmpty()) {
                addApplication(userId, appName);
                appTextField.setText("");
                updateAppList(userId);
            } else {
                JOptionPane.showMessageDialog(frame, "Veuillez entrer un nom d'application.");
            }
        });


        // Rendre défilable si beaucoup de contenu
        JScrollPane scrollPane = new JScrollPane(panel);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);

        updateAppList(userId);
    }

    // Rafraîchir la liste de boutons d'applications
    public static void updateAppList() {
        buttonPanel.removeAll();

        for (String app : apps) {
            JButton appButton = new JButton(app);
            appButton.addActionListener(e ->
                    JOptionPane.showMessageDialog(frame, "Vous avez lancé l'application : " + app)
            );
            buttonPanel.add(appButton);
        }

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    public static void addApplication(int userId, String name) {
        String url = "jdbc:sqlite:app_data.db";
        String sql = "INSERT INTO applications (user_id, name) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, name);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("❌ Erreur ajout app : " + e.getMessage());
        }
    }

    public static void updateAppList(int userId) {
        apps.clear();
        String url = "jdbc:sqlite:app_data.db";
        String sql = "SELECT name FROM applications WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                apps.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur lecture apps : " + e.getMessage());
        }

        buttonPanel.removeAll();
        for (String app : apps) {
            JButton appButton = new JButton(app);
            appButton.addActionListener(e -> openAppManager(userId, app)); // ✅ ici
            buttonPanel.add(appButton);
        }

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }


    public static void openAppManager(int userId, String appName) {
        JDialog dialog = new JDialog(frame, "Fichiers de l'application : " + appName, true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));

        JButton addFileButton = new JButton("Ajouter un fichier");
        addFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                saveFileToDatabase(userId, appName, selectedFile);
                refreshFileList(filePanel, userId, appName);
            }
        });

        dialog.add(addFileButton, BorderLayout.NORTH);
        dialog.add(new JScrollPane(filePanel), BorderLayout.CENTER);

        refreshFileList(filePanel, userId, appName);

        dialog.setVisible(true);
    }
    public static void saveFileToDatabase(int userId, String appName, File file) {
        String url = "jdbc:sqlite:app_data.db";
        String sql = "INSERT INTO files (app_id, filename, content) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            // Trouver app_id
            String getAppIdSql = "SELECT id FROM applications WHERE user_id = ? AND name = ?";
            PreparedStatement getAppIdStmt = conn.prepareStatement(getAppIdSql);
            getAppIdStmt.setInt(1, userId);
            getAppIdStmt.setString(2, appName);
            ResultSet rs = getAppIdStmt.executeQuery();

            if (rs.next()) {
                int appId = rs.getInt("id");
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, appId);
                pstmt.setString(2, file.getName());

                FileInputStream fis = new FileInputStream(file);
                pstmt.setBinaryStream(3, fis, (int) file.length());
                pstmt.executeUpdate();
                fis.close();
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    public static void refreshFileList(JPanel filePanel, int userId, String appName) {
        filePanel.removeAll();

        String url = "jdbc:sqlite:app_data.db";
        String sql = "SELECT files.id, files.filename FROM files " +
                "JOIN applications ON files.app_id = applications.id " +
                "WHERE applications.user_id = ? AND applications.name = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, appName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int fileId = rs.getInt("id");
                String filename = rs.getString("filename");

                JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel label = new JLabel(filename);
                JButton downloadButton = new JButton("Télécharger");

                downloadButton.addActionListener(e -> downloadFileById(fileId, filename));

                fileRow.add(label);
                fileRow.add(downloadButton);
                filePanel.add(fileRow);
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur chargement fichiers : " + e.getMessage());
        }

        filePanel.revalidate();
        filePanel.repaint();
    }

    public static void downloadFileById(int fileId, String filename) {
        String url = "jdbc:sqlite:app_data.db";
        String sql = "SELECT content FROM files WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, fileId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                byte[] fileData = rs.getBytes("content");

                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(filename));
                int option = chooser.showSaveDialog(frame);

                if (option == JFileChooser.APPROVE_OPTION) {
                    File saveFile = chooser.getSelectedFile();
                    FileOutputStream fos = new FileOutputStream(saveFile);
                    fos.write(fileData);
                    fos.close();
                    JOptionPane.showMessageDialog(frame, "Fichier téléchargé avec succès !");
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur téléchargement : " + e.getMessage());
        }
    }
}
