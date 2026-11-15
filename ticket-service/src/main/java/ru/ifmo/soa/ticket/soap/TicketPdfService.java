package ru.ifmo.soa.ticket.soap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ticket.model.Ticket;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TicketPdfService {

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 800;
    private static final Color CREAM = new Color(255, 247, 225);
    private static final Color NAVY = new Color(25, 35, 58);
    private static final Color RED = new Color(174, 38, 44);
    private static final Color GOLD = new Color(225, 168, 63);

    public GeneratedTicketPdf generate(Ticket ticket) {
        BufferedImage ticketImage = drawTicket(ticket);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDRectangle pageSize = new PDRectangle(800, 400);
            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            PDImageXObject image = LosslessFactory.createFromImage(
                    document,
                    ticketImage
            );
            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {
                content.drawImage(image, 0, 0, pageSize.getWidth(), pageSize.getHeight());
            }

            document.getDocumentInformation().setTitle(
                    "ЦИРКуль - билет № " + ticket.getId()
            );
            document.getDocumentInformation().setSubject("Электронный входной билет");
            document.save(output);

            return new GeneratedTicketPdf(
                    "circul-ticket-" + ticket.getId() + ".pdf",
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать PDF-билет.", exception);
        }
    }

    private BufferedImage drawTicket(Ticket ticket) {
        BufferedImage image = new BufferedImage(
                WIDTH,
                HEIGHT,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );

            graphics.setPaint(new GradientPaint(0, 0, CREAM, WIDTH, HEIGHT, new Color(244, 223, 178)));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            graphics.setColor(RED);
            graphics.fill(new RoundRectangle2D.Double(35, 35, WIDTH - 70, HEIGHT - 70, 46, 46));
            graphics.setColor(CREAM);
            graphics.fill(new RoundRectangle2D.Double(55, 55, WIDTH - 110, HEIGHT - 110, 34, 34));

            drawHeader(graphics, ticket);
            drawMainDetails(graphics, ticket);
            drawStub(graphics, ticket);
            drawDecorations(graphics, ticket.getId());
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void drawHeader(Graphics2D graphics, Ticket ticket) {
        graphics.setColor(NAVY);
        graphics.fillRoundRect(85, 82, 1110, 142, 24, 24);

        graphics.setColor(GOLD);
        graphics.setFont(font(Font.BOLD, 70));
        graphics.drawString("ЦИРКуль", 125, 174);

        graphics.setColor(CREAM);
        graphics.setFont(font(Font.BOLD, 29));
        graphics.drawString("ЭЛЕКТРОННЫЙ ВХОДНОЙ БИЛЕТ", 490, 147);
        graphics.setFont(font(Font.PLAIN, 21));
        graphics.drawString("Пусть представление начнётся!", 490, 184);

        graphics.setColor(RED);
        graphics.setFont(font(Font.BOLD, 31));
        graphics.drawString("№ " + ticket.getId(), 1015, 174);
    }

    private void drawMainDetails(Graphics2D graphics, Ticket ticket) {
        int left = 115;
        int valueLeft = 360;
        int y = 295;

        String eventName = ticket.getEvent() == null
                ? "Основное представление"
                : ticket.getEvent().getName();

        drawLabelValue(graphics, "Представление", eventName, left, valueLeft, y, 720);
        y += 76;
        drawLabelValue(graphics, "Название билета", ticket.getName(), left, valueLeft, y, 720);
        y += 76;
        drawLabelValue(graphics, "Категория", ticket.getType().toString(), left, valueLeft, y, 350);
        drawLabelValue(graphics, "Цена", formatPrice(ticket.getPrice()), 720, 875, y, 300);
        y += 76;
        drawLabelValue(
                graphics,
                "Место",
                "координаты " + ticket.getCoordinates().getX() + "; " + ticket.getCoordinates().getY(),
                left,
                valueLeft,
                y,
                720
        );
        y += 76;
        drawLabelValue(graphics, "Создан", ticket.getCreationDate(), left, valueLeft, y, 720);

        graphics.setColor(new Color(112, 91, 65));
        graphics.setFont(font(Font.ITALIC, 22));
        List<String> commentLines = wrap(
                graphics,
                ticket.getComment(),
                1010
        );
        int commentY = 675;
        for (int i = 0; i < Math.min(2, commentLines.size()); i++) {
            graphics.drawString(commentLines.get(i), left, commentY + i * 28);
        }
    }

    private void drawStub(Graphics2D graphics, Ticket ticket) {
        graphics.setColor(new Color(206, 178, 125));
        graphics.setStroke(new BasicStroke(
                3,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                0,
                new float[]{12, 12},
                0
        ));
        graphics.drawLine(1235, 85, 1235, 715);

        graphics.setColor(NAVY);
        graphics.setFont(font(Font.BOLD, 28));
        graphics.drawString("КОНТРОЛЬ", 1300, 140);

        graphics.setColor(RED);
        graphics.setFont(font(Font.BOLD, 74));
        graphics.drawString("№" + ticket.getId(), 1280, 248);

        graphics.setColor(NAVY);
        graphics.setFont(font(Font.BOLD, 24));
        graphics.drawString(ticket.getType().toString(), 1300, 305);
        graphics.setFont(font(Font.PLAIN, 20));
        graphics.drawString(formatPrice(ticket.getPrice()), 1300, 340);

        drawBarcode(graphics, ticket.getId(), 1280, 410, 235, 125);

        graphics.setColor(NAVY);
        graphics.setFont(font(Font.PLAIN, 15));
        graphics.drawString(barcodeValue(ticket.getId()), 1300, 560);

        graphics.setFont(font(Font.BOLD, 19));
        graphics.drawString("ПРЕДЪЯВИТЬ", 1300, 600);
        graphics.drawString("ПРИ ВХОДЕ", 1317, 630);
        graphics.setColor(RED);
        graphics.setFont(font(Font.BOLD, 21));
        graphics.drawString("ЦИРКУЛЬ", 1340, 690);
    }

    private void drawDecorations(Graphics2D graphics, long ticketId) {
        graphics.setStroke(new BasicStroke(5));
        graphics.setColor(GOLD);
        graphics.drawOval(1045, 270, 95, 95);
        graphics.setColor(RED);
        graphics.drawOval(1062, 287, 61, 61);

        graphics.setFont(font(Font.BOLD, 26));
        graphics.setColor(NAVY);
        graphics.drawString("★", 1080, 327);

        graphics.setFont(font(Font.PLAIN, 16));
        graphics.setColor(new Color(112, 91, 65));
        graphics.drawString("ID " + String.format("%08d", ticketId), 1010, 710);
    }

    private void drawLabelValue(
            Graphics2D graphics,
            String label,
            String value,
            int labelX,
            int valueX,
            int y,
            int maxWidth
    ) {
        graphics.setColor(RED);
        graphics.setFont(font(Font.BOLD, 22));
        graphics.drawString(label.toUpperCase(), labelX, y);

        graphics.setColor(NAVY);
        graphics.setFont(fitFont(graphics, value, Font.BOLD, 31, maxWidth));
        graphics.drawString(value, valueX, y);
    }

    private Font fitFont(
            Graphics2D graphics,
            String value,
            int style,
            int initialSize,
            int maxWidth
    ) {
        int size = initialSize;
        Font candidate = font(style, size);
        while (size > 18 && graphics.getFontMetrics(candidate).stringWidth(value) > maxWidth) {
            candidate = font(style, --size);
        }
        return candidate;
    }

    private void drawBarcode(
            Graphics2D graphics,
            long ticketId,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.drawImage(renderBarcode(ticketId, width, height), x, y, null);
    }

    private BufferedImage renderBarcode(long ticketId, int width, int height) {
        BitMatrix matrix = new Code128Writer().encode(
                barcodeValue(ticketId),
                BarcodeFormat.CODE_128,
                width,
                height,
                Map.of(EncodeHintType.MARGIN, 8)
        );

        BufferedImage barcode = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_BYTE_BINARY
        );
        for (int column = 0; column < matrix.getWidth(); column++) {
            for (int row = 0; row < matrix.getHeight(); row++) {
                barcode.setRGB(
                        column,
                        row,
                        matrix.get(column, row) ? Color.BLACK.getRGB() : Color.WHITE.getRGB()
                );
            }
        }
        return barcode;
    }

    private String barcodeValue(long ticketId) {
        return "CIRCUL-" + String.format("%08d", ticketId);
    }

    private List<String> wrap(Graphics2D graphics, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        FontMetrics metrics = graphics.getFontMetrics();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (metrics.stringWidth(candidate) <= width) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private Font font(int style, int size) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    private String formatPrice(double price) {
        return String.format("%.2f ₽", price);
    }

    public record GeneratedTicketPdf(String fileName, byte[] content) {
    }
}
