package com.logistech.fms.albaran;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Albarán de entrega en PDF: cabecera de ruta + tabla de paradas + QR de verificación. */
public class DeliveryNotePdf {

  public record StopLine(int seq, String customerName, String address, String eta, boolean delivered) {
  }

  public record RouteDoc(String companyName, String date, String driverName, String totalKm,
      List<StopLine> stops, String verifyUrl) {
  }

  public static byte[] generate(RouteDoc doc) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document pdf = new Document(PageSize.A4, 36, 36, 36, 36);
      PdfWriter.getInstance(pdf, out);
      pdf.open();

      Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
      Font sub = FontFactory.getFont(FontFactory.HELVETICA, 11);
      Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
      Font cell = FontFactory.getFont(FontFactory.HELVETICA, 10);

      pdf.add(new Paragraph("LogisTech B2B - Albarán de ruta", title));
      pdf.add(new Paragraph("Empresa: " + doc.companyName(), sub));
      pdf.add(new Paragraph("Fecha: " + doc.date() + "   Conductor: " + doc.driverName()
          + "   Total: " + doc.totalKm() + " km", sub));
      pdf.add(new Paragraph(" "));

      PdfPTable table = new PdfPTable(new float[] { 1, 4, 6, 2, 2 });
      table.setWidthPercentage(100);
      for (String header : new String[] { "Nº", "Cliente", "Dirección", "ETA", "Estado" }) {
        PdfPCell h = new PdfPCell(new Phrase(header, bold));
        h.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(h);
      }
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
      for (StopLine stop : doc.stops()) {
        table.addCell(new Phrase(String.valueOf(stop.seq()), cell));
        table.addCell(new Phrase(stop.customerName(), cell));
        table.addCell(new Phrase(stop.address(), cell));
        table.addCell(new Phrase(stop.eta(), cell));
        PdfPCell state = new PdfPCell(new Phrase(stop.delivered() ? "Entregada" : "Pendiente", cell));
        state.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(state);
      }
      pdf.add(table);
      pdf.add(new Paragraph(" "));

      if (doc.verifyUrl() != null && !doc.verifyUrl().isBlank()) {
        byte[] qr = QrCode.png(doc.verifyUrl(), 160);
        Image qrImage = Image.getInstance(qr);
        qrImage.setAlignment(Element.ALIGN_LEFT);
        pdf.add(qrImage);
        pdf.add(new Paragraph("Verificar: " + doc.verifyUrl(), sub));
      }
      pdf.add(new Paragraph("Generado: "
          + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), sub));

      pdf.close();
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar el albarán PDF", e);
    }
  }
}
