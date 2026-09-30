package com.apiatlas.exporter;

import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.SecurityFinding;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/** CSV, Excel and PDF renderings of the catalog. */
@Component
public class TabularExporter {
    static final String[] HEADERS = {"ID", "Name", "Module", "Service", "URL", "Method", "Version", "Protocol",
            "Authentication", "Status", "Third party", "Last seen", "Description"};

    static String[] row(ApiEndpoint e) {
        return new String[]{String.valueOf(e.getId()), e.getName(), e.getModule(), e.getService(), e.getUrl(), e.getMethod(),
                e.getVersion(), e.getProtocol().name(), e.getAuthType().name(), e.getStatus().name(),
                String.valueOf(e.isThirdParty()), String.valueOf(e.getLastSeen()), e.getDescription()};
    }

    public byte[] csv(List<ApiEndpoint> endpoints) throws IOException {
        StringWriter out = new StringWriter();
        try (CSVPrinter p = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {
            for (ApiEndpoint e : endpoints) {
                String[] r = row(e);
                for (int i = 0; i < r.length; i++) r[i] = safe(r[i]);
                p.printRecord((Object[]) r);
            }
        }
        return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] excel(List<ApiEndpoint> endpoints, List<SecurityFinding> findings) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font f = wb.createFont();
            f.setBold(true);
            bold.setFont(f);

            Sheet apis = wb.createSheet("APIs");
            header(apis, bold, HEADERS);
            int r = 1;
            for (ApiEndpoint e : endpoints) {
                Row row = apis.createRow(r++);
                String[] v = row(e);
                for (int c = 0; c < v.length; c++) row.createCell(c).setCellValue(safe(v[c]));
            }
            Sheet sec = wb.createSheet("Security findings");
            header(sec, bold, new String[]{"Endpoint", "Category", "OWASP", "Severity", "Title", "Description", "Detected"});
            r = 1;
            for (SecurityFinding s : findings) {
                Row row = sec.createRow(r++);
                String[] v = {s.getEndpointLabel(), s.getCategory(), s.getOwasp(), s.getSeverity().name(), s.getTitle(),
                        s.getDescription(), String.valueOf(s.getDetectedAt())};
                for (int c = 0; c < v.length; c++) row.createCell(c).setCellValue(safe(v[c]));
            }
            for (int c = 0; c < 6; c++) {
                apis.setColumnWidth(c, 6000);
                sec.setColumnWidth(c, 6000);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] pdf(List<ApiEndpoint> endpoints) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("API Atlas - API Catalog", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        doc.add(new Paragraph(endpoints.size() + " APIs", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph(" "));
        String[] cols = {"Method", "Service", "Path", "Auth", "Status", "Description"};
        PdfPTable table = new PdfPTable(new float[]{1, 3, 4, 1.5f, 1.5f, 5});
        table.setWidthPercentage(100);
        Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, hf));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
        Font bf = FontFactory.getFont(FontFactory.HELVETICA, 7);
        for (ApiEndpoint e : endpoints) {
            for (String v : new String[]{e.getMethod(), e.getService(), e.getPath(), e.getAuthType().name(),
                    e.getStatus().name(), e.getDescription() == null ? "" : e.getDescription()}) {
                table.addCell(new Phrase(v, bf));
            }
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    private static void header(Sheet sheet, CellStyle style, String[] headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(style);
        }
    }

    /** Neutralizes spreadsheet formula injection ({@code = + - @} prefixes). */
    static String safe(String v) {
        if (v == null) return "";
        return !v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 ? "'" + v : v;
    }
}
