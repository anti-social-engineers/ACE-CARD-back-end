--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.1
-- Dumped by pg_dump version 9.6.1

-- Started on 2019-05-16 14:41:43

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
-- TOC entry 2206 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 186 (class 1259 OID 70083)
-- Name: addresses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE addresses (
    id integer NOT NULL,
    address character varying(255) NOT NULL,
    city character varying(255) NOT NULL,
    postalcode character varying(6) NOT NULL,
    country character varying(5) NOT NULL,
    address_num integer NOT NULL
);


ALTER TABLE addresses OWNER TO postgres;

--
-- TOC entry 185 (class 1259 OID 70081)
-- Name: address_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE address_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE address_id_seq OWNER TO postgres;

--
-- TOC entry 2207 (class 0 OID 0)
-- Dependencies: 185
-- Name: address_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE address_id_seq OWNED BY addresses.id;


--
-- TOC entry 189 (class 1259 OID 70117)
-- Name: cards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE cards (
    id character varying(255) NOT NULL,
    is_activated boolean NOT NULL,
    credits numeric(10,2) NOT NULL,
    is_blocked boolean NOT NULL,
    user_id_id uuid NOT NULL
);


ALTER TABLE cards OWNER TO postgres;

--
-- TOC entry 190 (class 1259 OID 70129)
-- Name: clubs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE clubs (
    id uuid NOT NULL,
    min_age integer NOT NULL,
    club_address_id integer NOT NULL,
    owner_id uuid NOT NULL,
    club_name character varying(255) NOT NULL
);


ALTER TABLE clubs OWNER TO postgres;

--
-- TOC entry 194 (class 1259 OID 70149)
-- Name: deposits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE deposits (
    id integer NOT NULL,
    amount numeric(10,2) NOT NULL,
    card_id_id character varying(255) NOT NULL,
    deposit_date timestamp NOT NULL
);


ALTER TABLE deposits OWNER TO postgres;

--
-- TOC entry 193 (class 1259 OID 70147)
-- Name: deposit_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE deposit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE deposit_id_seq OWNER TO postgres;

--
-- TOC entry 2208 (class 0 OID 0)
-- Dependencies: 193
-- Name: deposit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE deposit_id_seq OWNED BY deposits.id;


--
-- TOC entry 188 (class 1259 OID 70106)
-- Name: email_activations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE email_activations (
    token uuid NOT NULL,
    first_send timestamp with time zone NOT NULL,
    amount_send integer NOT NULL,
    user_id_id uuid NOT NULL
);


ALTER TABLE email_activations OWNER TO postgres;

--
-- TOC entry 192 (class 1259 OID 70141)
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE payments (
    id integer NOT NULL,
    amount numeric(10,2) NOT NULL,
    card_id_id character varying(255) NOT NULL,
    club_id uuid NOT NULL,
	payment_date timestamp NOT NULL
);


ALTER TABLE payments OWNER TO postgres;

--
-- TOC entry 191 (class 1259 OID 70139)
-- Name: payment_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE payment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE payment_id_seq OWNER TO postgres;

--
-- TOC entry 2209 (class 0 OID 0)
-- Dependencies: 191
-- Name: payment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE payment_id_seq OWNED BY payments.id;


--
-- TOC entry 196 (class 1259 OID 70199)
-- Name: penalties; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE penalties (
    id integer NOT NULL,
    date_received date NOT NULL,
    handed_out_by_id uuid,
    received_at_id uuid NOT NULL,
    recipient_id_id uuid NOT NULL,
    description text NOT NULL
);


ALTER TABLE penalties OWNER TO postgres;

--
-- TOC entry 195 (class 1259 OID 70197)
-- Name: penalty_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE penalty_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE penalty_id_seq OWNER TO postgres;

--
-- TOC entry 2210 (class 0 OID 0)
-- Dependencies: 195
-- Name: penalty_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE penalty_id_seq OWNED BY penalties.id;


--
-- TOC entry 187 (class 1259 OID 70092)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE users (
    id uuid NOT NULL,
    email character varying(254) NOT NULL UNIQUE,
    password character varying(255) NOT NULL,
    password_salt character varying(255) NOT NULL,
    first_name character varying(255),
    last_name character varying(255),
    gender character varying(15),
    date_of_birth date,
    is_email_verified boolean DEFAULT false NOT NULL,
    role character varying(15) DEFAULT 'user' NOT NULL,
    address_id integer
);


ALTER TABLE users OWNER TO postgres;

--
-- TOC entry 2038 (class 2604 OID 70086)
-- Name: addresses id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY addresses ALTER COLUMN id SET DEFAULT nextval('address_id_seq'::regclass);


--
-- TOC entry 2040 (class 2604 OID 70152)
-- Name: deposits id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY deposits ALTER COLUMN id SET DEFAULT nextval('deposit_id_seq'::regclass);


--
-- TOC entry 2039 (class 2604 OID 70144)
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments ALTER COLUMN id SET DEFAULT nextval('payment_id_seq'::regclass);


--
-- TOC entry 2041 (class 2604 OID 70202)
-- Name: penalties id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties ALTER COLUMN id SET DEFAULT nextval('penalty_id_seq'::regclass);


--
-- TOC entry 2043 (class 2606 OID 70091)
-- Name: addresses address_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY addresses
    ADD CONSTRAINT address_pkey PRIMARY KEY (id);


--
-- TOC entry 2057 (class 2606 OID 70133)
-- Name: clubs club_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT club_pkey PRIMARY KEY (id);


--
-- TOC entry 2052 (class 2606 OID 70121)
-- Name: cards d_card_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cards
    ADD CONSTRAINT d_card_pkey PRIMARY KEY (id);


--
-- TOC entry 2048 (class 2606 OID 70110)
-- Name: email_activations d_emailactivation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY email_activations
    ADD CONSTRAINT d_emailactivation_pkey PRIMARY KEY (token);


--
-- TOC entry 2066 (class 2606 OID 70154)
-- Name: deposits deposit_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY deposits
    ADD CONSTRAINT deposit_pkey PRIMARY KEY (id);


--
-- TOC entry 2062 (class 2606 OID 70146)
-- Name: payments payment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payment_pkey PRIMARY KEY (id);


--
-- TOC entry 2069 (class 2606 OID 70204)
-- Name: penalties penalty_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalty_pkey PRIMARY KEY (id);


--
-- TOC entry 2046 (class 2606 OID 70099)
-- Name: users user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);


--
-- TOC entry 2054 (class 1259 OID 70160)
-- Name: club_club_address_id_fc4ef2fa; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX club_club_address_id_fc4ef2fa ON clubs USING btree (club_address_id);


--
-- TOC entry 2055 (class 1259 OID 70181)
-- Name: club_owner_id_a1d242eb; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX club_owner_id_a1d242eb ON clubs USING btree (owner_id);


--
-- TOC entry 2050 (class 1259 OID 70127)
-- Name: d_card_id_39d5c94f_like; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX d_card_id_39d5c94f_like ON cards USING btree (id varchar_pattern_ops);


--
-- TOC entry 2053 (class 1259 OID 70128)
-- Name: d_card_user_id_id_2f613386; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX d_card_user_id_id_2f613386 ON cards USING btree (user_id_id);


--
-- TOC entry 2049 (class 1259 OID 70116)
-- Name: d_emailactivation_user_id_id_816f14d1; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX d_emailactivation_user_id_id_816f14d1 ON email_activations USING btree (user_id_id);


--
-- TOC entry 2063 (class 1259 OID 70179)
-- Name: deposit_card_id_id_50828cd4; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX deposit_card_id_id_50828cd4 ON deposits USING btree (card_id_id);


--
-- TOC entry 2064 (class 1259 OID 70180)
-- Name: deposit_card_id_id_50828cd4_like; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX deposit_card_id_id_50828cd4_like ON deposits USING btree (card_id_id varchar_pattern_ops);


--
-- TOC entry 2058 (class 1259 OID 70171)
-- Name: payment_card_id_id_17098bcb; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX payment_card_id_id_17098bcb ON payments USING btree (card_id_id);


--
-- TOC entry 2059 (class 1259 OID 70172)
-- Name: payment_card_id_id_17098bcb_like; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX payment_card_id_id_17098bcb_like ON payments USING btree (card_id_id varchar_pattern_ops);


--
-- TOC entry 2060 (class 1259 OID 70173)
-- Name: payment_club_id_d9008460; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX payment_club_id_d9008460 ON payments USING btree (club_id);


--
-- TOC entry 2067 (class 1259 OID 70220)
-- Name: penalty_handed_out_by_id_efd49643; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalty_handed_out_by_id_efd49643 ON penalties USING btree (handed_out_by_id);


--
-- TOC entry 2070 (class 1259 OID 70221)
-- Name: penalty_received_at_id_23927f67; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalty_received_at_id_23927f67 ON penalties USING btree (received_at_id);


--
-- TOC entry 2071 (class 1259 OID 70222)
-- Name: penalty_recipient_id_id_2164a1da; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalty_recipient_id_id_2164a1da ON penalties USING btree (recipient_id_id);


--
-- TOC entry 2044 (class 1259 OID 70105)
-- Name: user_address_id_d8fffd8c; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX user_address_id_d8fffd8c ON users USING btree (address_id);


--
-- TOC entry 2075 (class 2606 OID 70182)
-- Name: clubs club_owner_id_a1d242eb_fk_user_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT club_owner_id_a1d242eb_fk_user_id FOREIGN KEY (owner_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2076 (class 2606 OID 70273)
-- Name: clubs clubs_club_address_id_5d4314c9_fk_addresses_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT clubs_club_address_id_5d4314c9_fk_addresses_id FOREIGN KEY (club_address_id) REFERENCES addresses(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2074 (class 2606 OID 70122)
-- Name: cards d_card_user_id_id_2f613386_fk_user_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cards
    ADD CONSTRAINT d_card_user_id_id_2f613386_fk_user_id FOREIGN KEY (user_id_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2073 (class 2606 OID 70111)
-- Name: email_activations d_emailactivation_user_id_id_816f14d1_fk_user_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY email_activations
    ADD CONSTRAINT d_emailactivation_user_id_id_816f14d1_fk_user_id FOREIGN KEY (user_id_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2079 (class 2606 OID 70174)
-- Name: deposits deposit_card_id_id_50828cd4_fk_card_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY deposits
    ADD CONSTRAINT deposit_card_id_id_50828cd4_fk_card_id FOREIGN KEY (card_id_id) REFERENCES cards(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2077 (class 2606 OID 70161)
-- Name: payments payment_card_id_id_17098bcb_fk_card_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payment_card_id_id_17098bcb_fk_card_id FOREIGN KEY (card_id_id) REFERENCES cards(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2078 (class 2606 OID 70166)
-- Name: payments payment_club_id_d9008460_fk_club_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payment_club_id_d9008460_fk_club_id FOREIGN KEY (club_id) REFERENCES clubs(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2080 (class 2606 OID 70205)
-- Name: penalties penalty_handed_out_by_id_efd49643_fk_user_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalty_handed_out_by_id_efd49643_fk_user_id FOREIGN KEY (handed_out_by_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2081 (class 2606 OID 70210)
-- Name: penalties penalty_received_at_id_23927f67_fk_club_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalty_received_at_id_23927f67_fk_club_id FOREIGN KEY (received_at_id) REFERENCES clubs(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2082 (class 2606 OID 70215)
-- Name: penalties penalty_recipient_id_id_2164a1da_fk_user_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalty_recipient_id_id_2164a1da_fk_user_id FOREIGN KEY (recipient_id_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2072 (class 2606 OID 70278)
-- Name: users users_address_id_96e92564_fk_addresses_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_address_id_96e92564_fk_addresses_id FOREIGN KEY (address_id) REFERENCES addresses(id) DEFERRABLE INITIALLY DEFERRED;


-- Completed on 2019-05-16 14:41:43

--
-- PostgreSQL database dump complete
--

