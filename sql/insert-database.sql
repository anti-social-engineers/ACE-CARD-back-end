---
--- [ADDRESSES]
---

INSERT INTO addresses (id, address, address_num, city, postalcode, country)
VALUES (1, 'Schermerhornstraat', 25, 'Rotterdam', '3066TG', 'NLD'),
(2, 'Wijnhaven', 107, 'Rotterdam', '3011WN', 'NLD'),
(3, 'Geerhoek', 167, 'Wouw', '4724EG', 'NLD'),
(4, 'Edisonstraat', 145, 'Wijchen', '6604BV', 'NLD');


---
--- [USERS]
--- password: hogeschool123
---
INSERT INTO users (id, email, password, password_salt, is_email_verified, role)
VALUES ('a2b19f94-4fb1-48d3-9e35-b6e250979d5c', 'aaron.beetstra@outlook.com', '0A9788E54B409D6C9D0283A4FDE01DDCB2C3683BED0146F3FA03869C5000C16485CB0654227B79467940104AAD6615EE7838D022125C9E7B88EF12C18667E7F4', '16699C320C8E3765044AC7911471437C0D74BB463CBB3202138336A26777152E', true, 'sysop'),
('e2005006-2d8a-4f6f-b557-f16da75519bf', 'party-goer@ase.com', '1C047E52BF2E862FD2D6B1AA30048950ACCF04F7B33429F490D45513A1A849890C622A220B49C92FA7FA7FBE560E809530B3407A6E84429FD49B25E6B561C046', '47E500D81774256B5A7D16423CF275C09E061963C1AB0A55388793588B98912F', true, 'user'),
('4f8c9c3c-c91d-4db0-9f8b-b8a6eb9dcc97', 'owner@awsomeclub.nl', '1C047E52BF2E862FD2D6B1AA30048950ACCF04F7B33429F490D45513A1A849890C622A220B49C92FA7FA7FBE560E809530B3407A6E84429FD49B25E6B561C046', '47E500D81774256B5A7D16423CF275C09E061963C1AB0A55388793588B98912F', true, 'clubowner');

---
--- [CARD]
--- Ensure ID is an actual id which corresponds with a id on a card
---
INSERT INTO cards (id, is_activated, credits, is_blocked, user_id_id, requested_date)
VALUES ('randomCardSecretID', true, 19.99, false, 'e2005006-2d8a-4f6f-b557-f16da75519bf', '2019-06-03 08:23:54+02:00');


---
--- [CLUB]
---
INSERT INTO clubs (id, min_age, club_address_id, owner_id, club_name)
VALUES ('2d466140-f70b-4ac3-8156-ee922657bacd', 18, 2, '4f8c9c3c-c91d-4db0-9f8b-b8a6eb9dcc97', 'ClubAwsome');


---
--- [USER]
--- Update user to have address and additional data
---
UPDATE users
SET first_name = 'Aaron', last_name = 'Beetstra', gender = 'Man', date_of_birth = '1999-01-18', address_id = 1
WHERE id = 'a2b19f94-4fb1-48d3-9e35-b6e250979d5c';

UPDATE users
SET first_name = 'Selim', last_name = 'Aydi', gender = 'Man', date_of_birth = '1997-01-1', address_id = 4
WHERE id = 'e2005006-2d8a-4f6f-b557-f16da75519bf';

UPDATE users
SET first_name = 'Mr.', last_name = 'Awsome', gender = 'Man', date_of_birth = '1980-08-16', address_id = 3
WHERE id = '4f8c9c3c-c91d-4db0-9f8b-b8a6eb9dcc97';


---
--- [PENALTIES]
---
INSERT INTO penalties (id, date_received, handed_out_by_id, received_at_id, recipient_id_id, description)
VALUES (1, '2019-05-16', '4f8c9c3c-c91d-4db0-9f8b-b8a6eb9dcc97', '2d466140-f70b-4ac3-8156-ee922657bacd', 'e2005006-2d8a-4f6f-b557-f16da75519bf', 'Verkocht coke');
