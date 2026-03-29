package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.OrderItem;
import com.grootan.storeflow.service.OrderReportPdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class OrderReportPdfServiceImpl implements OrderReportPdfService {

    private static final float START_X = 50;
    private static final float START_Y = 750;
    private static final float LINE_HEIGHT = 20;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public byte[] generateOrderReport(Order order) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = START_Y;

                y = writeLine(contentStream, "StoreFlow Order Summary", START_X, y, true);
                y -= 10;

                y = writeLine(contentStream, "Order Reference: #" + order.getId(), START_X, y, false);
                y = writeLine(contentStream, "Customer Name: " + order.getCustomer().getFullName(), START_X, y, false);
                y = writeLine(contentStream, "Order Date: " + formatOrderDate(order), START_X, y, false);
                y = writeLine(contentStream, "Status: " + order.getStatus().name(), START_X, y, false);
                y -= 10;

                y = writeLine(contentStream, "Items:", START_X, y, true);

                for (OrderItem item : order.getOrderItems()) {
                    String line = String.format(
                            "- %s | Qty: %d | Unit Price: %s | Subtotal: %s",
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getSubtotal()
                    );
                    y = writeLine(contentStream, line, START_X, y, false);
                }

                y -= 10;
                writeLine(contentStream, "Order Total: " + order.getTotalAmount(), START_X, y, true);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate order PDF", e);
        }
    }

    private float writeLine(PDPageContentStream contentStream, String text, float x, float y, boolean bold)
            throws IOException {
        contentStream.beginText();
        contentStream.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, bold ? 14 : 12);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - LINE_HEIGHT;
    }

    private String formatOrderDate(Order order) {
        if (order.getCreatedAt() == null) {
            return "N/A";
        }
        return order.getCreatedAt().format(DATE_FORMATTER);
    }
}