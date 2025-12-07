-- apply alter tables
alter table expenses add column if not exists title varchar default '' not null;
