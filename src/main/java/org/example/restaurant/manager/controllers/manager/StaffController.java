package org.example.restaurant.manager.controllers.manager; 
// Package : ce contrôleur appartient à la partie "manager" de l'application.

/* ===================== IMPORTS ===================== */

import javafx.beans.value.ChangeListener;              // Pour écouter les changements sur les champs de texte.
import javafx.collections.FXCollections;               // Méthodes utilitaires pour créer des ObservableList.
import javafx.collections.ObservableList;              // Liste observable, idéale pour TableView.
import javafx.fxml.FXML;                               // Annotation pour lier le code au fichier FXML.
import javafx.scene.Node;                              // Classe de base pour les éléments graphiques.
import javafx.scene.control.*;                         // Contrôles UI : TableView, Button, Dialog, Alert, etc.
import javafx.scene.control.cell.PropertyValueFactory; // Lie les colonnes aux propriétés du modèle (User).
import javafx.scene.layout.GridPane;                   // Layout en grille, utilisé pour les formulaires.
import javafx.scene.layout.HBox;                       // Layout horizontal, utilisé pour les boutons d'action.
import javafx.geometry.Insets;                         // Marges/padding autour des éléments.
import org.example.restaurant.manager.dao.UserDAO;     // DAO pour accéder à la table des utilisateurs en base.
import org.example.restaurant.manager.models.User;     // Modèle représentant un utilisateur (staff).
import org.example.restaurant.manager.utils.DatabaseConfig; // Classe utilitaire pour obtenir une connexion DB.
import org.example.restaurant.manager.utils.PasswordUtils;  // Pour hasher les mots de passe.

import java.security.NoSuchAlgorithmException;         // (Non utilisé ici) lié au hashing.
import java.sql.Connection;                            // Représente une connexion à la base de données.
import java.sql.SQLException;                          // Exceptions liées à SQL.
import java.util.Optional;                             // Conteneur pour résultats optionnels (Dialog, Alert).

/* ===================== CLASSE CONTROLEUR ===================== */

public class StaffController {

    // Liés au TableView dans le FXML
    @FXML private TableView<User> staffTable;          // Tableau affichant la liste du staff.
    @FXML private TableColumn<User, Integer> idColumn; // Colonne ID.
    @FXML private TableColumn<User, String> nameColumn;// Colonne Nom.
    @FXML private TableColumn<User, String> emailColumn;// Colonne Email.
    @FXML private TableColumn<User, String> roleColumn;// Colonne Rôle.
    @FXML private TableColumn<User, Void> actionColumn;// Colonne Actions (Modifier / Supprimer).

    private UserDAO userDAO;                           // Objet d’accès aux données pour les utilisateurs.
    private ObservableList<User> staffList = FXCollections.observableArrayList();
    // Liste observable qui contient les utilisateurs affichés dans le tableau.

    /* ========== INITIALISATION DU CONTROLEUR ========== */

    @FXML
    public void initialize() {
        try {
            // Récupère une connexion à la base de données.
            Connection connection = DatabaseConfig.getConnection();
            // Initialise le DAO avec cette connexion.
            userDAO = new UserDAO(connection);
            // Configure les colonnes du TableView.
            setupTableColumns();
            // Charge les données du staff depuis la base.
            loadStaffData();
        } catch (SQLException e) {
            // Si la connexion échoue, affiche un message d'erreur.
            showAlert("Erreur", "Impossible de se connecter à la base de données", Alert.AlertType.ERROR);
        }
    }

    /* ========== CONFIGURATION DES COLONNES ========== */

    private void setupTableColumns() {
        // Associe chaque colonne à une propriété de la classe User (via ses getters).
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("roleName"));

        // Configure la colonne des boutons d'action.
        setupActionButtons();
    }

    /* ========== CHARGEMENT DES DONNÉES DU STAFF ========== */

    private void loadStaffData() {
        try {
            // Récupère tous les utilisateurs de rôle 2 (staff) et met la liste dans staffList.
            staffList.setAll(userDAO.getUsersByRole(2));
            // Associe la liste observable au TableView pour affichage.
            staffTable.setItems(staffList);
        } catch (SQLException e) {
            // En cas d'erreur SQL, affiche un message.
            showAlert("Erreur", "Échec du chargement du personnel", Alert.AlertType.ERROR);
        }
    }

    /* ========== AJOUT D’UN NOUVEAU STAFF ========== */

    @FXML
    private void handleAddStaff() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un membre du staff");

        // Création des boutons de la boîte de dialogue.
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Création du formulaire sous forme de grille.
        GridPane grid = new GridPane();
        grid.setHgap(10);                      // Espacement horizontal entre colonnes.
        grid.setVgap(10);                      // Espacement vertical entre lignes.
        grid.setPadding(new Insets(20));       // Marges autour de la grille.

        // Champs de saisie.
        TextField nameField = new TextField(); // Nom.
        TextField emailField = new TextField();// Email.
        PasswordField passwordField = new PasswordField();         // Mot de passe.
        PasswordField confirmPasswordField = new PasswordField();  // Confirmation mot de passe.

        // Ajoute les labels et champs dans la grille.
        grid.addRow(0, new Label("Nom:"), nameField);
        grid.addRow(1, new Label("Email:"), emailField);
        grid.addRow(2, new Label("Mot de passe:"), passwordField);
        grid.addRow(3, new Label("Confirmation:"), confirmPasswordField);

        // Récupère le bouton "Enregistrer" pour pouvoir le (dés)activer.
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true); // Désactivé tant que le formulaire n’est pas valide.

        // Listener qui valide le formulaire à chaque changement de texte.
        ChangeListener<String> validator = (obs, oldVal, newVal) -> {
            boolean valid = !nameField.getText().isEmpty()           // nom non vide
                    && !emailField.getText().isEmpty()              // email non vide
                    && passwordField.getText().length() >= 6        // mot de passe ≥ 6 caractères
                    && passwordField.getText().equals(confirmPasswordField.getText()); // mots de passe identiques
            saveButton.setDisable(!valid); // active/désactive le bouton selon la validité.
        };

        // Attache le même validateur à tous les champs.
        nameField.textProperty().addListener(validator);
        emailField.textProperty().addListener(validator);
        passwordField.textProperty().addListener(validator);
        confirmPasswordField.textProperty().addListener(validator);

        // Place le formulaire dans la boîte de dialogue.
        dialog.getDialogPane().setContent(grid);

        // Définit ce que renvoie la boîte de dialogue quand on clique sur un bouton.
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                // Crée un nouvel objet User si on a cliqué sur "Enregistrer".
                return new User(0, nameField.getText(), emailField.getText(), 2, "Staff", passwordField.getText());
            }
            return null; // Sinon (Annuler), renvoie null.
        });

        // Affiche la boîte de dialogue et traite le résultat.
        dialog.showAndWait().ifPresent(newStaff -> {
            try {
                // Vérifie si l'email existe déjà en base.
                if (userDAO.emailExists(newStaff.getEmail())) {
                    showAlert("Erreur", "Cet email existe déjà", Alert.AlertType.ERROR);
                    return;
                }

                // Hash le mot de passe avant de l'enregistrer.
                newStaff.setPassword(PasswordUtils.hashPassword(newStaff.getPassword()));
                // Ajoute le nouveau staff en base de données.
                userDAO.addStaff(newStaff);
                // Rafraîchit la liste affichée.
                refreshStaffList();
                // Informe l'utilisateur que tout s'est bien passé.
                showAlert("Succès", "Staff ajouté avec succès", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                // En cas d'erreur (SQL ou hashing), affiche un message.
                showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /* ========== COLONNE ACTIONS (MODIFIER / SUPPRIMER) ========== */

    private void setupActionButtons() {
        // Définit une cellule personnalisée pour la colonne "actionColumn".
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");   // Bouton modifier.
            private final Button deleteBtn = new Button("Supprimer");// Bouton supprimer.
            private final HBox buttons = new HBox(10, editBtn, deleteBtn); // Les deux boutons côte à côte.

            {
                // Ajoute des classes CSS pour le style.
                editBtn.getStyleClass().add("edit-button");
                deleteBtn.getStyleClass().add("delete-button");

                // Action du bouton Modifier : appelle editStaff sur la ligne courante.
                editBtn.setOnAction(event -> 
                    editStaff(getTableView().getItems().get(getIndex()))
                );
                // Action du bouton Supprimer : appelle deleteStaff sur la ligne courante.
                deleteBtn.setOnAction(event -> 
                    deleteStaff(getTableView().getItems().get(getIndex()))
                );
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                // Si la ligne est vide, pas de boutons. Sinon, affiche le HBox.
                setGraphic(empty ? null : buttons);
            }
        });
    }

    /* ========== MODIFICATION D’UN STAFF ========== */

    private void editStaff(User staff) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Modifier le staff");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Formulaire simple (nom + email).
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(staff.getName());   // pré-rempli avec le nom existant.
        TextField emailField = new TextField(staff.getEmail()); // pré-rempli avec l'email existant.

        grid.addRow(0, new Label("Nom:"), nameField);
        grid.addRow(1, new Label("Email:"), emailField);

        dialog.getDialogPane().setContent(grid);

        // Si on clique sur Enregistrer, met à jour l'objet staff.
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                staff.setName(nameField.getText());
                staff.setEmail(emailField.getText());
                return staff;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedStaff -> {
            try {
                // Met à jour le staff en base de données.
                userDAO.updateStaff(updatedStaff);
                // Rafraîchit la liste dans le TableView.
                refreshStaffList();
                showAlert("Succès", "Staff modifié avec succès", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la modification", Alert.AlertType.ERROR);
            }
        });
    }

    /* ========== SUPPRESSION D’UN STAFF ========== */

    private void deleteStaff(User staff) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer " + staff.getName() + "?");
        alert.setContentText("Cette action est irréversible.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Supprime en base l'utilisateur avec cet ID.
                    userDAO.deleteStaff(staff.getId());
                    // Rafraîchit la liste affichée.
                    refreshStaffList();
                    showAlert("Succès", "Staff supprimé avec succès", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur lors de la suppression", Alert.AlertType.ERROR);
                }
            }
        });
    }

    /* ========== RAFRAÎCHISSEMENT DE LA LISTE ========== */

    private void refreshStaffList() {
        try {
            // Recharge les utilisateurs de rôle 2 et met à jour la liste observable.
            staffList.setAll(userDAO.getUsersByRole(2));
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du rafraîchissement", Alert.AlertType.ERROR);
        }
    }

    /* ========== METHODE UTILITAIRE POUR LES ALERTES ========== */

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);   // Crée une alerte du type demandé (ERREUR, INFO…).
        alert.setTitle(title);           // Titre de la fenêtre.
        alert.setHeaderText(null);       // Pas de sous-titre.
        alert.setContentText(message);   // Message à afficher.
        alert.showAndWait();             // Affiche et attend que l'utilisateur ferme l'alerte.
    }
}
