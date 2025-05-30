package com.example.lms.controller;

import com.example.lms.model.Question;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class RadioButtonCellFactory implements Callback<TableColumn<Question, String>, TableCell<Question, String>> {

    @Override
    public TableCell<Question, String> call(TableColumn<Question, String> param) {
        return new TableCell<Question, String>() {  // 👈 Specify the types here
            private final ToggleGroup group = new ToggleGroup();
            private final RadioButton rbA = new RadioButton("A");
            private final RadioButton rbB = new RadioButton("B");
            private final RadioButton rbC = new RadioButton("C");
            private final RadioButton rbD = new RadioButton("D");
            private final HBox hbox = new HBox(10, rbA, rbB, rbC, rbD);

            {
                rbA.setToggleGroup(group);
                rbB.setToggleGroup(group);
                rbC.setToggleGroup(group);
                rbD.setToggleGroup(group);

                group.selectedToggleProperty().addListener((obs, old, newToggle) -> {
                    Question currentQuestion = getTableView().getItems().get(getIndex());

                    if (currentQuestion != null) { // 👈 Always check for null
                        if (newToggle == rbA) {
                            currentQuestion.setSelectedOption("A");
                        } else if (newToggle == rbB) {
                            currentQuestion.setSelectedOption("B");
                        } else if (newToggle == rbC) {
                            currentQuestion.setSelectedOption("C");
                        } else if (newToggle == rbD) {
                            currentQuestion.setSelectedOption("D");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);

                    // Get the current question
                    Question currentQuestion = getTableView().getItems().get(getIndex());

                    // Set the selected radio button if it matches the stored selection
                    if (currentQuestion != null) {
                        switch (currentQuestion.getSelectedOption()) {
                            case "A":
                                rbA.setSelected(true);
                                break;
                            case "B":
                                rbB.setSelected(true);
                                break;
                            case "C":
                                rbC.setSelected(true);
                                break;
                            case "D":
                                rbD.setSelected(true);
                                break;
                            default:
                                group.selectToggle(null); // Deselect all if no match
                                break;
                        }
                    }
                }
            }
        };
    }

}
