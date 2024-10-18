package com.example.logProcessingService.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogProcessingService {

    @Value("${cloud.file.path}")
    private String cloudFilePath;

    @Value("${mainframe.file.path}")
    private String mainframeFilePath;

    @Value("${cloud.filter.pattern}")
    private String cloudFilterPattern;

    @Value("${cloud.split.pattern}")
    private String cloudSplitPattern;

    @Value("${mainframe.filter.column}")
    private String mainframeFilterColumn;

    @Value("${excel.output.path}")
    private String excelOutputPath;

    public List<String> findUnmatchedItems(List<String> mainframeList, List<String> cloudList) {
        return mainframeList.stream()
                .filter(item -> !cloudList.contains(item))
                .collect(Collectors.toList());
    }

    public List<String> findMatchedItems(List<String> mainframeList, List<String> cloudList) {
        return mainframeList.stream()
                .filter(cloudList::contains)
                .collect(Collectors.toList());
    }

    public List<String> findUnprocessedItems(List<String> cloudList, List<String> unmatched, List<String> matched) {
        var processedList = new ArrayList<String>();
        processedList.addAll(matched);
        processedList.addAll(unmatched);

        return cloudList.stream()
                .filter(item -> !processedList.contains(item))
                .collect(Collectors.toList());
    }

    public List<String> getCloud() {
        try (var lines = Files.lines(Path.of(cloudFilePath))) {
            return lines
                    .filter(line -> line.contains(cloudFilterPattern) && line.contains("outgoing"))
                    .map(line -> line.split(cloudSplitPattern))
                    .filter(arr -> arr.length > 1)
                    .map(arr -> "571=" + arr[1].split(cloudFilterPattern)[0])
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error reading cloud log file", e);
        }
    }

    public List<String> getMainFrame() {
        try (var lines = Files.lines(Path.of(mainframeFilePath))) {
            return lines
                    .filter(line -> line.contains("|"))
                    .map(line -> line.split("\\|"))
                    .filter(arr -> arr.length >= 12 && arr[11].startsWith(mainframeFilterColumn))
                    .map(arr -> arr[11])
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error reading mainframe log file", e);
        }
    }

    // Method to generate the Excel file
    public void generateExcelFile(List<String> unmatched, List<String> matched, List<String> unProcessedList) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Log Comparison");

        // Creating header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Unmatched Items");
        headerRow.createCell(1).setCellValue("Matched Items");
        headerRow.createCell(2).setCellValue("Unprocessed Items");

        // Find the max size of the lists
        int maxRows = Math.max(Math.max(unmatched.size(), matched.size()), unProcessedList.size());

        // Populate the rows
        for (int i = 0; i < maxRows; i++) {
            Row row = sheet.createRow(i + 1);
            if (i < unmatched.size()) {
                row.createCell(0).setCellValue(unmatched.get(i));
            }
            if (i < matched.size()) {
                row.createCell(1).setCellValue(matched.get(i));
            }
            if (i < unProcessedList.size()) {
                row.createCell(2).setCellValue(unProcessedList.get(i));
            }
        }

        // Write the output to the file
        try (FileOutputStream fileOut = new FileOutputStream(excelOutputPath)) {
            workbook.write(fileOut);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException("Error writing to Excel file", e);
        }
    }
}
