package com.example.demo.service.mail;

import com.example.demo.dto.sales.SalesAggregates;
import com.example.demo.event.ReportRequestedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    private static final DateTimeFormatter SUBJECT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void sendSummaryEmail(ReportRequestedEvent event, SalesAggregates aggregates, String summaryText) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setTo(event.getEmailTo());
            helper.setSubject(buildSubject(event));

            if (event.isPremium()) {
                String htmlBody = buildPremiumHtml(event, aggregates, summaryText);
                helper.setText(htmlBody, true);
            } else {
                String plainBody = buildPlainBody(event, aggregates, summaryText);
                helper.setText(plainBody, false);
            }

            mailSender.send(message);
            log.info("✅ Email enviado exitosamente para request: {}", event.getRequestId());

        } catch (MessagingException e) {
            log.error("❌ Error preparando correo de resumen {}", event.getRequestId(), e);
            throw new RuntimeException("Error al preparar el email", e);
        } catch (Exception e) {
            log.error("❌ No se pudo enviar el correo de resumen {}", event.getRequestId(), e);
            throw new RuntimeException("Error al enviar el email", e);
        }
    }

    public void sendFailureNotification(ReportRequestedEvent event, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setTo(event.getEmailTo());
            helper.setSubject("❌ Error en Reporte Semanal Oreo");
            helper.setText(buildFailureBody(event, reason));

            mailSender.send(message);
            log.info("📧 Notificación de error enviada para request: {}", event.getRequestId());

        } catch (MessagingException ex) {
            log.error("❌ Error notificando fallo del reporte {}", event.getRequestId(), ex);
        } catch (Exception ex) {
            log.error("❌ Fallo al enviar correo de error {}", event.getRequestId(), ex);
        }
    }

    private String buildSubject(ReportRequestedEvent event) {
        String base = "🍪 Reporte Semanal Oreo - " +
                SUBJECT_FORMATTER.format(event.getFrom()) + " a " +
                SUBJECT_FORMATTER.format(event.getTo());

        if (event.isPremium()) {
            base += " 📊 [PREMIUM]";
        }

        return base;
    }

    private String buildPlainBody(ReportRequestedEvent event, SalesAggregates aggregates, String summaryText) {
        StringBuilder body = new StringBuilder();
        body.append(summaryText).append("\n\n");
        body.append("=").append("=".repeat(50)).append("=\n");
        body.append("DETALLES DEL REPORTE\n");
        body.append("=").append("=".repeat(50)).append("=\n");
        body.append("• Periodo: ").append(event.getFrom()).append(" a ").append(event.getTo()).append("\n");
        body.append("• Sucursal: ").append(event.getBranch()).append("\n");
        body.append("• Total unidades: ").append(aggregates.getTotalUnits()).append("\n");
        body.append("• Total ingresos: S/ ").append(String.format("%.2f", aggregates.getTotalRevenue())).append("\n");

        if (aggregates.getTopSku() != null) {
            body.append("• SKU más vendido: ").append(aggregates.getTopSku()).append("\n");
        }
        if (aggregates.getTopBranch() != null && !aggregates.getTopBranch().equals(event.getBranch())) {
            body.append("• Sucursal top: ").append(aggregates.getTopBranch()).append("\n");
        }

        body.append("\n--\nSistema de Reportes Oreo Insight Factory");
        return body.toString();
    }

    private String buildFailureBody(ReportRequestedEvent event, String reason) {
        return "No fue posible generar el resumen solicitado.\n\n" +
                "Detalles:\n" +
                "• ID de solicitud: " + event.getRequestId() + "\n" +
                "• Periodo: " + event.getFrom() + " a " + event.getTo() + "\n" +
                "• Sucursal: " + event.getBranch() + "\n" +
                "• Motivo del error: " + reason + "\n\n" +
                "Por favor, contacte al administrador del sistema.\n\n" +
                "--\nSistema de Reportes Oreo Insight Factory";
    }

    private String buildPremiumHtml(ReportRequestedEvent event, SalesAggregates aggregates, String summaryText) {
        String chartUrl = buildChartUrl(aggregates);

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Reporte Oreo</title>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; background: #f8fafc; color: #1e293b; }" +
                ".container { max-width: 800px; margin: 0 auto; background: white; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); overflow: hidden; }" +
                ".header { background: linear-gradient(135deg, #6B46C1, #805AD5); color: white; padding: 30px; text-align: center; }" +
                ".header h1 { margin: 0; font-size: 28px; }" +
                ".header p { margin: 8px 0 0; opacity: 0.9; }" +
                ".content { padding: 30px; }" +
                ".summary { background: #f1f5f9; padding: 20px; border-radius: 8px; margin-bottom: 25px; line-height: 1.6; }" +
                ".metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin: 25px 0; }" +
                ".metric { background: #f8fafc; padding: 20px; border-radius: 8px; border-left: 4px solid #6B46C1; text-align: center; }" +
                ".metric h3 { margin: 0 0 8px; font-size: 14px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }" +
                ".metric p { margin: 0; font-size: 24px; font-weight: bold; color: #1e293b; }" +
                ".chart-container { margin: 30px 0; text-align: center; }" +
                ".footer { background: #f1f5f9; padding: 20px; text-align: center; color: #64748b; font-size: 14px; }" +
                ".premium-badge { background: #fbbf24; color: #78350f; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; display: inline-block; margin-left: 10px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🍪 Reporte Semanal Oreo " + (event.isPremium() ? "<span class='premium-badge'>PREMIUM</span>" : "") + "</h1>" +
                "<p>" + event.getFrom() + " a " + event.getTo() + " | " + event.getBranch() + "</p>" +
                "</div>" +
                "<div class='content'>" +
                "<div class='summary'>" +
                "<p>" + summaryText.replace("\n", "<br>") + "</p>" +
                "</div>" +
                "<div class='metrics'>" +
                "<div class='metric'><h3>Total Unidades</h3><p>" + aggregates.getTotalUnits() + "</p></div>" +
                "<div class='metric'><h3>Total Ingresos</h3><p>S/ " + String.format("%.2f", aggregates.getTotalRevenue()) + "</p></div>" +
                (aggregates.getTopSku() != null ? "<div class='metric'><h3>SKU Top</h3><p>" + aggregates.getTopSku() + "</p></div>" : "") +
                (aggregates.getTopBranch() != null && !aggregates.getTopBranch().equals(event.getBranch()) ?
                        "<div class='metric'><h3>Sucursal Top</h3><p>" + aggregates.getTopBranch() + "</p></div>" : "") +
                "</div>" +
                (event.isIncludeCharts() ?
                        "<div class='chart-container'>" +
                                "<img src='" + chartUrl + "' alt='Gráfico de Resumen' style='max-width: 100%; height: auto; border-radius: 8px;'/>" +
                                "<p style='color: #64748b; font-size: 12px; margin-top: 8px;'>Gráfico generado automáticamente</p>" +
                                "</div>" : "") +
                "</div>" +
                "<div class='footer'>" +
                "<p>🚀 Generado automáticamente por Oreo Insight Factory</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildChartUrl(SalesAggregates aggregates) {
        return "https://quickchart.io/chart?c=" +
                "{" +
                "  type: 'bar'," +
                "  data: {" +
                "    labels: ['Unidades Vendidas', 'Ingresos (S/)']," +
                "    datasets: [{" +
                "      label: 'Métricas Principales'," +
                "      data: [" + aggregates.getTotalUnits() + ", " + String.format("%.2f", aggregates.getTotalRevenue()) + "]," +
                "      backgroundColor: ['#6B46C1', '#805AD5']" +
                "    }]" +
                "  }," +
                "  options: {" +
                "    plugins: {" +
                "      legend: { display: false }," +
                "      title: { display: true, text: 'Resumen de Ventas' }" +
                "    }," +
                "    scales: {" +
                "      y: { beginAtZero: true }" +
                "    }" +
                "  }" +
                "}&width=600&height=300";
    }
}