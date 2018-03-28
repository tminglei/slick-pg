insert into SUPPLIER values(49,  'Superior Coffee', '1 Party Place', 'Mendocino', 'CA', '95460');
insert into SUPPLIER values(101, 'Acme, Inc.', '99 Market Street', 'Groundsville', 'CA', '95199');
insert into SUPPLIER values(150, 'The High Ground', '100 Coffee Lane', 'Meadows', 'CA', '93966');
insert into SUPPLIER values(456, 'Restaurant Supplies, Inc.', '200 Magnolia Street', 'Meadows', 'CA', '93966');
insert into SUPPLIER values(927, 'Professional Kitchen', '300 Daisy Avenue', 'Groundsville', 'CA', '95199');

insert into COFFEE values('Colombian',          101, 7.99, 0, 0);
insert into COFFEE values('French_Roast',       49,  8.99, 0, 0);
insert into COFFEE values('Espresso',           150, 9.99, 0, 0);
insert into COFFEE values('Colombian_Decaf',    101, 8.99, 0, 0);
insert into COFFEE values('French_Roast_Decaf', 049, 9.99, 0, 0);

insert into COFFEE_INVENTORY values(1234, 'Colombian',       101, 0, '2006-04-01');
insert into COFFEE_INVENTORY values(1234, 'French_Roast',    49,  0, '2006-04-01');
insert into COFFEE_INVENTORY values(1234, 'Espresso',        150, 0, '2006-04-01');
insert into COFFEE_INVENTORY values(1234, 'Colombian_Decaf', 101, 0, '2006-04-01');

insert into MERCH_INVENTORY values(00001234, 'Cup_Large', 456, 28, '2006-04-01');
insert into MERCH_INVENTORY values(00001235, 'Cup_Small', 456, 36, '2006-04-01');
insert into MERCH_INVENTORY values(00001236, 'Saucer', 456, 64, '2006-04-01');
insert into MERCH_INVENTORY values(00001287, 'Carafe', 456, 12, '2006-04-01');
insert into MERCH_INVENTORY values(00006931, 'Carafe', 927, 3, '2006-04-01');
insert into MERCH_INVENTORY values(00006935, 'PotHolder', 927, 88, '2006-04-01');
insert into MERCH_INVENTORY values(00006977, 'Napkin', 927, 108, '2006-04-01');
insert into MERCH_INVENTORY values(00006979, 'Towel', 927, 24, '2006-04-01');
insert into MERCH_INVENTORY values(00004488, 'CofMaker', 456, 5, '2006-04-01');
insert into MERCH_INVENTORY values(00004490, 'CofGrinder', 456, 9, '2006-04-01');
insert into MERCH_INVENTORY values(00004495, 'EspMaker', 456, 4, '2006-04-01');
insert into MERCH_INVENTORY values(00006914, 'Cookbook', 927, 12, '2006-04-01');

insert into COFFEE_HOUSE values(10023, 'Mendocino', 3450, 2005, 5455);
insert into COFFEE_HOUSE values(33002, 'Seattle', 4699, 3109, 7808);
insert into COFFEE_HOUSE values(10040, 'SF', 5386, 2841, 8227);
insert into COFFEE_HOUSE values(32001, 'Portland', 3147, 3579, 6726);
insert into COFFEE_HOUSE values(10042, 'SF', 2863, 1874, 4710);
insert into COFFEE_HOUSE values(10024, 'Sacramento', 1987, 2341, 4328);
insert into COFFEE_HOUSE values(10039, 'Carmel', 2691, 1121, 3812);
insert into COFFEE_HOUSE values(10041, 'LA', 1533, 1007, 2540);
insert into COFFEE_HOUSE values(33005, 'Olympia', 2733, 1550, 4283);
insert into COFFEE_HOUSE values(33010, 'Seattle', 3210, 2177, 5387);
insert into COFFEE_HOUSE values(10035, 'SF', 1922, 1056, 2978);
insert into COFFEE_HOUSE values(10037, 'LA', 2143, 1876, 4019);
insert into COFFEE_HOUSE values(10034, 'San_Jose', 1234, 1032, 2266);
insert into COFFEE_HOUSE values(32004, 'Eugene', 1356, 1112, 2468);
