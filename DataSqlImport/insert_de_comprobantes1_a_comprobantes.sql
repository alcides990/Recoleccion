 INSERT INTO comprobantes1
SELECT *
FROM comprobantes c
WHERE NOT EXISTS (
    SELECT 1
    FROM comprobantes1 c1
    WHERE c1.nrorecibo = c.nrorecibo
);