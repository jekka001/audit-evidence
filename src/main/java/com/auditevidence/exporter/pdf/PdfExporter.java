package com.auditevidence.exporter.pdf;

import com.auditevidence.model.CheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PdfExporter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.systemDefault());

    private final ObjectMapper objectMapper;
    private final boolean addWatermark;

    public PdfExporter(boolean addWatermark) {
        this.addWatermark = addWatermark;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void export(CheckResult result, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());

        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputPath.toFile()));
            document.open();

            addHeader(document, result);
            addSummary(document, result);
            addFindings(document, result);
            addRawData(document, result);
            addFooter(document);

            if (addWatermark) {
                addWatermarkText(document);
            }
        } catch (DocumentException e) {
            throw new IOException("Failed to create PDF: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    private void addHeader(Document document, CheckResult result) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(33, 37, 41));
        Font subtitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL, new Color(108, 117, 125));

        Paragraph title = new Paragraph("SOC2 Audit Evidence", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph subtitle = new Paragraph(result.clauseId() + " - " + result.checkName(), subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(30);
        document.add(subtitle);
    }

    private void addSummary(Document document, CheckResult result) throws DocumentException {
        Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(33, 37, 41));
        Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(33, 37, 41));

        Color statusColor = switch (result.status()) {
            case PASS -> new Color(40, 167, 69);
            case FAIL -> new Color(220, 53, 69);
            case PARTIAL -> new Color(255, 193, 7);
        };
        Font statusFont = new Font(Font.HELVETICA, 14, Font.BOLD, statusColor);

        addLabelValue(document, "Standard:", result.standard(), labelFont, valueFont);
        addLabelValue(document, "Clause ID:", result.clauseId(), labelFont, valueFont);
        addLabelValue(document, "Check Name:", result.checkName(), labelFont, valueFont);
        addLabelValue(document, "Description:", result.description(), labelFont, valueFont);
        addLabelValue(document, "Timestamp:", DATE_FORMAT.format(result.timestamp()), labelFont, valueFont);
        addLabelValue(document, "Data Source:", result.dataSource(), labelFont, valueFont);

        Paragraph statusPara = new Paragraph();
        statusPara.add(new Chunk("Result: ", labelFont));
        statusPara.add(new Chunk(result.status().name(), statusFont));
        statusPara.setSpacingAfter(20);
        document.add(statusPara);
    }

    private void addLabelValue(Document document, String label, String value,
                               Font labelFont, Font valueFont) throws DocumentException {
        Paragraph para = new Paragraph();
        para.add(new Chunk(label + " ", labelFont));
        para.add(new Chunk(value, valueFont));
        para.setSpacingAfter(5);
        document.add(para);
    }

    private void addFindings(Document document, CheckResult result) throws DocumentException {
        if (result.findings().isEmpty()) {
            return;
        }

        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(33, 37, 41));
        Font findingFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(220, 53, 69));

        Paragraph findingsTitle = new Paragraph("Findings", sectionFont);
        findingsTitle.setSpacingBefore(20);
        findingsTitle.setSpacingAfter(10);
        document.add(findingsTitle);

        List list = new List(List.UNORDERED);
        for (String finding : result.findings()) {
            ListItem item = new ListItem(finding, findingFont);
            list.add(item);
        }
        document.add(list);
    }

    private void addRawData(Document document, CheckResult result) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(33, 37, 41));
        Font codeFont = new Font(Font.COURIER, 8, Font.NORMAL, new Color(33, 37, 41));

        Paragraph rawDataTitle = new Paragraph("Raw Data (API Response)", sectionFont);
        rawDataTitle.setSpacingBefore(30);
        rawDataTitle.setSpacingAfter(10);
        document.add(rawDataTitle);

        try {
            String jsonData = objectMapper.writeValueAsString(result.rawData());
            Paragraph rawData = new Paragraph(jsonData, codeFont);
            rawData.setSpacingAfter(10);
            document.add(rawData);
        } catch (Exception e) {
            Paragraph error = new Paragraph("Unable to serialize raw data: " + e.getMessage(), codeFont);
            document.add(error);
        }
    }

    private void addFooter(Document document) throws DocumentException {
        Font footerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(108, 117, 125));

        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(40);
        footer.add(new Chunk("Generated by GitHub SOC2 Audit Evidence Exporter", footerFont));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("This document is intended for audit purposes. " +
                "The data presented was collected directly from the GitHub API.", footerFont));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addWatermarkText(Document document) throws DocumentException {
        Font watermarkFont = new Font(Font.HELVETICA, 40, Font.BOLD, new Color(200, 200, 200, 128));

        Paragraph watermark = new Paragraph("FREE TIER", watermarkFont);
        watermark.setAlignment(Element.ALIGN_CENTER);
        watermark.setSpacingBefore(100);
        document.add(watermark);
    }
}
