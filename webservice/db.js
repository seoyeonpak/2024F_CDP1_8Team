const mysql = require('mysql2');

const {
    MYSQL_HOST='itabus.duckdns.org',
    MYSQL_PORT=33306,
    MYSQL_USER='root',
    MYSQL_PW='nari1004*',
    MYSQL_DB='abb',
} = process.env;

const sql = mysql.createPool({
    host: MYSQL_HOST,
    port: MYSQL_PORT,
    user: MYSQL_USER,
    password: MYSQL_PW,
    database: MYSQL_DB,
  })
module.exports = sql