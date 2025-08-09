package com.buckpal.dto.csv;

import java.util.List;

public class CsvUploadResponse {
    private String sessionId;
    private List<String> headers;
    private List<List<String>> previewData;
    private int totalRows;
    
    public CsvUploadResponse() {}
    
    public CsvUploadResponse(String sessionId, List<String> headers, List<List<String>> previewData, int totalRows) {
        this.sessionId = sessionId;
        this.headers = headers;
        this.previewData = previewData;
        this.totalRows = totalRows;
    }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }
    
    public List<List<String>> getPreviewData() { return previewData; }
    public void setPreviewData(List<List<String>> previewData) { this.previewData = previewData; }
    
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
}