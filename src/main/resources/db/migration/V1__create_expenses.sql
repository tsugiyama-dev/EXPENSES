create table if not exists expenses(
	id bigint primary key,
	applicant_id bigint ,
	title varchar(100),
	amount decimal,
	currency decimal,
	status enum('DRAFT', 'SUBMITTED'),
	submitted_at datetime default null,
	created_at datetime default current_timestamp,
	updated_at datetime,
	version int
)

