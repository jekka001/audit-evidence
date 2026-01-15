package com.auditevidence.exporter.json;

import com.auditevidence.model.AuditReport;
import com.auditevidence.model.CheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonExporter {
    private final ObjectMapper objectMapper;

    public JsonExporter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void export(CheckResult result, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        objectMapper.writeValue(outputPath.toFile(), result);
    }

    public void exportReport(AuditReport report, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        objectMapper.writeValue(outputPath.toFile(), report);
    }
}
