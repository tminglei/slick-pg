create table SUPPLIER
  (ID integer NOT NULL,
  NAME varchar(40) NOT NULL,
  LOCATION geometry,
  STREET varchar(40) NOT NULL,
  CITY varchar(20) NOT NULL,
  STATE char(2) NOT NULL,
  ZIP char(5),
  PRIMARY KEY (ID));
  
create table COFFEE
  (NAME varchar(32) NOT NULL,
  SUP_ID int NOT NULL,
  PRICE numeric(10,2) NOT NULL,
  SALES integer NOT NULL,
  TOTAL integer NOT NULL,
  PRIMARY KEY (NAME),
  FOREIGN KEY (SUP_ID) REFERENCES SUPPLIER (ID));
  
create table COFFEE_DESCRIPTION
  (NAME varchar(32) NOT NULL,
  DESCRIPTION text NOT NULL,
  PRIMARY KEY (NAME),
  FOREIGN KEY (NAME) REFERENCES COFFEE (NAME));

create table RSS_FEED
  (NAME varchar(32) NOT NULL,
  FEED_XML text NOT NULL,
  PRIMARY KEY (NAME));
  
create table COFFEE_INVENTORY
  (WAREHOUSE_ID integer NOT NULL,
  COFFEE_NAME varchar(32) NOT NULL,
  SUP_ID int NOT NULL,
  QUANTITY int NOT NULL,
  DATE_VAL timestamp,
  FOREIGN KEY (COFFEE_NAME) REFERENCES COFFEE (NAME),
  FOREIGN KEY (SUP_ID) REFERENCES SUPPLIER (ID));
  
create table MERCH_INVENTORY
  (ITEM_ID integer NOT NULL,
  ITEM_NAME varchar(20),
  SUP_ID int,
  QUANTITY int,
  DATE_VAL timestamp,
  PRIMARY KEY (ITEM_ID),
  FOREIGN KEY (SUP_ID) REFERENCES SUPPLIER (ID));
  
create table COFFEE_HOUSE
  (ID integer NOT NULL,
  CITY varchar(32),
  LOCATION geometry,
  COFFEE int NOT NULL,
  MERCH int NOT NULL,
  TOTAL int NOT NULL,
  PRIMARY KEY (ID));
  
create table DATA_REPOSITORY
  (DOCUMENT_NAME varchar(50),
  URL varchar(200));