create schema idx_cand_test;
set schema 'idx_cand_test';
create table t1 (
  c1 int,
  c2 int,
  c3 int
);

create table t2 (
  cc1 char(12),
  cc2 float
);

insert into t2 values ('space ball22', 12.5), ('drink 7up 2L', 2.1);

insert into t1 values (1, 2, 3), (4, 5, 6), (7, 8, 9), (10, 11, 12),
(13, 14, 15), (16, 17, 18), (19, 20, 21), (25, 23, 24), (25, 26, 27),
(25, 26, 27);

-- verify udx is working
select * from table(applib.show_idx_candidates('IDX_CAND_TEST', 'T1', 90));
select * from table(applib.show_idx_candidates('IDX_CAND_TEST', 'T1', 80));
select * from table(applib.show_idx_candidates('LOCALDB', 'IDX_CAND_TEST', 'T1', 70));
select * from table(applib.show_idx_candidates('SYS_FEM', 'IDX_CAND_TEST', 'T1', 70));

select * from table(applib.show_idx_candidates('IDX_CAND_TEST', 'T2', 50));

-- should reject cols with idxs already
create index idx_cand_c1 on idx_cand_test.t1(c1);
select * from table(applib.show_idx_candidates('IDX_CAND_TEST', 'T1', 80));
create index idx_cand_c2 on idx_cand_test.t1(c2);
select * from table(applib.show_idx_candidates('IDX_CAND_TEST', 'T1', 80));
create index idx_cand_c3 on idx_cand_test.t1(c3);
select * from table(applib.show_idx_candidates('IDX_CAND_TEST', 'T1', 80));

drop schema idx_cand_test cascade;