-- drop dependencies
alter table investments drop constraint if exists ck_investments_type;
-- apply alter tables
alter table investment_accounts add column if not exists tracking varchar(13) default 'CONTRIBUTIONS' not null;
alter table investments alter column type type varchar(8);
-- apply post alter
alter table investments add constraint ck_investments_type check ( type in ('IN','OUT','INTEREST','FEE'));
alter table investment_accounts add constraint ck_investment_accounts_tracking check ( tracking in ('CONTRIBUTIONS','INTEREST'));
