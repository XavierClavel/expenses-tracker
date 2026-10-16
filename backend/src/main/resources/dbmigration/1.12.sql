-- apply changes
create table sync_tombstones (
  uuid                          uuid not null,
  user_id                       bigint not null,
  revision                      bigint default 0 not null,
  type                          varchar(11) not null,
  constraint ck_sync_tombstones_type check ( type in ('CATEGORY','SUBCATEGORY','TAG','EXPENSE')),
  constraint pk_sync_tombstones primary key (uuid)
);

-- apply alter tables
alter table categories add column if not exists uuid uuid default gen_random_uuid() not null;
alter table categories add column if not exists revision bigint default 0 not null;
alter table expenses add column if not exists uuid uuid default gen_random_uuid() not null;
alter table expenses add column if not exists revision bigint default 0 not null;
alter table subcategories add column if not exists uuid uuid default gen_random_uuid() not null;
alter table subcategories add column if not exists revision bigint default 0 not null;
alter table tags add column if not exists uuid uuid default gen_random_uuid() not null;
alter table tags add column if not exists revision bigint default 0 not null;
alter table users add column if not exists sync_revision bigint default 0 not null;
alter table categories add constraint uq_categories_uuid unique (uuid);
alter table expenses add constraint uq_expenses_uuid unique (uuid);
alter table subcategories add constraint uq_subcategories_uuid unique (uuid);
alter table tags add constraint uq_tags_uuid unique (uuid);
-- foreign keys and indices
create index ix_sync_tombstones_user_id on sync_tombstones (user_id);
alter table sync_tombstones add constraint fk_sync_tombstones_user_id foreign key (user_id) references users (id) on delete cascade on update restrict;

create index if not exists ix_categories_user_id_revision on categories (user_id,revision);
create index if not exists ix_expenses_user_id_revision on expenses (user_id,revision);
create index if not exists ix_subcategories_user_id_revision on subcategories (user_id,revision);
create index if not exists ix_sync_tombstones_user_id_revision on sync_tombstones (user_id,revision);
create index if not exists ix_tags_user_id_revision on tags (user_id,revision);

-- Sync: number every existing row, per user, so that devices download them on their first pull.
-- Categories come first, then subcategories, tags and expenses, so that a row never comes before
-- a row it references.
create temporary table sync_backfill as
select kind, id, user_id, row_number() over (partition by user_id order by kind, id) as revision
from (
    select 1 as kind, id, user_id from categories
    union all select 2, id, user_id from subcategories
    union all select 3, id, user_id from tags
    union all select 4, id, user_id from expenses
) as synced;

update categories c set revision = b.revision from sync_backfill b where b.kind = 1 and b.id = c.id;
update subcategories s set revision = b.revision from sync_backfill b where b.kind = 2 and b.id = s.id;
update tags t set revision = b.revision from sync_backfill b where b.kind = 3 and b.id = t.id;
update expenses e set revision = b.revision from sync_backfill b where b.kind = 4 and b.id = e.id;
update users u set sync_revision = b.revision
from (select user_id, max(revision) as revision from sync_backfill group by user_id) b
where b.user_id = u.id;

drop table sync_backfill;

-- Sync: every change to a user's synced rows takes the next value of users.sync_revision.
-- Incrementing it locks the user's row until commit, so one user's revisions commit in order:
-- a device that pulled every row up to revision N can't miss one later committed below N.
create or replace function sync_next_revision(owner_id bigint) returns bigint as $$
    update users set sync_revision = sync_revision + 1 where id = owner_id returning sync_revision;
$$ language sql;

create or replace function sync_stamp_revision() returns trigger as $$
begin
    new.revision := sync_next_revision(new.user_id);
    return new;
end;
$$ language plpgsql;

-- Deleting a synced row leaves a tombstone, which devices pull to delete their copy.
create or replace function sync_record_tombstone() returns trigger as $$
begin
    insert into sync_tombstones (uuid, user_id, type) values (old.uuid, old.user_id, tg_argv[0])
    on conflict (uuid) do update set type = excluded.type;
    return old;
end;
$$ language plpgsql;

-- Tags are part of an expense as devices see it: linking or unlinking one gives the expense a
-- new revision.
create or replace function sync_touch_tagged_expense() returns trigger as $$
begin
    if tg_op = 'DELETE' then
        update expenses set revision = revision where id = old.expense_id;
    else
        update expenses set revision = revision where id = new.expense_id;
    end if;
    return null;
end;
$$ language plpgsql;

create trigger categories_sync_revision before insert or update on categories
    for each row execute function sync_stamp_revision();
create trigger subcategories_sync_revision before insert or update on subcategories
    for each row execute function sync_stamp_revision();
create trigger tags_sync_revision before insert or update on tags
    for each row execute function sync_stamp_revision();
create trigger expenses_sync_revision before insert or update on expenses
    for each row execute function sync_stamp_revision();
create trigger sync_tombstones_sync_revision before insert or update on sync_tombstones
    for each row execute function sync_stamp_revision();

create trigger categories_sync_tombstone after delete on categories
    for each row execute function sync_record_tombstone('CATEGORY');
create trigger subcategories_sync_tombstone after delete on subcategories
    for each row execute function sync_record_tombstone('SUBCATEGORY');
create trigger tags_sync_tombstone after delete on tags
    for each row execute function sync_record_tombstone('TAG');
create trigger expenses_sync_tombstone after delete on expenses
    for each row execute function sync_record_tombstone('EXPENSE');

create trigger expense_tag_sync_revision after insert or delete on expense_tag
    for each row execute function sync_touch_tagged_expense();
