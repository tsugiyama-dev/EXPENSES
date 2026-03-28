

create table receipts (
	id BIGINT auto_increment PRIMARY KEY,
	expense_id BIGINT not null,
	original_finename varchar(255) not null,
	stored_filename varchar(255) not null,
	file_path varchar(500) not null,
	content_type varchar(100) not null,
	file_size BIGINT NOT NULL,
	uploaded_by VARCHAR(255) NOT NULL,
	uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
	INDEX idx_expense_id (expense_id)
);