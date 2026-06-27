-- apply alter tables
alter table investment_accounts add column if not exists type varchar(10) default 'OTHER' not null;
-- apply post alter
alter table investment_accounts add constraint ck_investment_accounts_type check ( type in ('CHECKING','SAVINGS','INVESTMENT','RETIREMENT','OTHER'));
