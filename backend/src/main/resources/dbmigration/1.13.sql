-- drop dependencies
alter table sync_tombstones drop constraint if exists ck_sync_tombstones_type;
-- apply alter tables
alter table recurring_expenses add column if not exists uuid uuid default gen_random_uuid() not null;
alter table recurring_expenses add column if not exists revision bigint default 0 not null;
alter table recurring_expenses add constraint uq_recurring_expenses_uuid unique (uuid);
alter table sync_tombstones alter column type type varchar(17);
-- apply post alter
alter table sync_tombstones add constraint ck_sync_tombstones_type check ( type in ('CATEGORY','SUBCATEGORY','TAG','EXPENSE','RECURRING_EXPENSE'));
-- foreign keys and indices
create index if not exists ix_recurring_expenses_user_id_revision on recurring_expenses (user_id,revision);

-- Sync: number the existing recurring expenses after each user's latest revision, so that
-- devices download them on their next pull.
create temporary table sync_backfill as
select r.id, r.user_id, u.sync_revision + row_number() over (partition by r.user_id order by r.id) as revision
from recurring_expenses r
join users u on u.id = r.user_id;

update recurring_expenses r set revision = b.revision from sync_backfill b where b.id = r.id;
update users u set sync_revision = b.revision
from (select user_id, max(revision) as revision from sync_backfill group by user_id) b
where b.user_id = u.id;

drop table sync_backfill;

-- Sync: recurring expenses take revisions and leave tombstones like the other synced rows.
create trigger recurring_expenses_sync_revision before insert or update on recurring_expenses
    for each row execute function sync_stamp_revision();
create trigger recurring_expenses_sync_tombstone after delete on recurring_expenses
    for each row execute function sync_record_tombstone('RECURRING_EXPENSE');

-- Tags are part of a recurring expense as devices see it: linking or unlinking one gives the
-- recurring expense a new revision.
create or replace function sync_touch_tagged_recurring_expense() returns trigger as $$
begin
    if tg_op = 'DELETE' then
        update recurring_expenses set revision = revision where id = old.recurring_expense_id;
    else
        update recurring_expenses set revision = revision where id = new.recurring_expense_id;
    end if;
    return null;
end;
$$ language plpgsql;

create trigger recurring_expense_tag_sync_revision after insert or delete on recurring_expense_tag
    for each row execute function sync_touch_tagged_recurring_expense();
