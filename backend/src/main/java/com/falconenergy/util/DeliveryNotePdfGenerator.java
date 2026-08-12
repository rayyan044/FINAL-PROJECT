package com.falconenergy.util;

import com.falconenergy.entity.Delivery;
import com.falconenergy.entity.DeliveryNote;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

public class DeliveryNotePdfGenerator {

    public static byte[] generate(Delivery delivery) {
        if (delivery == null || delivery.getDeliveryNote() == null) {
            throw new IllegalArgumentException("Delivery and its associated Delivery Note must not be null");
        }

        DeliveryNote note = delivery.getDeliveryNote();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Set up fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, new java.awt.Color(30, 58, 138)); // Primary Color #1E3A8A
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, java.awt.Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, java.awt.Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, java.awt.Color.BLACK);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Font.NORMAL, java.awt.Color.GRAY);

            // Header Section
            Paragraph logoParagraph = new Paragraph("FALCON ENERGY", titleFont);
            logoParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(logoParagraph);

            Paragraph docTypeParagraph = new Paragraph("DELIVERY NOTE", subtitleFont);
            docTypeParagraph.setAlignment(Element.ALIGN_CENTER);
            docTypeParagraph.setSpacingAfter(20);
            document.add(docTypeParagraph);

            // Details Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);
            
            // Widths for columns
            table.setWidths(new float[]{1f, 1f});

            // 1. Document Info (Left Column)
            PdfPCell cell1 = new PdfPCell();
            cell1.setBorder(Rectangle.NO_BORDER);
            cell1.setPadding(8);
            cell1.addElement(new Paragraph("DOCUMENT INFORMATION", boldFont));
            cell1.addElement(new Paragraph("Delivery Note No: " + note.getDeliveryNoteNumber(), regularFont));
            cell1.addElement(new Paragraph("Delivery Date: " + (note.getPreparedAt() != null ? note.getPreparedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A"), regularFont));
            cell1.addElement(new Paragraph("Delivery Reference: " + delivery.getDeliveryNumber(), regularFont));
            table.addCell(cell1);

            // 2. Customer Info (Right Column)
            PdfPCell cell2 = new PdfPCell();
            cell2.setBorder(Rectangle.NO_BORDER);
            cell2.setPadding(8);
            cell2.addElement(new Paragraph("CUSTOMER DETAILS", boldFont));
            cell2.addElement(new Paragraph("Buyer/Customer: " + (note.getCustomer() != null ? note.getCustomer().getCompanyName() : "N/A"), regularFont));
            cell2.addElement(new Paragraph("Destination: " + note.getDestination(), regularFont));
            table.addCell(cell2);

            // 3. Driver & Vehicle Info (Left Column)
            PdfPCell cell3 = new PdfPCell();
            cell3.setBorder(Rectangle.NO_BORDER);
            cell3.setPadding(8);
            cell3.addElement(new Paragraph("DRIVER & VEHICLE", boldFont));
            cell3.addElement(new Paragraph("Driver Name: " + note.getDriverName(), regularFont));
            cell3.addElement(new Paragraph("Vehicle Reg No: " + note.getTruckNumber(), regularFont));
            table.addCell(cell3);

            // 4. Fuel & Product Details (Right Column)
            PdfPCell cell4 = new PdfPCell();
            cell4.setBorder(Rectangle.NO_BORDER);
            cell4.setPadding(8);
            cell4.addElement(new Paragraph("FUEL PRODUCT DETAILS", boldFont));
            cell4.addElement(new Paragraph("Product: " + (note.getProduct() != null ? note.getProduct().getProductName() : "N/A"), regularFont));
            cell4.addElement(new Paragraph("Quantity: " + (note.getStandardVolume() != null ? String.format("%,.2f", note.getStandardVolume()) : "0.00") + " L", regularFont));
            table.addCell(cell4);

            document.add(table);

            // Divider Line
            Paragraph divider = new Paragraph("────────────────────────────────────────────────────────────────────────", regularFont);
            divider.setAlignment(Element.ALIGN_CENTER);
            divider.setSpacingAfter(30);
            document.add(divider);

            // Signature Section
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setWidths(new float[]{1f, 1f});
            sigTable.setSpacingAfter(40);

            PdfPCell driverSigCell = new PdfPCell();
            driverSigCell.setBorder(Rectangle.NO_BORDER);
            driverSigCell.setPadding(10);
            driverSigCell.addElement(new Paragraph("Driver Signature:", boldFont));
            driverSigCell.addElement(new Paragraph("\n\n__________________________________", regularFont));
            driverSigCell.addElement(new Paragraph("Name: " + note.getDriverName(), regularFont));
            sigTable.addCell(driverSigCell);

            PdfPCell customerSigCell = new PdfPCell();
            customerSigCell.setBorder(Rectangle.NO_BORDER);
            customerSigCell.setPadding(10);
            customerSigCell.addElement(new Paragraph("Customer Received By Signature:", boldFont));
            customerSigCell.addElement(new Paragraph("\n\n__________________________________", regularFont));
            customerSigCell.addElement(new Paragraph("Name: ____________________________", regularFont));
            sigTable.addCell(customerSigCell);

            document.add(sigTable);

            // Footer Section
            Paragraph footerText = new Paragraph("Generated Date/Time: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " | Document Reference: " + note.getDeliveryNoteNumber(), footerFont);
            footerText.setAlignment(Element.ALIGN_CENTER);
            document.add(footerText);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Delivery Note PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }
}
