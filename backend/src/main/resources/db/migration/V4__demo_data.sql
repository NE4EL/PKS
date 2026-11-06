INSERT INTO subscriber(id,code,name,registration_year,phone,email,address,active,created_at,updated_at,version)
SELECT '10000000-0000-0000-0000-000000000001','ACC-DEMO','Иванов Иван Иванович',2026,'+7-999-123-45-67',
'ivanov@example.org','Москва, ул. Связи, 1',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0
WHERE NOT EXISTS (SELECT 1 FROM subscriber);
INSERT INTO request(id,external_id,title,description,subscriber_id,status,type,tariff_plan,estimated_hours,urgent,
 priority_level,archived,created_at,updated_at,version)
SELECT '20000000-0000-0000-0000-000000000001','REQ-DEMO','Подключение интернета',
'Подключить домашний интернет и настроить роутер',id,'NEW','NEW_CONNECTION','Домашний 500',4,false,'LOW',false,
CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0 FROM subscriber WHERE code='ACC-DEMO' AND NOT EXISTS (SELECT 1 FROM request);
