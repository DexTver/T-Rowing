-- Импорт многодневных протоколов и базы спортсменов.
-- 1) День у заезда: предв. и финалы одной категории могут идти в разные дни,
--    поэтому день привязывается к заезду, а не только к категории.
alter table heat add column competition_day_id bigint references competition_day (id);
create index idx_heat_day on heat (competition_day_id);

-- 2) Внешний номер спортсмена («Номер» из листа «база») — ключ связи протокола с базой.
alter table athlete add column ext_number bigint;
create index idx_athlete_ext_number on athlete (ext_number);
