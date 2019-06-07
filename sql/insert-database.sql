---
--- [ADDRESSES]
---

INSERT INTO addresses (id, address, address_num, city, postalcode, country)
VALUES ('7372a47b-9629-4037-aae4-e7494979b3c4', 'Schermerhornstraat', 25, 'Rotterdam', '3066TG', 'NLD'),
('1c7ef92f-5424-438f-84f4-ba0158ad06e4', 'Wijnhaven', 107, 'Rotterdam', '3011WN', 'NLD'),
('48812838-57c7-4289-86cd-f40a132d61b5', 'Geerhoek', 167, 'Wouw', '4724EG', 'NLD'),
('df0eaa27-c448-41e5-a5e6-e4e021d03e9f', 'Peppelbos', 90, 'Geldermalsen', '4191ME', 'NLD'),
('5b214aa6-4fb6-4eca-aefa-62a4568be445', 'Edisonstraat', 145, 'Wijchen', '6604BV', 'NLD');


---
--- [USERS]
--- password: helloworld123
---
INSERT INTO users (id, email, password, password_salt, is_email_verified, role)
VALUES ('a2b19f94-4fb1-48d3-9e35-b6e250979d5c', 'aaron.beetstra@outlook.com', '0A9788E54B409D6C9D0283A4FDE01DDCB2C3683BED0146F3FA03869C5000C16485CB0654227B79467940104AAD6615EE7838D022125C9E7B88EF12C18667E7F4', '16699C320C8E3765044AC7911471437C0D74BB463CBB3202138336A26777152E', true, 'sysop'),
('e2005006-2d8a-4f6f-b557-f16da75519bf', 'party-goer@ase.com', '0A9788E54B409D6C9D0283A4FDE01DDCB2C3683BED0146F3FA03869C5000C16485CB0654227B79467940104AAD6615EE7838D022125C9E7B88EF12C18667E7F4', '16699C320C8E3765044AC7911471437C0D74BB463CBB3202138336A26777152E', true, 'user'),
('d952e373-2f43-456e-a76a-c01541952c8b', 'raspberry.pi@aceofclubs.nl', 'A786C507067D85CE14752BD745B13C0701E6804B9E93B28BF13C6ED365D5A21FDF6BA5CC9E418EC654714C209DFE3C8E8421FEC939990058430DC5277A12790F', '1A02425D28F15C10880EC240DB7EA9E80673830585D8BF320091BC0F7DFC964A', true, 'sysop'),
('ab5b9a98-fcac-4ecd-92ec-952e4d799f02', 'edohooghiemster@live.nl', '02CFEF7D718081F60F917D8C3082F87274DFE0AFCD4362BCC1DA5A8D6585323104815275425FEC61AAA1B57BE5DB558059631938C767350C91C56C3D30AEA48A', '68CF13B93C90717CE95A5BE88919DFD947F2A8FEC37677FB19DFCD0D39EDF974', true, 'sysop'),
('4f8c9c3c-c91d-4db0-9f8b-b8a6eb9dcc97', 'owner@awsomeclub.nl', '0A9788E54B409D6C9D0283A4FDE01DDCB2C3683BED0146F3FA03869C5000C16485CB0654227B79467940104AAD6615EE7838D022125C9E7B88EF12C18667E7F4', '16699C320C8E3765044AC7911471437C0D74BB463CBB3202138336A26777152E', true, 'club_employee');

---
--- [CARD]
--- Ensure ID is an actual id which corresponds with a id on a card
---
INSERT INTO cards (id, card_code, is_activated, credits, is_blocked, user_id_id, requested_at)
VALUES ('4e162c6c-cc6f-433f-a6aa-bb167a9d8480', 'randomCardSecretID', true, 19.99, false, 'e2005006-2d8a-4f6f-b557-f16da75519bf', '2019-06-03 08:23:54+02:00');


---
--- [CLUB]
---
INSERT INTO clubs (id, min_age, club_address_id, owner_id, club_name)
VALUES ('2d466140-f70b-4ac3-8156-ee922657bacd', 18, '1c7ef92f-5424-438f-84f4-ba0158ad06e4', '4f8c9c3c-c91d-4db0-9f8b-b8a6eb9dcc97', 'ClubAwsome');


---
--- [USER]
--- Update user to have address and additional data
---
UPDATE users
SET first_name = 'Aaron', last_name = 'Beetstra', gender = 'Man', date_of_birth = '1999-01-18', address_id = '7372a47b-9629-4037-aae4-e7494979b3c4'
WHERE id = 'a2b19f94-4fb1-48d3-9e35-b6e250979d5c';

UPDATE users
SET first_name = 'Selim', last_name = 'Aydi', gender = 'Man', date_of_birth = '1997-01-1', address_id = '48812838-57c7-4289-86cd-f40a132d61b5'
WHERE id = 'e2005006-2d8a-4f6f-b557-f16da75519bf';

UPDATE users
SET first_name = 'Mr.', last_name = 'Awsome', gender = 'Man', date_of_birth = '1980-08-16', address_id = '5b214aa6-4fb6-4eca-aefa-62a4568be445'
WHERE id = '4f8c9c3c-c91d-4db0-9f8b-b8a6eb9dcc97';

UPDATE users
SET first_name = 'Edo', last_name = 'Hooghiemster', gender = 'Man', date_of_birth = '1980-08-16', address_id = 'df0eaa27-c448-41e5-a5e6-e4e021d03e9f'
WHERE id = 'ab5b9a98-fcac-4ecd-92ec-952e4d799f02';

UPDATE users
SET first_name = 'Sysop', last_name = 'Raspberry Pi', gender = 'M', date_of_birth = '2019-06-04', address_id = '7372a47b-9629-4037-aae4-e7494979b3c4'
WHERE id = 'd952e373-2f43-456e-a76a-c01541952c8b';


---
--- [PENALTIES]
---
INSERT INTO penalties (id, date_received, handed_out_by_id, received_at_id, recipient_id_id, description)
VALUES ('a2b19f94-4fb1-48d3-9e35-b6e250979d5c', '2019-05-16', '4f8c9c3c-c91d-4db0-9f8b-b8a6eb9dcc97', '2d466140-f70b-4ac3-8156-ee922657bacd', 'e2005006-2d8a-4f6f-b557-f16da75519bf', 'Verkocht coke');
