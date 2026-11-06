-- Stable UUID mapping keeps links and source IDs reproducible. Legacy tables remain intact.
INSERT INTO subscriber (id,code,name,registration_year,phone,email,address,active,created_at,updated_at,version)
SELECT md5('subscriber:' || id)::uuid, 'ACC-' || id, full_name,
       extract(year from registration_date)::integer,phone,email,address,true,
       registration_date::timestamp,registration_date::timestamp,0 FROM subscribers;
INSERT INTO request (id,external_id,title,description,address,email,phone,subscriber_id,status,type,tariff_plan,
                     estimated_hours,urgent,priority_level,archived,created_at,updated_at,version)
SELECT md5('request:' || r.id)::uuid,'REQ-' || r.id,
       CASE r.type WHEN 'NEW_CONNECTION' THEN 'Подключение' WHEN 'TARIFF_CHANGE' THEN 'Смена тарифа'
        WHEN 'TECHNICAL_SUPPORT' THEN 'Техподдержка' WHEN 'DISCONNECTION' THEN 'Отключение'
        ELSE 'Замена оборудования' END,
       r.description,s.address,s.email,s.phone,md5('subscriber:' || s.id)::uuid,r.status,r.type,r.tariff_plan,
       NULL,false,CASE WHEN r.status IN ('COMPLETED','REJECTED','CANCELLED') THEN 'NOT_APPLICABLE' ELSE 'MEDIUM' END,
       false,r.created_at,r.updated_at,0 FROM connection_requests r JOIN subscribers s ON s.id=r.subscriber_id;
