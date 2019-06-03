--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.1
-- Dumped by pg_dump version 9.6.1

-- Started on 2019-06-03 13:20:26

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
-- TOC entry 2208 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 188 (class 1259 OID 71734)
-- Name: addresses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE addresses (
    id integer NOT NULL,
    address character varying(255) NOT NULL,
    address_num integer NOT NULL,
    city character varying(255) NOT NULL,
    postalcode character varying(6) NOT NULL,
    country character varying(5) NOT NULL
);


ALTER TABLE addresses OWNER TO postgres;

--
-- TOC entry 187 (class 1259 OID 71732)
-- Name: addresses_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE addresses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE addresses_id_seq OWNER TO postgres;

--
-- TOC entry 2209 (class 0 OID 0)
-- Dependencies: 187
-- Name: addresses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE addresses_id_seq OWNED BY addresses.id;


--
-- TOC entry 189 (class 1259 OID 71743)
-- Name: cards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE cards (
    id uuid NOT NULL,
    card_code character varying(255) NOT NULL,
    is_activated boolean NOT NULL,
    credits numeric(10,2) NOT NULL,
    is_blocked boolean NOT NULL,
    requested_at timestamp with time zone NOT NULL,
    activated_at timestamp with time zone,
    user_id_id uuid NOT NULL
);


ALTER TABLE cards OWNER TO postgres;

--
-- TOC entry 190 (class 1259 OID 71748)
-- Name: clubs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE clubs (
    id uuid NOT NULL,
    min_age integer NOT NULL,
    club_name character varying(255) NOT NULL,
    club_address_id integer NOT NULL,
    owner_id uuid NOT NULL
);


ALTER TABLE clubs OWNER TO postgres;

--
-- TOC entry 197 (class 1259 OID 71782)
-- Name: deposits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE deposits (
    id integer NOT NULL,
    amount numeric(10,2) NOT NULL,
    deposited_at timestamp with time zone NOT NULL,
    card_id_id uuid NOT NULL
);


ALTER TABLE deposits OWNER TO postgres;

--
-- TOC entry 196 (class 1259 OID 71780)
-- Name: deposits_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE deposits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE deposits_id_seq OWNER TO postgres;

--
-- TOC entry 2210 (class 0 OID 0)
-- Dependencies: 196
-- Name: deposits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE deposits_id_seq OWNED BY deposits.id;


--
-- TOC entry 186 (class 1259 OID 71723)
-- Name: django_migrations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE django_migrations (
    id integer NOT NULL,
    app character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    applied timestamp with time zone NOT NULL
);


ALTER TABLE django_migrations OWNER TO postgres;

--
-- TOC entry 185 (class 1259 OID 71721)
-- Name: django_migrations_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE django_migrations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE django_migrations_id_seq OWNER TO postgres;

--
-- TOC entry 2211 (class 0 OID 0)
-- Dependencies: 185
-- Name: django_migrations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE django_migrations_id_seq OWNED BY django_migrations.id;


--
-- TOC entry 195 (class 1259 OID 71774)
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE payments (
    id integer NOT NULL,
    amount numeric(10,2) NOT NULL,
    paid_at timestamp with time zone NOT NULL,
    card_id_id uuid NOT NULL,
    club_id uuid NOT NULL
);


ALTER TABLE payments OWNER TO postgres;

--
-- TOC entry 194 (class 1259 OID 71772)
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE payments_id_seq OWNER TO postgres;

--
-- TOC entry 2212 (class 0 OID 0)
-- Dependencies: 194
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE payments_id_seq OWNED BY payments.id;


--
-- TOC entry 193 (class 1259 OID 71763)
-- Name: penalties; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE penalties (
    id integer NOT NULL,
    date_received date NOT NULL,
    description text NOT NULL,
    handed_out_by_id uuid,
    received_at_id uuid NOT NULL,
    recipient_id_id uuid NOT NULL
);


ALTER TABLE penalties OWNER TO postgres;

--
-- TOC entry 192 (class 1259 OID 71761)
-- Name: penalties_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE penalties_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE penalties_id_seq OWNER TO postgres;

--
-- TOC entry 2213 (class 0 OID 0)
-- Dependencies: 192
-- Name: penalties_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE penalties_id_seq OWNED BY penalties.id;


--
-- TOC entry 191 (class 1259 OID 71753)
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
    is_email_verified boolean NOT NULL,
    role character varying(15) NOT NULL,
    address_id integer
);


ALTER TABLE users OWNER TO postgres;

--
-- TOC entry 2042 (class 2604 OID 71737)
-- Name: addresses id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY addresses ALTER COLUMN id SET DEFAULT nextval('addresses_id_seq'::regclass);


--
-- TOC entry 2045 (class 2604 OID 71785)
-- Name: deposits id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY deposits ALTER COLUMN id SET DEFAULT nextval('deposits_id_seq'::regclass);


--
-- TOC entry 2041 (class 2604 OID 71726)
-- Name: django_migrations id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY django_migrations ALTER COLUMN id SET DEFAULT nextval('django_migrations_id_seq'::regclass);


--
-- TOC entry 2044 (class 2604 OID 71777)
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments ALTER COLUMN id SET DEFAULT nextval('payments_id_seq'::regclass);


--
-- TOC entry 2043 (class 2604 OID 71766)
-- Name: penalties id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties ALTER COLUMN id SET DEFAULT nextval('penalties_id_seq'::regclass);


--
-- TOC entry 2049 (class 2606 OID 71789)
-- Name: addresses addresses_address_address_num_city_18b2518a_uniq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY addresses
    ADD CONSTRAINT addresses_address_address_num_city_18b2518a_uniq UNIQUE (address, address_num, city, postalcode, country);


--
-- TOC entry 2051 (class 2606 OID 71742)
-- Name: addresses addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY addresses
    ADD CONSTRAINT addresses_pkey PRIMARY KEY (id);


--
-- TOC entry 2053 (class 2606 OID 71747)
-- Name: cards cards_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cards
    ADD CONSTRAINT cards_pkey PRIMARY KEY (id);


--
-- TOC entry 2058 (class 2606 OID 71752)
-- Name: clubs clubs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT clubs_pkey PRIMARY KEY (id);


--
-- TOC entry 2075 (class 2606 OID 71787)
-- Name: deposits deposits_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY deposits
    ADD CONSTRAINT deposits_pkey PRIMARY KEY (id);


--
-- TOC entry 2047 (class 2606 OID 71731)
-- Name: django_migrations django_migrations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY django_migrations
    ADD CONSTRAINT django_migrations_pkey PRIMARY KEY (id);


--
-- TOC entry 2072 (class 2606 OID 71779)
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- TOC entry 2066 (class 2606 OID 71771)
-- Name: penalties penalties_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalties_pkey PRIMARY KEY (id);


--
-- TOC entry 2061 (class 2606 OID 71802)
-- Name: users users_first_name_last_name_date_of_birth_0ff19396_uniq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_first_name_last_name_date_of_birth_0ff19396_uniq UNIQUE (first_name, last_name, date_of_birth);


--
-- TOC entry 2063 (class 2606 OID 71760)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 2054 (class 1259 OID 71846)
-- Name: cards_user_id_id_9325af68; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX cards_user_id_id_9325af68 ON cards USING btree (user_id_id);


--
-- TOC entry 2055 (class 1259 OID 71795)
-- Name: clubs_club_address_id_5d4314c9; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX clubs_club_address_id_5d4314c9 ON clubs USING btree (club_address_id);


--
-- TOC entry 2056 (class 1259 OID 71840)
-- Name: clubs_owner_id_2db8d5b5; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX clubs_owner_id_2db8d5b5 ON clubs USING btree (owner_id);


--
-- TOC entry 2073 (class 1259 OID 71839)
-- Name: deposits_card_id_id_6d01cdd0; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX deposits_card_id_id_6d01cdd0 ON deposits USING btree (card_id_id);


--
-- TOC entry 2069 (class 1259 OID 71832)
-- Name: payments_card_id_id_e9ee5c9d; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX payments_card_id_id_e9ee5c9d ON payments USING btree (card_id_id);


--
-- TOC entry 2070 (class 1259 OID 71833)
-- Name: payments_club_id_6ab8b231; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX payments_club_id_6ab8b231 ON payments USING btree (club_id);


--
-- TOC entry 2064 (class 1259 OID 71819)
-- Name: penalties_handed_out_by_id_11a76981; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalties_handed_out_by_id_11a76981 ON penalties USING btree (handed_out_by_id);


--
-- TOC entry 2067 (class 1259 OID 71820)
-- Name: penalties_received_at_id_41faf01e; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalties_received_at_id_41faf01e ON penalties USING btree (received_at_id);


--
-- TOC entry 2068 (class 1259 OID 71821)
-- Name: penalties_recipient_id_id_dbab4a76; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX penalties_recipient_id_id_dbab4a76 ON penalties USING btree (recipient_id_id);


--
-- TOC entry 2059 (class 1259 OID 71803)
-- Name: users_address_id_96e92564; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX users_address_id_96e92564 ON users USING btree (address_id);


--
-- TOC entry 2076 (class 2606 OID 71847)
-- Name: cards cards_user_id_id_9325af68_fk_users_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY cards
    ADD CONSTRAINT cards_user_id_id_9325af68_fk_users_id FOREIGN KEY (user_id_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2077 (class 2606 OID 71790)
-- Name: clubs clubs_club_address_id_5d4314c9_fk_addresses_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT clubs_club_address_id_5d4314c9_fk_addresses_id FOREIGN KEY (club_address_id) REFERENCES addresses(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2078 (class 2606 OID 71841)
-- Name: clubs clubs_owner_id_2db8d5b5_fk_users_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY clubs
    ADD CONSTRAINT clubs_owner_id_2db8d5b5_fk_users_id FOREIGN KEY (owner_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2085 (class 2606 OID 71834)
-- Name: deposits deposits_card_id_id_6d01cdd0_fk_cards_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY deposits
    ADD CONSTRAINT deposits_card_id_id_6d01cdd0_fk_cards_id FOREIGN KEY (card_id_id) REFERENCES cards(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2083 (class 2606 OID 71822)
-- Name: payments payments_card_id_id_e9ee5c9d_fk_cards_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payments_card_id_id_e9ee5c9d_fk_cards_id FOREIGN KEY (card_id_id) REFERENCES cards(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2084 (class 2606 OID 71827)
-- Name: payments payments_club_id_6ab8b231_fk_clubs_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payments
    ADD CONSTRAINT payments_club_id_6ab8b231_fk_clubs_id FOREIGN KEY (club_id) REFERENCES clubs(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2080 (class 2606 OID 71804)
-- Name: penalties penalties_handed_out_by_id_11a76981_fk_users_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalties_handed_out_by_id_11a76981_fk_users_id FOREIGN KEY (handed_out_by_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2081 (class 2606 OID 71809)
-- Name: penalties penalties_received_at_id_41faf01e_fk_clubs_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalties_received_at_id_41faf01e_fk_clubs_id FOREIGN KEY (received_at_id) REFERENCES clubs(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2082 (class 2606 OID 71814)
-- Name: penalties penalties_recipient_id_id_dbab4a76_fk_users_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY penalties
    ADD CONSTRAINT penalties_recipient_id_id_dbab4a76_fk_users_id FOREIGN KEY (recipient_id_id) REFERENCES users(id) DEFERRABLE INITIALLY DEFERRED;


--
-- TOC entry 2079 (class 2606 OID 71796)
-- Name: users users_address_id_96e92564_fk_addresses_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_address_id_96e92564_fk_addresses_id FOREIGN KEY (address_id) REFERENCES addresses(id) DEFERRABLE INITIALLY DEFERRED;


-- Completed on 2019-06-03 13:20:26

--
-- PostgreSQL database dump complete
--

