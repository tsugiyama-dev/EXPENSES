

create table if not exists users (
	
	id bigint auto_increment primary key,
	username varchar(100) not null unique,
	password varchar(255) not null,
	role varchar(30) not null,
	created_at datetime default current_timestamp
);