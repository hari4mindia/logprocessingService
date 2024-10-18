package com.example.logProcessingService.controller;


import com.example.logProcessingService.service.LogProcessingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LogProcessingController {

    private final LogProcessingService logProcessingService;

    public LogProcessingController(LogProcessingService logProcessingService) {
        this.logProcessingService = logProcessingService;
    }

    @GetMapping("/process-logs")
    public String processLogs() {
        List<String> cloudList = logProcessingService.getCloud();
        List<String> mainframeList = logProcessingService.getMainFrame();

        var unmatched = logProcessingService.findUnmatchedItems(mainframeList, cloudList);
        var matched = logProcessingService.findMatchedItems(mainframeList, cloudList);
        var unProcessedList = logProcessingService.findUnprocessedItems(cloudList, unmatched, matched);

        logProcessingService.generateExcelFile(unmatched, matched, unProcessedList);

        return String.format("Total cloud items: %d, Total mainframe items: %d, Unmatched: %d, Matched: %d, Remaining: %d",
                cloudList.size(), mainframeList.size(), unmatched.size(), matched.size(), unProcessedList.size());
    }
}

