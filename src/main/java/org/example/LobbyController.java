package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class LobbyController {
    @FXML
    private ListView<String> userListView;
    @FXML
    private TextField nicknameField;
    @FXML
    private Button addButton;
    @FXML
    private Button removeButton;

    private ObservableList<String> userList;

    @FXML
    public void initialize() {
        userList = FXCollections.observableArrayList();
        userList.addAll("JanKowalski", "AnnaNowak", "PiotrWiśniewski", "MariaLewandowska");
        userListView.setItems(userList);
        userListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        userListView.setTooltip(new Tooltip("Lista użytkowników - kliknij dwukrotnie, aby edytować"));
    }
    @FXML
    private void addUser() {
        String nickname = nicknameField.getText().trim();

        if (!nickname.isEmpty() && !userList.contains(nickname)) {
            userList.add(nickname);
            nicknameField.clear();
            nicknameField.requestFocus();
        }
    }

    public void addUser(String nickname) {
        if (nickname != null && !nickname.trim().isEmpty() && !userList.contains(nickname)) {
            userList.add(nickname);
        }
    }
    @FXML
    private void removeSelectedUser() {
        int selectedIndex = userListView.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            userList.remove(selectedIndex);
        }
    }
    public ObservableList<String> getUserList() {
        return userList;
    }

    public void setUserList(ObservableList<String> userList) {
        this.userList = userList;
        userListView.setItems(userList);
    }
}