const express = require('express');
const app = express();

const router = require('../web/router/view/webview');
const PORT = 50002;
const session = require('express-session');

// 세션정보
const sql = require('../db');
let MySQLStore = require('express-mysql-session')(session);
let mysqlSessionStore = new MySQLStore({}, sql);

app.use(
    session({
        key: 'session_cookie_name',
        secret: 'session_cookie_secret',
        store: mysqlSessionStore,
        resave: false,
        saveUninitialized: true,
        cookie: {
            secure: false,
        },
    })
);

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.set('views', '../web/router/views');
app.use(express.static('../web/router/'));

// 요청 도메인 확인 미들웨어
app.use((req, res, next) => {
    const allowedDomains = ['localhost', '127.0.0.1']; // 허용 도메인
    const requestDomain = req.hostname;

    if (!allowedDomains.includes(requestDomain)) {
        return res.status(403).send('Forbidden: This service is available only locally.');
    }
    next();
});

// API 라우트
const AbbdataRouter = require('../web/router/api/abblogin'); // abb api
app.use('/api/abblogin', AbbdataRouter);

// View 라우트
const webRouter = require('../web/router/view/webview');
app.use('/view/webview', webRouter);

app.use('/', router);

// 서버 실행
app.listen(PORT, 'localhost', () => { // 'localhost'로 제한
    console.log(`로컬 서버 실행 중: http://localhost:${PORT}`);
});

module.exports = app;
