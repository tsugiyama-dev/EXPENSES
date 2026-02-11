create table if not exists roles (
	id bigint auto_increment primary key,
	user_id bigint not null,
	role varchar(30) not null,
	created_at datetime default current_timestamp,
	updated_at datetime,
	foreign key (user_id) references users(id) on delete cascade
)