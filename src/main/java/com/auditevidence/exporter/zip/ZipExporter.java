package com.auditevidence.exporter.zip;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipExporter {

    public void createZip(List<Path> files, Path outputZipPath) throws IOException {
        Files.createDirectories(outputZipPath.getParent());

        try (FileOutputStream fos = new FileOutputStream(outputZipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (Path file : files) {
                if (Files.exists(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getFileName().toString());
                    zos.putNextEntry(zipEntry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                }
            }
        }
    }
}
