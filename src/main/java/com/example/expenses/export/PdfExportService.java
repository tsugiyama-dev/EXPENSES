package com.example.expenses.export;

import com.example.expenses.domain.Expense;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDFエクスポートサービス
 *
 * <p><b>学習ポイント:</b></p>
 * <ul>
 *   <li>OpenPDFの使い方</li>
 *   <li>PDFファイルの生成</li>
 *   <li>テーブル、段落、画像の挿入</li>
 *   <li>フォント設定</li>
 *   <li>レイアウト設計</li>
 * </ul>
 */
@Service
public class PdfExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 経費詳細をPDF形式でエクスポートします
     *
     * @param expense 経費
     * @return PDFファイルのバイト配列
     * @throws IOException ファイル生成エラー
     */
    public byte[] exportExpenseDetail(Expense expense) throws IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // タイトル
            addTitle(document, "経費詳細レポート");

            // 空行
            document.add(new Paragraph(" "));

            // 経費情報テーブル
            addExpenseInfoTable(document, expense);

            // 空行
            document.add(new Paragraph(" "));

            // フッター
            addFooter(document);

        } catch (DocumentException e) {
            throw new IOException("PDF生成エラー", e);
        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }

    /**
     * 経費一覧をPDF形式でエクスポートします
     *
     * @param expenses 経費リスト
     * @return PDFファイルのバイト配列
     * @throws IOException ファイル生成エラー
     */
    public byte[] exportExpenseList(List<Expense> expenses) throws IOException {
        Document document = new Document(PageSize.A4.rotate()); // 横向き
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // タイトル
            addTitle(document, "経費一覧レポート");

            // 出力日時
            Paragraph dateParagraph = new Paragraph("出力日時: " + LocalDateTime.now().format(DATE_FORMATTER));
            dateParagraph.setAlignment(Element.ALIGN_RIGHT);
            document.add(dateParagraph);

            // 空行
            document.add(new Paragraph(" "));

            // 経費一覧テーブル
            addExpenseListTable(document, expenses);

            // フッター
            addFooter(document);

        } catch (DocumentException e) {
            throw new IOException("PDF生成エラー", e);
        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }

    /**
     * タイトルを追加します
     */
    private void addTitle(Document document, String title) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);
    }

    /**
     * 経費情報テーブルを追加します
     */
    private void addExpenseInfoTable(Document document, Expense expense) throws DocumentException {
        PdfPTable table = new PdfPTable(2); // 2カラム
        table.setWidthPercentage(100);
        table.setWidths(new int[]{1, 2});

        // ヘッダースタイル
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font dataFont = new Font(Font.HELVETICA, 11);

        // 経費ID
        addTableRow(table, "経費ID", String.valueOf(expense.getId()), headerFont, dataFont);

        // タイトル
        addTableRow(table, "タイトル", expense.getTitle(), headerFont, dataFont);

        // 申請者ID
        addTableRow(table, "申請者ID", String.valueOf(expense.getApplicantId()), headerFont, dataFont);

        // 金額
        String amountStr = String.format("¥%,d %s",
                expense.getAmount().longValue(),
                expense.getCurrency());
        addTableRow(table, "金額", amountStr, headerFont, dataFont);

        // ステータス
        addTableRow(table, "ステータス", expense.getStatus().toString(), headerFont, dataFont);

        // 提出日時
        String submittedAt = expense.getSubmittedAt() != null
                ? expense.getSubmittedAt().format(DATE_FORMATTER)
                : "未提出";
        addTableRow(table, "提出日時", submittedAt, headerFont, dataFont);

        // 作成日時
        addTableRow(table, "作成日時", expense.getCreatedAt().format(DATE_FORMATTER), headerFont, dataFont);

        // 更新日時
        addTableRow(table, "更新日時", expense.getUpdatedAt().format(DATE_FORMATTER), headerFont, dataFont);

        document.add(table);
    }

    /**
     * 経費一覧テーブルを追加します
     */
    private void addExpenseListTable(Document document, List<Expense> expenses) throws DocumentException {
        PdfPTable table = new PdfPTable(7); // 7カラム
        table.setWidthPercentage(100);
        table.setWidths(new int[]{1, 2, 3, 2, 2, 2, 3});

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font dataFont = new Font(Font.HELVETICA, 9);

        // ヘッダー行
        String[] headers = {"No", "申請者ID", "タイトル", "金額", "通貨", "ステータス", "提出日時"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new java.awt.Color(200, 220, 255));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // データ行
        int no = 1;
        for (Expense expense : expenses) {
            // No
            addDataCell(table, String.valueOf(no++), dataFont, Element.ALIGN_CENTER);

            // 申請者ID
            addDataCell(table, String.valueOf(expense.getApplicantId()), dataFont, Element.ALIGN_CENTER);

            // タイトル
            addDataCell(table, expense.getTitle(), dataFont, Element.ALIGN_LEFT);

            // 金額
            String amount = String.format("%,d", expense.getAmount().longValue());
            addDataCell(table, amount, dataFont, Element.ALIGN_RIGHT);

            // 通貨
            addDataCell(table, expense.getCurrency(), dataFont, Element.ALIGN_CENTER);

            // ステータス
            addDataCell(table, expense.getStatus().toString(), dataFont, Element.ALIGN_CENTER);

            // 提出日時
            String submittedAt = expense.getSubmittedAt() != null
                    ? expense.getSubmittedAt().format(DATE_FORMATTER)
                    : "未提出";
            addDataCell(table, submittedAt, dataFont, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    /**
     * テーブル行を追加します
     */
    private void addTableRow(PdfPTable table, String label, String value,
                             Font labelFont, Font valueFont) {
        // ラベルセル
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(new java.awt.Color(240, 240, 240));
        labelCell.setPadding(8);
        table.addCell(labelCell);

        // 値セル
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(8);
        table.addCell(valueCell);
    }

    /**
     * データセルを追加します
     */
    private void addDataCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    /**
     * フッターを追加します
     */
    private void addFooter(Document document) throws DocumentException {
        // 空行
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // 区切り線
        Paragraph separator = new Paragraph("_".repeat(80));
        separator.setAlignment(Element.ALIGN_CENTER);
        document.add(separator);

        // フッターテキスト
        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC);
        Paragraph footer = new Paragraph("本レポートは経費管理システムより自動生成されました。", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(5f);
        document.add(footer);

        Paragraph footerDate = new Paragraph("出力日時: " + LocalDateTime.now().format(DATE_FORMATTER), footerFont);
        footerDate.setAlignment(Element.ALIGN_CENTER);
        document.add(footerDate);
    }
}
