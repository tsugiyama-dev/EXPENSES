CREATE TABLE IF NOT EXISTS pending_notifications (
  id bigint primary key auto_increment,
  user_id bigint not null,
  type varchar(50) not null,
  expense_id bigint,
  message varchar(500) not null,
  is_read boolean not null default false,
  created_at datetime not null default current_timestamp,
  INDEX idx_user_id_is_read (user_id, is_read)
);