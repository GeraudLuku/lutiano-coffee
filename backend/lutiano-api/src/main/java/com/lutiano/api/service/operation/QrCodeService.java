package com.lutiano.api.service.operation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

/** Renders the permanent operation QR. The payload is an opaque token only — no seller, customer or payment data. */
@Service
public class QrCodeService {
  public static final String PREFIX = "LUTIANO:OP:";

  /** Customer loyalty QR: the customer shows it at checkout to confirm the association. */
  public static final String CUSTOMER_PREFIX = "LUTIANO:CU:";

  public byte[] operationPng(String opaqueToken) { return png(PREFIX + opaqueToken); }

  public byte[] customerPng(String loyaltyToken) { return png(CUSTOMER_PREFIX + loyaltyToken); }

  private byte[] png(String payload) {
    try {
      BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 512, 512,
          Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q, EncodeHintType.MARGIN, 2));
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return out.toByteArray();
    } catch (WriterException | IOException e) {
      throw new IllegalStateException("Could not render QR code", e);
    }
  }
}
