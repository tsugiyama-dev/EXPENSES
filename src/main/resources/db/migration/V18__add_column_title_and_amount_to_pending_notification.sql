alter table pending_notifications 
add column title varchar(100) not null after expense_id,
add column amount decimal(12, 2) not null after title;