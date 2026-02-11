

create table if not exists expense_audit_logs (
	id bigint primary key,
	expense_id bigint,
	actor_id bigint,
	action enum('CREATE','SUBMIT','APPROVE', 'REJECT'),
	before_status enum('CREATE', 'SUBMIT', 'APPROVE', 'REJECT'),
	after_status enum('CREATE', 'SUBMIT', 'APPROVE', 'REJECT'),
	note varchar(100),
	trace_id bigint,
	created_at datetime default current_timestamp
--	foreign key (expense_id) references expenses(id) on delete cascade
)
