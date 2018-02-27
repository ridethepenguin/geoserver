-- Schema

DROP SCHEMA IF EXISTS hale_stations CASCADE;
CREATE SCHEMA hale_stations AUTHORIZATION geosolutions;

SET search_path TO hale_stations, public;

-- Stations

DROP TABLE IF EXISTS stations_gml32 CASCADE;
CREATE TABLE stations_gml32 (
	id varchar(10) primary key,
	name varchar(50),
	phone varchar(50),
	mail varchar(50),
	location geometry(Point, 4326)
);

INSERT INTO stations_gml32
	(id, name, phone, mail, location)
VALUES
	('st.1', 'station1', '32154898', 'station1@stations.org', ST_SetSRID(ST_GeomFromText('POINT(8.63 44.92)'), 4326));


-- Observations

DROP TABLE IF EXISTS observations_gml32 CASCADE;
CREATE TABLE observations_gml32 (
	id varchar(50) primary key,
	name varchar(50) not null,
	unit varchar(10) not null,
	value double precision not null,
	station_id varchar(10) not null,
	CONSTRAINT fk_station FOREIGN KEY (station_id) REFERENCES stations_gml32 (id)
);

INSERT INTO observations_gml32
	(id, name, unit, value, station_id)
VALUES
	('ms.1', 'temperature', 'c', 21.0, 'st.1'),
	('ms.2', 'wind', 'km/h', 75.6, 'st.1');
