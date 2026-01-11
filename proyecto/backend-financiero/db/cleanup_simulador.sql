-- Borra usuarios, cuentas y transacciones generados por el simulador (CURP que empieza con 'SIMU')
use financiero_db;
DELETE t FROM transactions t
JOIN accounts a ON t.account_id = a.id
JOIN users u ON a.user_id = u.id
WHERE u.curp LIKE 'SIMU%';

DELETE a FROM accounts a
JOIN users u ON a.user_id = u.id
WHERE u.curp LIKE 'SIMU%';

DELETE FROM users WHERE curp LIKE 'SIMU%';
