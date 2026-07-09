-- План категории теперь может быть не только буквой A–Q, но и «A-alt» (жеребьёвка в 2 полуфинала).
alter table category alter column plan type varchar(16);
