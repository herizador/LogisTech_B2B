package com.logistech.fms.albaran;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/** QR en PNG usando solo ZXing core (sin artefacto javase). */
public class QrCode {

  public static byte[] png(String text, int size) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
      BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
      for (int x = 0; x < size; x++) {
        for (int y = 0; y < size; y++) {
          image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
        }
      }
      ImageIO.write(image, "PNG", out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo generar el QR", e);
    }
  }
}
