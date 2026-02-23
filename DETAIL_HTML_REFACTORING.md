# detail.html リファクタリングメモ

## 🔍 修正した問題点

---

## 1. `<meta charset>` の位置（HTMLベストプラクティス）

### ❌ 修正前

```html
<head>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap..." rel="stylesheet">  <!-- 先にCSSを読む -->
  <meta charset="UTF-8">                                                     <!-- 後からcharset宣言 -->
```

### ✅ 修正後

```html
<head>
  <meta charset="UTF-8">                                                     <!-- 最初にcharset宣言 -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap..." rel="stylesheet">
```

### 💡 なぜ問題なのか

`<meta charset>` はHTMLの最初に来るべきものです。
ブラウザは文字コードを知らないとHTMLを正しく解釈できないため、**文字化けの原因**になることがあります。

---

## 2. navbar のBootstrapクラス不足

### ❌ 修正前

```html
<nav class="navbar">
  <div class="container-fluid">
    <a class="navbar-brand" href="#">Expenses</a>
    <button type="button" style="border: none; background-color: transparent;">
      <a th:href="..." style="text-decoration: none; color: green;">経費登録フォームへ</a>
    </button>
  </div>
</nav>
<form th:action="@{/logout}" method="post">
  <button type="submit" style="border: none; color: red;">ログアウト</button>
</form>
<p>ログイン中のユーザー：<strong th:text="${username}"></strong></p>
```

### ✅ 修正後

```html
<nav class="navbar navbar-expand-lg bg-dark navbar-dark px-3">
  <div class="container-fluid">
    <a class="navbar-brand" href="#">Expenses</a>
    <div class="d-flex align-items-center gap-3">
      <span class="text-white">ログイン中：<strong th:text="${username}"></strong></span>
      <a th:href="..." class="btn btn-outline-success btn-sm">経費登録フォームへ</a>
      <form th:action="@{/logout}" method="post" class="m-0">
        <button type="submit" class="btn btn-outline-danger btn-sm">ログアウト</button>
      </form>
    </div>
  </div>
</nav>
```

### 💡 学習ポイント

| 変更点 | 理由 |
|--------|------|
| `navbar-expand-lg bg-dark navbar-dark` を追加 | Bootstrapのnavbarは基本クラスが必要 |
| インラインスタイルを削除 | Bootstrapのユーティリティクラスで代替 |
| ログアウトをnavの中に移動 | UIとして自然な配置 |
| `btn btn-outline-success btn-sm` | `style="color: green"` の代替 |
| `btn btn-outline-danger btn-sm` | `style="color: red"` の代替 |

---

## 3. インラインスタイルをBootstrapクラスに置き換え

### ❌ 修正前

```html
<div class="m-3" style="background-color: lightgray;">
```

```html
<strong th:text="${message}" style="color: forestgreen;"></strong>
```

```html
<strong th:text="${alert}" style="color: orangered;"></strong>
```

### ✅ 修正後

```html
<div class="m-3 bg-light rounded p-3">
```

```html
<strong th:text="${message}" class="text-success"></strong>
```

```html
<strong th:text="${alert}" class="text-danger"></strong>
```

### 💡 インラインスタイル vs Bootstrapクラス

```
❌ style="color: forestgreen"  →  ✅ class="text-success"
❌ style="color: orangered"    →  ✅ class="text-danger"
❌ style="color: white"        →  ✅ class="text-white"
❌ style="background-color: lightgray" → ✅ class="bg-light"
```

**なぜインラインスタイルを避けるか？**
- 保守性が低い（色を変えたいとき全部書き換えが必要）
- Bootstrapのテーマ変更に追従できない
- CSSの優先度が高すぎて後からスタイルを上書きしにくい

---

## 4. `<td class="badge">` の誤った使い方

### ❌ 修正前

```html
<td class="badge"
    th:classappend="${expense.status.name() == 'APPROVED'} ? 'bg-success' : ..."
    th:text="${expense.status}">
</td>
```

### ✅ 修正後

```html
<td>
  <span class="badge"
        th:classappend="${expense.status.name() == 'APPROVED'} ? 'bg-success' :
                        (${expense.status.name() == 'SUBMITTED'} ? 'bg-danger' :
                        (${expense.status.name() == 'REJECTED'} ? 'bg-warning text-dark' : 'bg-secondary'))"
        th:text="${expense.status}">
  </span>
</td>
```

### 💡 なぜ問題なのか

`badge` は **インライン要素（`<span>`）に使うBootstrapコンポーネント**です。
`<td>` はブロックレベルの表セルなので、badgeのスタイルが意図通りに適用されません。

```
❌ <td class="badge">   →  badgeがセル全体に広がってしまう
✅ <td><span class="badge">...</span></td>  →  バッジがセル内で適切に表示される
```

---

## 5. `<button>` 内の `<a>` タグ（不正なHTML）

### ❌ 修正前

```html
<button type="submit" class="btn btn-primary">
  <a th:href="@{/expenses/csv(...)}" style="color: white;">csvダウンロード</a>
</button>
```

### ✅ 修正後

```html
<a th:href="@{/expenses/csv(...)}" class="btn btn-primary">csvダウンロード</a>
```

### 💡 なぜ問題なのか

HTMLの仕様では **`<button>` の中に `<a>` を入れることは禁止されています**（インタラクティブコンテンツの入れ子禁止）。

```
❌ <button><a href="...">リンク</a></button>  →  HTMLの仕様違反
✅ <a class="btn btn-primary" href="...">リンク</a>  →  Bootstrapでボタン風のリンクを作れる
```

---

## 6. 提出フォームの閉じタグ問題

### ❌ 修正前

```html
<div th:if="${expense.status.name() == 'DRAFT'}">
  <form th:action="@{|/expenses/submit/${expense.id}|}" method="post">
  <button type="submit">提出</button>
</div>   ← formが閉じていない！divが先に閉じている
```

### ✅ 修正後

```html
<div th:if="${expense.status.name() == 'DRAFT'}">
  <form th:action="@{|/expenses/submit/${expense.id}|}" method="post">
    <button type="submit" class="btn btn-sm btn-outline-primary">提出</button>
  </form>    ← formを正しく閉じる
</div>
```

### 💡 なぜ問題なのか

`<form>` タグが閉じられていないと、ブラウザが暗黙的に補完しようとします。
特にネストした複数の `<form>` がある場合、意図しない動作になります。

> **補足**: HTMLでは `<form>` の中に `<form>` を入れることは禁止されています。

---

## 7. `<input></input>` の書き方

### ❌ 修正前

```html
<input class="" placeholder="入力不可" disabled></input>
```

### ✅ 修正後

```html
<input placeholder="入力不可" disabled>
```

### 💡 ポイント

`<input>` は**空要素（void element）**のため、閉じタグ `</input>` は存在しません。
また空の `class=""` も不要なので削除します。

---

## 8. `<label>title</label>` の英語ラベル

### ❌ 修正前

```html
<label>title</label>
<input th:field="*{title}" placeholder="タイトル">
```

### ✅ 修正後

```html
<label>タイトル</label>
<input th:field="*{title}" placeholder="タイトル">
```

### 💡 ポイント

ユーザーに表示されるラベルが英語のままになっていました。
他のラベルは日本語なので統一します。

---

## 📊 修正内容のまとめ

| # | 種類 | 問題 | 修正方法 |
|---|------|------|---------|
| 1 | ベストプラクティス | `<meta charset>` の位置が `<link>` より後 | head の先頭に移動 |
| 2 | UI/クラス不足 | navbar にBootstrapクラスがない | `navbar-expand-lg bg-dark navbar-dark` を追加 |
| 3 | スタイル混在 | インラインスタイルとBootstrapが混在 | Bootstrapユーティリティクラスに統一 |
| 4 | 誤ったHTML | `<td>` に `badge` クラスを使用 | `<span class="badge">` を `<td>` の中に入れる |
| 5 | HTML仕様違反 | `<button>` の中に `<a>` タグ | `<a class="btn btn-primary">` に変更 |
| 6 | 閉じタグ不正 | `<form>` が閉じられないまま `<div>` が閉じる | `</form>` を正しい位置に追加 |
| 7 | 空要素の書き方 | `<input></input>` と閉じタグあり | `<input>` に修正 |
| 8 | 日本語統一 | `<label>title</label>` が英語のまま | `<label>タイトル</label>` に変更 |
