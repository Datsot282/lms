package com.example.lms.controller;

import com.example.lms.model.Transcript;
import com.example.lms.connection.DBConnection;
import com.itextpdf.text.Font;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;

public class TranscriptController {

    @FXML private TableView<Transcript> transcriptTable;
    @FXML private TableColumn<Transcript, Integer> idColumn;
    @FXML private TableColumn<Transcript, String> nameColumn;
    @FXML private TableColumn<Transcript, String> courseColumn;
    @FXML private TableColumn<Transcript, String> gradeColumn;
    @FXML private TableColumn<Transcript, String> moduleColumn;
    @FXML private TableColumn<Transcript, String> assignmentColumn;
    @FXML private TableColumn<Transcript, String> submissionDateColumn;


    private final ObservableList<Transcript> transcriptList = FXCollections.observableArrayList();

    private String loggedInRole;
    private String loggedInName;


    @FXML
    public void initialize() {
        //idColumn.setCellValueFactory(new PropertyValueFactory<>("student_Id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        courseColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        moduleColumn.setCellValueFactory(new PropertyValueFactory<>("moduleName"));
        assignmentColumn.setCellValueFactory(new PropertyValueFactory<>("assignmentTitle"));
        submissionDateColumn.setCellValueFactory(new PropertyValueFactory<>("submissionDate"));

        loadTranscriptData();
    }

    public void setUserContext(String name, String role) {
        this.loggedInName = name;
        this.loggedInRole = role;

        //gradeBtn.setDisable(isStudent);
        //answerCol.setVisible(!isStudent());
        //studentNameField.setVisible(!isStudent());
        //assignmentComboBox.setVisible(!isStudent());
        //timeLimitField.setVisible(!isStudent());
        //generateQuizButton.setVisible(!isStudent());
        //answerCol.setDisable(isStudent());
        //addAssignmentButton.setDisable(isStudent());
        //questionField.setDisable(isStudent());
        //optionAField.setDisable(isStudent());
        //optionBField.setDisable(isStudent());
        //optionCField.setDisable(isStudent());
        //optionDField.setDisable(isStudent());
        //correctOptionCombo.setDisable(isStudent());
        //addQuestionButton.setDisable(isStudent());

    }

    private void loadTranscriptData() {
        String query = "SELECT \n" +
                "    sub.student_name, \n" +
                "    c.name AS course_name, \n" +
                "    sub.grade AS submission_grade,\n" +
                "    mod.name AS module_name,\n" +
                "    a.title AS assignment_title,\n" +
                "    sub.submitted_at\n" +
                "FROM users.assignments a\n" +
                "LEFT JOIN users.submissions sub ON sub.assignment_id = a.id\n" +
                "JOIN users.module mod ON a.module_id = mod.id\n" +
                "JOIN users.course c ON mod.course_id = c.id;\n";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                transcriptList.add(new Transcript(
                        //rs.getInt("student_id"),
                        rs.getString("student_name"),
                        rs.getString("course_name"),
                        rs.getString("module_name"),
                        rs.getString("assignment_title"),
                        rs.getString("submitted_at"),
                        rs.getString("submission_grade")
                ));
            }

            transcriptTable.setItems(transcriptList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrintTranscript() {
        Document document = new Document();
        try {
            // Save to user's Documents folder
            String fileName = System.getProperty("user.home") + "/Documents/transcript_output.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            Paragraph title = new Paragraph("Student Transcript Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Table with updated headers
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 4, 4, 4, 5, 4, 2});

            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            table.addCell(new PdfPCell(new Phrase("ID", headFont)));
            table.addCell(new PdfPCell(new Phrase("Student Name", headFont)));
            table.addCell(new PdfPCell(new Phrase("Course", headFont)));
            table.addCell(new PdfPCell(new Phrase("Module", headFont)));
            table.addCell(new PdfPCell(new Phrase("Assignment", headFont)));
            table.addCell(new PdfPCell(new Phrase("Submitted At", headFont)));
            table.addCell(new PdfPCell(new Phrase("Grade", headFont)));

            // Add rows from transcriptList
            for (Transcript t : transcriptList) {
                //table.addCell(String.valueOf(t.getStudentId()));
                table.addCell(t.getStudentName());
                table.addCell(t.getCourseName());
                table.addCell(t.getModuleName());
                table.addCell(t.getAssignmentTitle());
                table.addCell(t.getSubmissionDate());
                table.addCell(t.getGrade());
            }

            document.add(table);
            document.close();

            System.out.println("Transcript PDF generated successfully at: " + fileName);

            // Preview PDF in system viewer
            File pdfFile = new File(fileName);
            if (pdfFile.exists()) {
                Desktop.getDesktop().open(pdfFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleCertificate() {
        Transcript selected = transcriptTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            System.out.println("Please select a student transcript to generate the certificate.");
            return;
        }

        Document document = new Document(PageSize.A4.rotate());
        try {
            String fileName = System.getProperty("user.home") + "/Documents/certificate_" + selected.getStudentName().replace(" ", "_") + ".pdf";
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 36, Font.BOLD, BaseColor.BLUE);
            Paragraph title = new Paragraph("Certificate of Completion", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(50);
            document.add(title);

            // Body text
            Font bodyFont = new Font(Font.FontFamily.HELVETICA, 20, Font.NORMAL);
            Paragraph body = new Paragraph(
                    String.format("This certificate is proudly presented to\n\n%s\n\nfor the successful completion of the course\n\n%s\n\nwith commendable performance.",
                            selected.getStudentName(), selected.getCourseName()),
                    bodyFont);
            body.setAlignment(Element.ALIGN_CENTER);
            body.setSpacingAfter(40);
            document.add(body);

            // Empowering Message
            Font messageFont = new Font(Font.FontFamily.COURIER, 16, Font.ITALIC, BaseColor.DARK_GRAY);
            Paragraph message = new Paragraph(
                    "\"Education is the most powerful weapon which you can use to change the world.\"\n\nKeep pushing your limits and reach for greatness!",
                    messageFont);
            message.setAlignment(Element.ALIGN_CENTER);
            document.add(message);

            // Footer
            Paragraph footer = new Paragraph("\n\nDate Issued: " + java.time.LocalDate.now(), bodyFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            System.out.println("Certificate generated: " + fileName);

            File certFile = new File(fileName);
            if (certFile.exists()) {
                Desktop.getDesktop().open(certFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
