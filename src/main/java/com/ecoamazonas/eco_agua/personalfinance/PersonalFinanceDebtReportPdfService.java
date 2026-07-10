package com.ecoamazonas.eco_agua.personalfinance;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Service
public class PersonalFinanceDebtReportPdfService {

    private final SpringTemplateEngine templateEngine;
    private final PersonalFinanceDebtReportFormatter formatter;

    public PersonalFinanceDebtReportPdfService(
            SpringTemplateEngine templateEngine,
            PersonalFinanceDebtReportFormatter formatter
    ) {
        this.templateEngine = templateEngine;
        this.formatter = formatter;
    }

    public byte[] render(PersonalFinanceDebtReport report, PersonalFinanceDebtReportOptions options) {
        Context context = new Context(Locale.forLanguageTag("es-PE"));
        context.setVariable("report", report);
        context.setVariable("options", options);
        context.setVariable("fmt", formatter);
        context.setVariable("pdfMode", true);
        String html = templateEngine.process("personal_finance/debt_report_pdf", context);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate GastoClaro debt report PDF.", exception);
        }
    }
}
