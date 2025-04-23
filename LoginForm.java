import javax.swing.*;
import java.sql.*;

public class LoginForm {
    private static JFrame frame;
    private static JPanel contentPanel;

    public static void main(String[] args) {
        initializeDatabase(); // 💾 Initialise la base avant tout

        // Fenêtre principale
        frame = new JFrame("Application");
        frame.setSize(500, 350);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setLocationRelativeTo(null); // Centrer

        // Panel principal qui va contenir soit login soit register
        contentPanel = new JPanel();
        contentPanel.setBounds(0, 0, 500, 350);
        contentPanel.setLayout(null);
        frame.add(contentPanel);

        // Afficher d'abord le formulaire de connexion
        showLoginForm();

        frame.setVisible(true);
    }

    public static void showLoginForm() {
        contentPanel.removeAll(); // Vider l'ancien contenu

        JLabel titleLabel = new JLabel("Connexion", SwingConstants.CENTER);
        titleLabel.setBounds(50, 20, 400, 30);
        contentPanel.add(titleLabel);

        JLabel userLabel = new JLabel("Nom d'utilisateur:");
        userLabel.setBounds(100, 70, 120, 25);
        contentPanel.add(userLabel);

        JTextField userText = new JTextField();
        userText.setBounds(220, 70, 180, 25);
        contentPanel.add(userText);

        JLabel passwordLabel = new JLabel("Mot de passe:");
        passwordLabel.setBounds(100, 110, 120, 25);
        contentPanel.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField();
        passwordText.setBounds(220, 110, 180, 25);
        contentPanel.add(passwordText);

        JButton loginButton = new JButton("Connexion");
        loginButton.setBounds(140, 170, 100, 30);
        contentPanel.add(loginButton);

        JButton switchToRegister = new JButton("Créer un compte");
        switchToRegister.setBounds(260, 170, 140, 30);
        contentPanel.add(switchToRegister);

        // Bouton pour passer à l'inscription
        switchToRegister.addActionListener(e -> showRegisterForm());

        // Action : Vérification de la connexion
        loginButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passwordText.getPassword());

            int userId = authenticateUser(username, password);
            if (userId != -1) {
                frame.dispose(); // Ferme la fenêtre actuelle
                HomePage.createHomePage(userId, username); // Ouvre la nouvelle fenêtre
            } else {
                JOptionPane.showMessageDialog(frame, "Nom d'utilisateur ou mot de passe incorrect.");
            }
        });

        contentPanel.revalidate();
        contentPanel.repaint();
    }


    public static void showRegisterForm() {
        contentPanel.removeAll(); // Vider l'ancien contenu

        JLabel titleLabel = new JLabel("Créer un compte", SwingConstants.CENTER);
        titleLabel.setBounds(50, 20, 400, 30);
        contentPanel.add(titleLabel);

        JLabel userLabel = new JLabel("Nom d'utilisateur:");
        userLabel.setBounds(100, 70, 120, 25);
        contentPanel.add(userLabel);

        JTextField userText = new JTextField();
        userText.setBounds(230, 70, 160, 25);
        contentPanel.add(userText);

        JLabel passwordLabel = new JLabel("Mot de passe:");
        passwordLabel.setBounds(100, 110, 120, 25);
        contentPanel.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField();
        passwordText.setBounds(230, 110, 160, 25);
        contentPanel.add(passwordText);

        JLabel confirmPasswordLabel = new JLabel("Confirmer mot de passe:");
        confirmPasswordLabel.setBounds(60, 150, 160, 25);
        contentPanel.add(confirmPasswordLabel);

        JPasswordField confirmPasswordText = new JPasswordField();
        confirmPasswordText.setBounds(230, 150, 160, 25);
        contentPanel.add(confirmPasswordText);

        JButton registerButton = new JButton("S'inscrire");
        registerButton.setBounds(140, 210, 100, 30);
        contentPanel.add(registerButton);

        JButton backToLogin = new JButton("Retour");
        backToLogin.setBounds(260, 210, 100, 30);
        contentPanel.add(backToLogin);

        // Action : retour au formulaire de connexion
        backToLogin.addActionListener(e -> showLoginForm());

        // Action : inscription
        registerButton.addActionListener(e -> {
            String username = userText.getText();
            String pass1 = new String(passwordText.getPassword());
            String pass2 = new String(confirmPasswordText.getPassword());

            if (username.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Veuillez remplir tous les champs.");
            } else if (!pass1.equals(pass2)) {
                JOptionPane.showMessageDialog(frame, "Les mots de passe ne correspondent pas.");
            } else {
                boolean success = registerUser(username, pass1);
                if (success) {
                    JOptionPane.showMessageDialog(frame, "Compte créé avec succès !");
                    showLoginForm(); // Retour à la connexion
                } else {
                    JOptionPane.showMessageDialog(frame, "Erreur : ce nom d'utilisateur existe déjà.");
                }
            }
        });

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // 📌 Créer la base et la table
    public static void initializeDatabase() {
        String url = "jdbc:sqlite:app_data.db";

        String usersSql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL" +
                ");";

        String appsSql = "CREATE TABLE IF NOT EXISTS applications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "name TEXT," +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ");";

        String filesSql = "CREATE TABLE IF NOT EXISTS files (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "app_id INTEGER," +
                "filename TEXT," +
                "content BLOB," +
                "FOREIGN KEY(app_id) REFERENCES applications(id)" +
                ");";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.execute(usersSql);
            stmt.execute(appsSql);
            stmt.execute(filesSql);
            System.out.println("✅ Base de données prête.");

        } catch (SQLException e) {
            System.out.println("❌ Erreur SQLite : " + e.getMessage());
        }
    }


    // 🔐 Ajouter un utilisateur dans la table users
    public static boolean registerUser(String username, String password) {
        String url = "jdbc:sqlite:app_data.db";
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de l'inscription : " + e.getMessage());
            return false;
        }
    }

    public static int authenticateUser(String username, String password) {
        String url = "jdbc:sqlite:app_data.db";
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            } else {
                return -1; // utilisateur non trouvé
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur lors de la connexion à la base de données : " + e.getMessage());
            return -1;
        }
    }
}
