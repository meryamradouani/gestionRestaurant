package org.example.restaurant.manager.dao;

// DAO = Data Access Object : classe responsable de toutes les opérations SQL sur la table users.
import org.example.restaurant.manager.models.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // Connexion à la base de données utilisée par toutes les méthodes de ce DAO.
    private final Connection connection;

    // Le DAO reçoit une connexion déjà ouverte (Injection de dépendance).
    public UserDAO(Connection connection) {
        this.connection = connection;
    }

    // Récupère tous les staffs (role_id = 2)
    public List<User> getAllStaff() throws SQLException {
        // Réutilise la méthode générique getUsersByRole pour éviter de dupliquer du code.
        return getUsersByRole(2);
    }

    // Méthode générique pour récupérer les utilisateurs par rôle (admin, staff, etc.)
    public List<User> getUsersByRole(int roleId) throws SQLException {
        List<User> users = new ArrayList<>();

        // Requête SQL : jointure entre users et roles pour récupérer aussi le nom du rôle.
        String query = "SELECT u.id, u.name, u.email, u.password, u.role_id, r.name as role_name " +
                "FROM users u JOIN roles r ON u.role_id = r.id " +
                "WHERE u.role_id = ? ORDER BY u.name";

        // Utilisation de PreparedStatement pour sécuriser la requête et passer des paramètres.
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, roleId); // remplace le ? par la valeur roleId

            // Exécute la requête et parcourt le résultat.
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Pour chaque ligne, crée un objet User à partir des colonnes retournées.
                    users.add(new User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getInt("role_id"),
                            rs.getString("role_name"),
                            rs.getString("password") // mot de passe (déjà hashé en base)
                    ));
                }
            }
        }
        // Retourne la liste complète des utilisateurs trouvés.
        return users;
    }

    // Met à jour un membre du staff (nom + email uniquement)
    public void updateStaff(User staff) throws SQLException {
        String query = "UPDATE users SET name = ?, email = ? WHERE id = ? AND role_id = 2";

        // Prépare la requête de mise à jour.
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, staff.getName()); // nouveau nom
            pstmt.setString(2, staff.getEmail()); // nouvel email
            pstmt.setInt(3, staff.getId());       // id de l'utilisateur à modifier

            // Exécute la requête de mise à jour.
            pstmt.executeUpdate();
        }
    }

    // Supprime un membre du staff
    public void deleteStaff(int id) throws SQLException {
        String query = "DELETE FROM users WHERE id = ? AND role_id = 2";

        // Supprime uniquement l'utilisateur ayant cet id ET le rôle staff.
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // Vérifie l'existence d'un email (pour éviter les doublons)
    public boolean emailExists(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, email); // email à vérifier

            try (ResultSet rs = pstmt.executeQuery()) {
                // Si COUNT(*) > 0, l'email existe déjà.
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // Ajoute un nouveau membre du staff
    public void addStaff(User staff) throws SQLException {
        String query = "INSERT INTO users (name, email, password, role_id) VALUES (?, ?, ?, ?)";

        // RETURN_GENERATED_KEYS permet de récupérer l'id auto-généré par la base.
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, staff.getName());     // nom
            pstmt.setString(2, staff.getEmail());    // email
            pstmt.setString(3, staff.getPassword()); // mot de passe (déjà hashé avant l'appel)
            pstmt.setInt(4, staff.getRoleId());      // role_id (2 pour staff)

            int affectedRows = pstmt.executeUpdate(); // exécute l'INSERT

            // Si aucune ligne n’a été affectée, quelque chose s’est mal passé.
            if (affectedRows == 0) {
                throw new SQLException("Échec de l'ajout, aucune ligne affectée.");
            }

            // Récupère l'id généré par la base et le met dans l'objet User.
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    staff.setId(generatedKeys.getInt(1));
                }
            }
        }
    }
}
