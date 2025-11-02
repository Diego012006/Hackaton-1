package com.example.demo.service.mail;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class PdfGenerator {

    public byte[] generatePdf(String htmlContent) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(out);
            builder.run();
            log.info("✅ PDF generado correctamente ({} bytes)", out.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("❌ Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF", e);
        }
    }
}
