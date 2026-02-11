
alter table users
	change username email varchar(255) not null unique
	