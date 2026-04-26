package org.nelson.kidbank.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.nelson.kidbank.entity.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatementPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DateTimeFormatter TX_FMT   = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private static final DateTimeFormatter GEN_FMT  = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a");

    private static final Color COLOR_HEADER_BG  = new Color(233, 236, 239);
    private static final Color COLOR_DEPOSIT     = new Color(25, 135, 84);
    private static final Color COLOR_WITHDRAWAL  = new Color(220, 53, 69);
    private static final Color COLOR_INTEREST    = new Color(13, 110, 253);
    private static final Color COLOR_GRAY        = new Color(108, 117, 125);

    public byte[] generate(SavingsAccount account, User child,
                           List<Transaction> transactions, LocalDate from, LocalDate to) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER, 50, 50, 60, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            addHeader(doc, child, from, to);
            addTransactionTable(doc, transactions);
            addBalanceSummary(doc, account);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF statement.", e);
        }
    }

    private void addHeader(Document doc, User child, LocalDate from, LocalDate to) throws DocumentException {
        Font titleFont  = new Font(Font.HELVETICA, 20, Font.BOLD);
        Font subFont    = new Font(Font.HELVETICA, 13, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
        Font grayFont   = new Font(Font.HELVETICA, 9, Font.NORMAL, COLOR_GRAY);

        Paragraph title = new Paragraph("Nelson Car Quest Savings Statement", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph name = new Paragraph(child.getEffectiveName() + "  (@" + child.getUsername() + ")", subFont);
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(4);
        doc.add(name);

        Paragraph period = new Paragraph(
                "Period: " + from.format(DATE_FMT) + "  –  " + to.format(DATE_FMT), normalFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingBefore(3);
        doc.add(period);

        Paragraph generated = new Paragraph(
                "Generated: " + LocalDateTime.now().format(GEN_FMT), grayFont);
        generated.setAlignment(Element.ALIGN_CENTER);
        generated.setSpacingBefore(2);
        doc.add(generated);

        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingBefore(6);
        doc.add(spacer);

        doc.add(new Chunk(new LineSeparator(1f, 100f, COLOR_GRAY, Element.ALIGN_CENTER, -2f)));
        doc.add(new Paragraph(" "));
    }

    private void addTransactionTable(Document doc, List<Transaction> transactions) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font cellFont   = new Font(Font.HELVETICA, 10, Font.NORMAL);

        PdfPTable table = new PdfPTable(new float[]{2.6f, 1.3f, 1.3f, 3.0f, 1.8f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        for (String h : new String[]{"Date / Time", "Type", "Amount", "Note", "Balance After"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(COLOR_HEADER_BG);
            cell.setPadding(6);
            table.addCell(cell);
        }

        if (transactions.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No transactions in this period.", cellFont));
            empty.setColspan(5);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(12);
            table.addCell(empty);
        } else {
            for (Transaction tx : transactions) {
                PdfPCell dateCell = new PdfPCell(new Phrase(tx.getCreatedAt().format(TX_FMT), cellFont));
                dateCell.setPadding(5);
                table.addCell(dateCell);

                Font typeFont = new Font(Font.HELVETICA, 10, Font.BOLD, typeColor(tx.getType()));
                PdfPCell typeCell = new PdfPCell(new Phrase(typeName(tx.getType()), typeFont));
                typeCell.setPadding(5);
                table.addCell(typeCell);

                PdfPCell amtCell = new PdfPCell(new Phrase(formatMoney(tx.getAmount()), cellFont));
                amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                amtCell.setPadding(5);
                table.addCell(amtCell);

                PdfPCell noteCell = new PdfPCell(
                        new Phrase(tx.getNote() != null ? tx.getNote() : "", cellFont));
                noteCell.setPadding(5);
                table.addCell(noteCell);

                Font balFont = new Font(Font.HELVETICA, 10, Font.BOLD);
                PdfPCell balCell = new PdfPCell(new Phrase(formatMoney(tx.getBalanceAfter()), balFont));
                balCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                balCell.setPadding(5);
                table.addCell(balCell);
            }
        }

        doc.add(table);
    }

    private void addBalanceSummary(Document doc, SavingsAccount account) throws DocumentException {
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Paragraph balance = new Paragraph("Current Balance: " + formatMoney(account.getBalance()), boldFont);
        balance.setAlignment(Element.ALIGN_RIGHT);
        balance.setSpacingBefore(12);
        doc.add(balance);
    }

    private String typeName(Transaction.Type type) {
        return switch (type) {
            case DEPOSIT    -> "Deposit";
            case WITHDRAWAL -> "Withdrawal";
            case INTEREST   -> "Interest";
        };
    }

    private Color typeColor(Transaction.Type type) {
        return switch (type) {
            case DEPOSIT    -> COLOR_DEPOSIT;
            case WITHDRAWAL -> COLOR_WITHDRAWAL;
            case INTEREST   -> COLOR_INTEREST;
        };
    }

    private String formatMoney(java.math.BigDecimal amount) {
        return String.format("$%.2f", amount);
    }
}
