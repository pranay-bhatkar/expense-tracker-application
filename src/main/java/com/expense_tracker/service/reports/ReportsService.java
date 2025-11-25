package com.expense_tracker.service.reports;

import com.expense_tracker.model.Transaction;
import com.expense_tracker.model.TransactionType;
import com.expense_tracker.model.User;
import com.expense_tracker.repository.TransactionRepository;
import com.expense_tracker.service.UserService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final SpringTemplateEngine templateEngine; // autowired



    /*
     *
     */

    private static final com.lowagie.text.Font TITLE_FONT =
            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);

    private static final com.lowagie.text.Font SUBTITLE_FONT =
            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 13, com.lowagie.text.Font.BOLD);

    private static final com.lowagie.text.Font NORMAL_FONT =
            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11);

    private static final com.lowagie.text.Font HEADER_FONT =
            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);

    private static final java.awt.Color HEADER_BG = new java.awt.Color(230, 230, 250);  // lavender
    private static final java.awt.Color SUMMARY_BG = new java.awt.Color(240, 248, 255); // alice blue


    //CSV
    public byte[] exportCSV(int month, int year) throws Exception {
        User user = userService.getCurrentUser();

        List<Transaction> transactions = transactionRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(out), CSVFormat.DEFAULT.withHeader("Date",
                "Amount", "Type", "Category", "Notes"));

        for (Transaction t : transactions) {
            csvPrinter.printRecord(
                    t.getDate(),
                    t.getAmount(),
                    t.getType(),
                    t.getCategory() != null ? t.getCategory().getName() : "General",
                    t.getNotes() != null ? t.getNotes() : ""
            );
        }

        csvPrinter.flush();
        return out.toByteArray();
    }

    // Excel export

    public byte[] exportExcel(int month, int year) throws Exception {
        System.out.println("Entered in the exportExcel service");
        User user = userService.getCurrentUser();
        List<Transaction> transactions = transactionRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Transactions");

        Row header = sheet.createRow(0);
        String[] columns = {"Date", "Amount", "Type", "Category", "Notes"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (Transaction t : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(t.getDate().toString());
            row.createCell(1).setCellValue(t.getAmount());
            row.createCell(2).setCellValue(t.getType().name());
            row.createCell(3).setCellValue(t.getCategory() != null ? t.getCategory().getName() : "General");
            row.createCell(4).setCellValue(t.getNotes() != null ? t.getNotes() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // PDF Export -> iText 7
//    public byte[] exportPDF(int month, int year) throws Exception {
//        System.out.println("Entering the exportPDF service");
//
//        User user = userService.getCurrentUser();
//
//        List<Transaction> transactions = transactionRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);
//
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        PdfWriter writer = new PdfWriter(out);
//        PdfDocument pdfDoc = new PdfDocument(writer);
//        Document document = new Document(pdfDoc);
//
//        document.add(new Paragraph("Monthly Transactions Report"));
//        document.add(new Paragraph("Month: " + month + " Year: " + year));
//        document.add(new Paragraph(" "));
//
//        Table table = new Table(
//                new float[]{3, 2, 2, 3, 4}
//        ).useAllAvailableWidth();
//
//        table.addHeaderCell("Date");
//        table.addHeaderCell("Amount");
//        table.addHeaderCell("Type");
//        table.addHeaderCell("Category");
//        table.addHeaderCell("Notes");
//
//        for (Transaction t : transactions) {
//            table.addCell(t.getDate().toString());
//            table.addCell(String.valueOf(t.getAmount()));
//            table.addCell(t.getType().name());
//            table.addCell(t.getCategory() != null ? t.getCategory().getName() : "General");
//            table.addCell(t.getNotes() != null ? t.getNotes() : "");
//        }
//
//        document.add(table);
//        document.close();
//        return out.toByteArray();
//
//    }
//

    // using open pdf
//    public byte[] exportPDF(int month, int year) throws Exception {
//        System.out.println("Entering the exportPDF service");
//
//        User user = userService.getCurrentUser();
//        List<Transaction> transactions = transactionRepository.findByUserIdAndMonthAndYear(
//                user.getId(), month, year
//        );
//
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//        Document document = new Document();
//        PdfWriter writer = PdfWriter.getInstance(document, out);
//
//        document.open();
//
//        // ----------- TITLE -----------
//        Paragraph title = new Paragraph(
//                "Monthly Transactions Report",
//                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD)
//        );
//        title.setAlignment(Paragraph.ALIGN_CENTER);
//        document.add(title);
//
//        document.add(new Paragraph("Month: " + month + "   Year: " + year));
//        document.add(new Paragraph(" ")); // empty line
//
//        // ----------- TABLE -----------
//        PdfPTable table = new PdfPTable(5);
//        table.setWidthPercentage(100);
//        table.setSpacingBefore(10);
//
//        table.setWidths(new float[]{3f, 2f, 2f, 3f, 4f});
//
//        // Add header with bold style
//        com.lowagie.text.Font headerFont =
//                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
//
//        table.addCell(new com.lowagie.text.Phrase("Date", headerFont));
//        table.addCell(new com.lowagie.text.Phrase("Amount", headerFont));
//        table.addCell(new com.lowagie.text.Phrase("Type", headerFont));
//        table.addCell(new com.lowagie.text.Phrase("Category", headerFont));
//        table.addCell(new com.lowagie.text.Phrase("Notes", headerFont));
//
//        // Row font
//        com.lowagie.text.Font rowFont =
//                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11);
//
//        if (transactions.isEmpty()) {
//            PdfPCell emptyCell = new PdfPCell(new com.lowagie.text.Phrase("No transactions available", rowFont));
//            emptyCell.setColspan(5);
//            emptyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
//            emptyCell.setPadding(8f);
//            table.addCell(emptyCell);
//        } else {
//            for (Transaction t : transactions) {
//                table.addCell(new com.lowagie.text.Phrase(t.getDate() != null ? t.getDate().toString() : "", rowFont));
//                table.addCell(new com.lowagie.text.Phrase(String.valueOf(t.getAmount()), rowFont));
//                table.addCell(new com.lowagie.text.Phrase(
//                        t.getType() != null ? t.getType().name() : "", rowFont));
//                table.addCell(new com.lowagie.text.Phrase(
//                        t.getCategory() != null ? t.getCategory().getName() : "General", rowFont));
//                table.addCell(new com.lowagie.text.Phrase(
//                        t.getNotes() != null ? t.getNotes() : "", rowFont));
//            }
//        }
//
//        document.add(table);
//        document.close();
//        writer.close();
//
//        System.out.println("PDF size = " + out.size());
//        return out.toByteArray();
//    }


    public byte[] exportPDF(int month, int year) throws Exception {

        User user = userService.getCurrentUser();
        List<Transaction> transactions =
                transactionRepository.findByUserIdAndMonthAndYear(user.getId(), month, year);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, out);

        document.open();

        // ------------------------------------------------------------
        // 1. HEADER SECTION
        // ------------------------------------------------------------
        Paragraph title = new Paragraph("Monthly Statement", TITLE_FONT);
        title.setAlignment(Paragraph.ALIGN_CENTER);

        document.add(title);
        document.add(new Paragraph(user.getName() + " — Expense Tracker"));
        document.add(new Paragraph("Month: " + month + "   Year: " + year));
        document.add(new Paragraph(" ")); // spacing

        // ------------------------------------------------------------
        // 2. SUMMARY SECTION
        // ------------------------------------------------------------
        double income = transactions.stream()
                .filter(t -> t.getType().name().equals("INCOME"))
                .mapToDouble(Transaction::getAmount).sum();

        double expense = transactions.stream()
                .filter(t -> t.getType().name().equals("EXPENSE"))
                .mapToDouble(Transaction::getAmount).sum();

        double savings = income - expense;

        // CATEGORY SUMMARY
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getCategory() != null) {
                categoryTotals.merge(t.getCategory().getName(), t.getAmount(), Double::sum);
            }
        }

        String highestCategory = categoryTotals.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("None");

        // SUMMARY TABLE
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(10);

        PdfPCell header1 = new PdfPCell(new Phrase("Summary", SUBTITLE_FONT));
        header1.setColspan(2);
        header1.setBackgroundColor(SUMMARY_BG);
        header1.setPadding(10);
        header1.setHorizontalAlignment(Element.ALIGN_CENTER);
        summaryTable.addCell(header1);

        summaryTable.addCell("Total Income");
        summaryTable.addCell(String.valueOf(income));

        summaryTable.addCell("Total Expense");
        summaryTable.addCell(String.valueOf(expense));

        summaryTable.addCell("Savings");
        summaryTable.addCell(String.valueOf(savings));

        summaryTable.addCell("Most Spent Category");
        summaryTable.addCell(highestCategory);

        summaryTable.addCell("Total Transactions");
        summaryTable.addCell(String.valueOf(transactions.size()));

        document.add(summaryTable);

        document.add(new Paragraph("\n")); // spacing

        // ------------------------------------------------------------
        // 3. CATEGORY SUMMARY TABLE
        // ------------------------------------------------------------
        PdfPTable catTable = new PdfPTable(2);
        catTable.setWidthPercentage(100);

        PdfPCell catHeader = new PdfPCell(new Phrase("Category Summary", SUBTITLE_FONT));
        catHeader.setColspan(2);
        catHeader.setBackgroundColor(HEADER_BG);
        catHeader.setPadding(10);
        catHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        catTable.addCell(catHeader);

        catTable.addCell(new PdfPCell(new Phrase("Category", HEADER_FONT)));
        catTable.addCell(new PdfPCell(new Phrase("Total Amount", HEADER_FONT)));

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            catTable.addCell(new Phrase(entry.getKey()));
            catTable.addCell(new Phrase(String.valueOf(entry.getValue())));
        }

        document.add(catTable);

        document.add(new Paragraph("\n")); // spacing

        // ------------------------------------------------------------
        // 4. DETAILED TRANSACTIONS TABLE
        // ------------------------------------------------------------
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        table.setWidths(new float[]{1.5f, 3f, 2f, 2f, 3f, 4f});

        // HEADER ROW
        Stream.of("Sr.no.", "Date", "Amount", "Type", "Category", "Notes")
                .forEach(col -> {
                    PdfPCell cell = new PdfPCell(new Phrase(col, HEADER_FONT));
                    cell.setBackgroundColor(HEADER_BG);
                    cell.setPadding(5);
                    table.addCell(cell);
                });

        // DATA ROWS
        int srNo = 1;
        for (Transaction t : transactions) {
            table.addCell(String.valueOf(srNo++));
            table.addCell(t.getDate() != null ? t.getDate().toString() : "");
            table.addCell(String.valueOf(t.getAmount()));
            table.addCell(t.getType() != null ? t.getType().name() : "");
            table.addCell(t.getCategory() != null ? t.getCategory().getName() : "General");
            table.addCell(t.getNotes() != null ? t.getNotes() : "");
        }

        document.add(table);

        // ------------------------------------------------------------
        // 5. FOOTER
        // ------------------------------------------------------------
        document.add(new Paragraph("\nGenerated on: " + java.time.LocalDate.now()));
        Paragraph footer = new Paragraph("Thank you for using Expense Tracker", NORMAL_FONT);
        footer.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(footer);

        document.close();
        writer.close();

        return out.toByteArray();
    }


    public byte[] exportMonthlyHtmlPdf(int month, int year, @Nullable byte[] logoBytes) throws Exception {
        User user = userService.getCurrentUser();
        Long userId = user.getId();

        List<Transaction> transactions = transactionRepository.findByUserIdAndMonthAndYear(userId, month, year);

        // compute totals
        double income = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount).sum();
        double expense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount).sum();
        double savings = income - expense;

        // category summary
        Map<String, Double> catMap = new LinkedHashMap<>();
        transactions.stream()
                .filter(t -> t.getCategory() != null)
                .forEach(t -> catMap.merge(t.getCategory().getName(), t.getAmount(), Double::sum));

        // prepare Thymeleaf context
        Context ctx = new Context();
        ctx.setVariable("user", user);
        ctx.setVariable("month", month);
        ctx.setVariable("year", year);
        ctx.setVariable("income", income);
        ctx.setVariable("expense", expense);
        ctx.setVariable("savings", savings);

        // categories: list of objects with name & total (Thymeleaf expects iteration)
        List<Map<String, Object>> categories = catMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("total", e.getValue());
                    return map;
                })
                .toList();

        ctx.setVariable("categories", categories);

        ctx.setVariable("transactions", transactions);
        ctx.setVariable("generatedDate", java.time.LocalDate.now().toString());

        if (logoBytes != null) {
            String logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
            ctx.setVariable("logoBase64", logoBase64);
        }

        // render HTML via Thymeleaf
        String html = templateEngine.process("monthly_statement", ctx);

        // convert HTML to PDF using OpenHTMLToPDF
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            // optional: register fonts if needed
            // builder.useFont(() -> new File("..."), "YourFontName");

            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }


}