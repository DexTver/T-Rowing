-- Поддержка импорта реальных стартовых протоколов (Excel).
-- 1) Дистанция: в реальных протоколах встречаются и другие дистанции (например, 2000 м),
--    поэтому ослабляем жёсткий перечень до «положительное число».
alter table category drop constraint if exists category_distance_m_check;
alter table category add constraint category_distance_m_check check (distance_m > 0);

-- 2) Правило прохода заезда («1-6 л в п/ф», «1-2 + луч по времени в фин» и т.п.) —
--    свободный текст из протокола, для отображения «куда проходит» (раздел 7.1.2).
alter table heat add column advancement varchar(160);
