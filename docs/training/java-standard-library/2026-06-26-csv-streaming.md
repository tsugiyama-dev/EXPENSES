# 2026-06-26 CSVストリーミング出力

## 目的

MyBatis CursorやHTTPレスポンスを直接使わず、Java標準ライブラリの `BufferedWriter`、`Files`、`Stream` でCSVを1行ずつ出力する。大量データをメモリに溜めない設計を理解する。

## 7時間の進め方

- 1.0h: `ExpenseExportService#exportAllAsCsvStream` と `ExportController` を読む
- 1.5h: `java.io`、`java.nio.file`、try-with-resources、CSVエスケープを復習する
- 2.0h: CSVストリーミング出力を実装する
- 1.0h: 一括生成と逐次出力の違いを整理する
- 1.0h: エクスポート処理の責務分割を考える
- 0.5h: 日報を作成する

## 成果物

### CSV行変換

```java
class ExpenseCsvFormatter {
    String header() {
        return "id,applicantId,title,amount,status";
    }

    String line(Expense expense) {
        return String.join(",",
                String.valueOf(expense.id()),
                String.valueOf(expense.applicantId()),
                escape(expense.title()),
                String.valueOf(expense.amount()),
                expense.status().name());
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
```

### 逐次出力

```java
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class ExpenseCsvExporter {
    private final ExpenseCsvFormatter formatter = new ExpenseCsvFormatter();

    void export(Iterable<Expense> expenses, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            writer.write(formatter.header());
            writer.newLine();

            for (Expense expense : expenses) {
                writer.write(formatter.line(expense));
                writer.newLine();
            }
        }
    }
}
```

## 実務への接続

ExcelやPDFのように全件を `byte[]` にしてから返す方式は、件数が増えるとメモリを圧迫する。CSVのように1行ずつ書ける形式は、ストリーミング出力に向いている。`expenses` の `exportAllAsCsvStream` はこの考え方に近く、CursorとOutputStreamを使って一定メモリで出力する。

## リファクタ観点

- CSVの行変換、出力先、検索処理を分ける
- Controllerにファイル名やHTTPヘッダー以外の処理を持たせすぎない
- CSVエスケープ処理をテストできる単位へ切り出す
- 出力件数、処理時間、失敗理由をログやメトリクスに残す
- Excel、PDF、CSVで共通化すべき処理と分けるべき処理を見極める

## 日報

### 作業概要

- Javaの `BufferedWriter` と `Files` を使い、CSVを1行ずつ出力する処理を実装した
- `try-with-resources` によるリソースクローズの方法を確認した
- `StandardCharsets.UTF_8` とBOMの扱いを確認した
- カンマ、改行、ダブルクォートを含む文字列のCSVエスケープ処理を作成した
- 一括で文字列を作る方法と、逐次書き込みを行う方法の違いを整理した

### 作業所感

Java I/Oを使ったファイル出力では、書き込み処理だけでなく文字コードやリソース管理も重要になると分かった。`try-with-resources` を使うと、例外が発生した場合でもWriterを閉じやすくなる。CSVは単純な形式に見えるが、エスケープ処理を正しく行わないとデータが崩れるため、出力処理は小さく分けて確認できる形にする必要があると感じた。
