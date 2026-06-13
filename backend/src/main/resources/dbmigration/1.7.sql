-- apply alter tables
alter table investments alter column type drop default;
-- foreign keys and indices
create index if not exists idx_account_report_account_date on account_reports (account_id,date);
create index if not exists idx_expense_user_date on expenses (user_id,date);
create index if not exists idx_user_google_id on users (google_id);
