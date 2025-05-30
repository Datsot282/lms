package com.example.lms.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class User {

    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty surname;
    private final StringProperty idcardno;
    private final StringProperty password;
    private final StringProperty role;
    private final ObjectProperty<LocalDateTime> createdAt;

    public User(int id, String name, String surname, String idcardno, String password, String role, LocalDateTime createdAt) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.surname = new SimpleStringProperty(surname);
        this.idcardno = new SimpleStringProperty(idcardno);
        this.password = new SimpleStringProperty(password);
        this.role = new SimpleStringProperty(role);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getSurname() { return surname.get(); }
    public StringProperty surnameProperty() { return surname; }

    public String getIdcardno() { return idcardno.get(); }
    public StringProperty idcardnoProperty() { return idcardno; }

    public String getPassword() { return password.get(); }
    public StringProperty passwordProperty() { return password; }

    public String getRole() { return role.get(); }
    public StringProperty roleProperty() { return role; }

    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
}
