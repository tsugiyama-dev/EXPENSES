CREATE TABLE categories (
	id bigint auto_increment primary key,
	name varchar(100) not null,
	description text,
	color varchar(7) default '#6B7280', -- 16進数カラーコード
	icon varchar(50) default 'tag',
	active boolean default true,
	created_at datetime default current_timestamp,
	updated_at datetime default current_timestamp on update current_timestamp,
	unique key uk_category_name (name)
) engine=InnoDB default charset=utf8mb4;

-- expenses テーブルにカテゴリIDを追加

ALTER TABLE expenses
ADD COLUMN category_id bigint,
add constraint fk_expense_category
	foreign key (category_id) references categories(id);

-- 検索高速化のインデックス
CREATE INDEX idx_expense_category on expenses(category_id);

INSERT INTO categories (name, description, color, icon) VALUES
('交通費', '電車、タクシー、飛行機などの交通費', '#3B82F6', 'airplane'),
('食事', '会食、接待、出張時の食事代', '#EF4444', 'utensils'),
('事務用品', '文房具、消耗品など', '#10B981', 'paperclip'),
('ソフトウェア', 'ライセンス、サブスクリプション', '#8B5CF6', 'code'),
('研修', '書籍、セミナー、研修費用', '#F59E0B', 'book'),
('接待交際費', 'クライアント接待、イベント参加費', '#EC4899', 'ticket'),
('その他', 'その他の経費', '#6B7280', 'tag');
