package com.seventhray.contract_backend.service.parser;

import com.seventhray.contract_backend.exception.FileStorageException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Service to parse uploaded binary contract documents (PDF and DOCX)
 * and extract readable plain text.
 */
@Service
public class DocumentParserService {

    /**
     * Main entry point: Inspects file extension and routes to appropriate parser.
     * 
     * @param file The uploaded MultipartFile from the controller
     * @return Cleaned, plain text representation of the contract
     */
    public String extractText(MultipartFile file) {
        // Guard clause: empty file yields empty string
        if (file.isEmpty()) {
            return "";
        }

        // 1. Determine file extension from original filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        try {
            // 2. Route to the specialized extractor based on format
            return switch (extension) {
                case ".pdf" -> parsePdf(file.getBytes());
                case ".docx" -> parseDocx(file.getInputStream());
                case ".txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
                default -> {
                    // Fallback attempt: Try PDF first, then Word
                    try {
                        yield parsePdf(file.getBytes());
                    } catch (Exception ex) {
                        try (InputStream is = file.getInputStream()) {
                            yield parseDocx(is);
                        } catch (Exception e) {
                            yield ""; // If unrecognized binary, return empty string safely
                        }
                    }
                }
            };
        } catch (Exception e) {
            // Wrap in our custom FileStorageException so GlobalExceptionHandler catches it!
            throw new FileStorageException("Failed to parse document content: " + e.getMessage(), e);
        }
    }

    /**
     * Parses PDF documents using Apache PDFBox 3.
     * Uses try-with-resources to guarantee PDDocument is closed and memory is freed.
     */
    private String parsePdf(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Critical for legal documents: reads left-to-right, top-to-bottom
            stripper.setSortByPosition(true);
            
            String rawText = stripper.getText(document);
            return cleanText(rawText);
        }
    }

    /**
     * Parses MS Word (.docx) documents using Apache POI 5.
     */
    private String parseDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            String rawText = extractor.getText();
            return cleanText(rawText);
        }
    }

    /**
     * Cleans up messy formatting, tabs, and carriage returns while
     * strictly preserving readable paragraph breaks (needed for Q&A chunking).
     */
    private String cleanText(String input) {
        if (input == null) return "";
        
        return input.replaceAll("\\r\\n", "\n")      // Normalize Windows CRLF to standard \n
                    .replaceAll("\\r", "\n")        // Normalize Mac CR
                    .replaceAll("[ \\t]+", " ")     // Collapse multiple horizontal spaces/tabs
                    .replaceAll("\\n{3,}", "\n\n")  // Limit consecutive empty lines to maximum 2
                    .trim();
    }
}