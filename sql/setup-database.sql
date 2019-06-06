--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.1
-- Dumped by pg_dump version 9.6.1

-- Started on 2019-06-04 11:26:22

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 1 (class 3079 OID 12387)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2192 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 185 (class 1259 OID 72542)
-- Name: addresses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE addresses (
    id uuid NOT NULL,
    address character varying(255) NOT NULL,
    address_num integer NOT NULL,
    address_annex character varying(1),
    city character varying(255) NOT NULL,
    postalcode character varying(6) NOT NULL,
    country character varying(5) NOT NULL
);


ALTER TABLE addresses OWNER TO postgres;

--
-- TOC entry 186 (class 1259 OID 72550)
-- Name: cards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE cards (
    id uuid NOT NULL,
    card_code character varying(255),
    is_activated boolean DEFAULT false NOT NULL,
    credits numeric(10,2) default 0 NOT NULL,
    is_blocked boolean DEFAULT false NOT NULL,
    requested_at timestamp with time zone NOT NULL,
    activated_at timestamp with time zone,
    user_id_id uuid NOT NULL
);


ALTER TABLE cards OWNER TO postgres;

--
-- TOC entry 187 (class 1259 OID 72557)
-- Name: clubs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE clubs (
    id uuid NOT NULL,
    min_age integer NOT NULL,
    club_name character varying(255) NOT NULL,
    club_address_id uuid NOT NULL,
    owner_id uuid NOT NULL
);


ALTER TABLE clubs OWNER TO postgres;

--
-- TOC entry 191 (class 1259 OID 72585)
-- Name: deposits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE deposits (
    id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    deposited_at timestamp with time zone NOT NULL,
    card_id_id uuid NOT NULL
);


ALTER TABLE deposits OWNER TO postgres;

--
-- TOC entry 190 (class 1259 OID 72580)
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE payments (
    id uuid NOT NULL,
    amount numeric(10,2) NOT NULL,
    paid_at timestamp with time zone NOT NULL,
    card_id_id uuid NOT NULL,
    club_id uuid NOT NULL
);


ALTER TABLE payments OWNER TO postgres;

--
-- TOC entry 189 (class 1259 OID 72572)
-- Name: penalties; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE penalties (
    id uuid NOT NULL,
    date_received date NOT NULL,
    description text NOT NULL,
    handed_out_by_id uuid,
    received_at_id uuid NOT NULL,
    recipient_id_id uuid NOT NULL
);


ALTER TABLE penalties OWNER TO postgres;

--
-- TOC entry 188 (class 1259 OID 72562)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE users (
    id uuid NOT NULL,
    email character varying(254) NOT NULL,
    password character varying(255) NOT NULL,
    password_salt character varying(255) NOT NULL,
    first_name character varying(255),
    last_name character varying(255),
    gender character varying(15),
    date_of_birth date,
    is_email_verified boolean DEFAULT false NOT NULL,
    role character varying(15) DEFAULT 'user' NOT NULL,
    image_id uuid,
    address_id uuid
);


ALTER TABLE users OWNER TO postgres;

--
-- TOC entry 2027 (class 2606 OID 72591)
-- Name: addresses addresses_address_address_num_city_18b2518a_uniq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY addresses
    ADD CONSTRAINT addresses_address_address_num_city_18b2518a_uniq UNIQUE (address, address_num, city, postalcode, country);


--
-- TOC entry 2029 (class 2606 OID 72549)
-- Name: addresses addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY addresses
    ADD CONSTRAINT addresses_pkey PRIMARY KEY (id);


--
-- TOC entry 2032 (class 2606 OID 72556)
-- Name: cards cards_card_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cards
    ADD CONSTRAINT cards_card_code_key UNIQUE (card_code);


--
-- TOC entry 2034 (class 2606 OID 72554)
-- Name: cards cards_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cards
    ADD CONSTRAINT cards_pkey PRIMARY KEY (id);


--
-- TOC entry 2039 (class 2606 OID 72561)
-- Name: clubs clubs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT clubs_pkey PRIMARY KEY (id);


--
-- TOC entry 2059 (class 2606 OID 72589)
-- Name: deposits deposits_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY deposits
    ADD CONSTRAINT deposits_pkey PRIMARY KEY (id);


--
-- TOC entry 2056 (class 2606 OID 72584)
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- TOC entry 2050 (class 2606 OID 72579)
-- Name: penalties penalties_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalties_pkey PRIMARY KEY (id);


--
-- TOC entry 2043 (class 2606 OID 72571)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 2047 (class 2606 OID 72569)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 2030 (class 1259 OID 72592)
-- Name: cards_card_code_5bdcceef_like; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX cards_card_code_5bdcceef_like ON cards USING btree (card_code varchar_pattern_ops);


--
-- TOC entry 2035 (class 1259 OID 72650)
-- Name: cards_user_id_id_9325af68; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX cards_user_id_id_9325af68 ON cards USING btree (user_id_id);


--
-- TOC entry 2036 (class 1259 OID 72598)
-- Name: clubs_club_address_id_5d4314c9; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX clubs_club_address_id_5d4314c9 ON clubs USING btree (club_address_id);


--
-- TOC entry 2037 (class 1259 OID 72644)
-- Name: clubs_owner_id_2db8d5b5; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX clubs_owner_id_2db8d5b5 ON clubs USING btree (owner_id);


--
-- TOC entry 2057 (class 1259 OID 72643)
-- Name: deposits_card_id_id_6d01cdd0; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX deposits_card_id_id_6d01cdd0 ON deposits USING btree (card_id_id);


--
-- TOC entry 2053 (class 1259 OID 72636)
-- Name: payments_card_id_id_e9ee5c9d; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX payments_card_id_id_e9ee5c9d ON payments USING btree (card_id_id);


--
-- TOC entry 2054 (class 1259 OID 72637)
-- Name: payments_club_id_6ab8b231; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX payments_club_id_6ab8b231 ON payments USING btree (club_id);


--
-- TOC entry 2048 (class 1259 OID 72623)
-- Name: penalties_handed_out_by_id_11a76981; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalties_handed_out_by_id_11a76981 ON penalties USING btree (handed_out_by_id);


--
-- TOC entry 2051 (class 1259 OID 72624)
-- Name: penalties_received_at_id_41faf01e; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalties_received_at_id_41faf01e ON penalties USING btree (received_at_id);


--
-- TOC entry 2052 (class 1259 OID 72625)
-- Name: penalties_recipient_id_id_dbab4a76; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalties_recipient_id_id_dbab4a76 ON penalties USING btree (recipient_id_id);


--
-- TOC entry 2040 (class 1259 OID 72607)
-- Name: users_address_id_96e92564; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX users_address_id_96e92564 ON users USING btree (address_id);


--
-- TOC entry 2041 (class 1259 OID 72606)
-- Name: users_email_0ea73cca_like; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX users_email_0ea73cca_like ON users USING btree (email varchar_pattern_ops);


--
-- TOC entry 2060 (class 2606 OID 72651)
-- Name: cards cards_user_id_id_9325af68_fk_users_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cards
    ADD CONSTRAINT cards_user_id_id_9325af68_fk_users_id FOREIGN KEY (user_id_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2061 (class 2606 OID 72593)
-- Name: clubs clubs_club_address_id_5d4314c9_fk_addresses_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT clubs_club_address_id_5d4314c9_fk_addresses_id FOREIGN KEY (club_address_id) REFERENCES addresses(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2062 (class 2606 OID 72645)
-- Name: clubs clubs_owner_id_2db8d5b5_fk_users_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT clubs_owner_id_2db8d5b5_fk_users_id FOREIGN KEY (owner_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2069 (class 2606 OID 72638)
-- Name: deposits deposits_card_id_id_6d01cdd0_fk_cards_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY deposits
    ADD CONSTRAINT deposits_card_id_id_6d01cdd0_fk_cards_id FOREIGN KEY (card_id_id) REFERENCES cards(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2067 (class 2606 OID 72626)
-- Name: payments payments_card_id_id_e9ee5c9d_fk_cards_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payments_card_id_id_e9ee5c9d_fk_cards_id FOREIGN KEY (card_id_id) REFERENCES cards(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2068 (class 2606 OID 72631)
-- Name: payments payments_club_id_6ab8b231_fk_clubs_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payments_club_id_6ab8b231_fk_clubs_id FOREIGN KEY (club_id) REFERENCES clubs(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2064 (class 2606 OID 72608)
-- Name: penalties penalties_handed_out_by_id_11a76981_fk_users_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalties_handed_out_by_id_11a76981_fk_users_id FOREIGN KEY (handed_out_by_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2065 (class 2606 OID 72613)
-- Name: penalties penalties_received_at_id_41faf01e_fk_clubs_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalties_received_at_id_41faf01e_fk_clubs_id FOREIGN KEY (received_at_id) REFERENCES clubs(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2066 (class 2606 OID 72618)
-- Name: penalties penalties_recipient_id_id_dbab4a76_fk_users_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalties_recipient_id_id_dbab4a76_fk_users_id FOREIGN KEY (recipient_id_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2063 (class 2606 OID 72599)
-- Name: users users_address_id_96e92564_fk_addresses_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_address_id_96e92564_fk_addresses_id FOREIGN KEY (address_id) REFERENCES addresses(id) DEFERRABLE INITIALLY DEFERRED;


-- Completed on 2019-06-04 11:26:22

--
-- PostgreSQL database dump complete
--

