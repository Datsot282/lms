package com.example.lms.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Question {
    private final int id;
    private final StringProperty questionText;
    private final StringProperty optionA;
    private final StringProperty optionB;
    private final StringProperty optionC;
    private final StringProperty optionD;
    private final StringProperty correctOption;
    private final StringProperty selectedOption;
    private final StringProperty answer;

    public Question(int id, String questionText, String optionA, String optionB, String optionC, String optionD, String correctOption, String answer) {
        this.id = id;
        this.questionText = new SimpleStringProperty(questionText);
        this.optionA = new SimpleStringProperty(optionA);
        this.optionB = new SimpleStringProperty(optionB);
        this.optionC = new SimpleStringProperty(optionC);
        this.optionD = new SimpleStringProperty(optionD);
        this.correctOption = new SimpleStringProperty(correctOption);
        this.selectedOption = new SimpleStringProperty();  // This is for storing the selected answer
        this.answer = new SimpleStringProperty(answer);

    }


    public int getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText.get();
    }

    public StringProperty questionTextProperty() {
        return questionText;
    }

    public String getOptionA() {
        return optionA.get();
    }

    public StringProperty optionAProperty() {
        return optionA;
    }

    public String getOptionB() {
        return optionB.get();
    }

    public StringProperty optionBProperty() {
        return optionB;
    }

    public String getOptionC() {
        return optionC.get();
    }

    public StringProperty optionCProperty() {
        return optionC;
    }

    public String getOptionD() {
        return optionD.get();
    }

    public StringProperty optionDProperty() {
        return optionD;
    }

    public String getCorrectOption() {
        return correctOption.get();
    }

    public StringProperty correctOptionProperty() {
        return correctOption;
    }

    public String getSelectedOption() {
        return selectedOption.get();
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption.set(selectedOption);
    }

    public StringProperty selectedOptionProperty() { return selectedOption; }

    public String getAnswer() { return answer.get();}


    public StringProperty AnswerProperty() { return answer ;}
}
