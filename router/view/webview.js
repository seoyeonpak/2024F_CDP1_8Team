const router = require("express").Router();
const path = require('path');

// //로그인 페이지
// router.get('/', (req, res) => {
//     res.sendFile(path.join(__dirname, '../views', '1-main.html'));
// });

// 로그인후 페이지 - 테스트용
router.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '1-main_login.html'));
});

router.get('/1-main_login.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '1-main_login.html'));
});

router.get('/1-main.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '1-main.html'));
});

router.get('/2-menubar.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '2-menubar.html'));
});

router.get('/3-all-location.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '3-all-location.html'));
});

router.get('/4-ranking.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '4-ranking.html'));
});

router.get('/5-now-loc.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '5-now-loc.html'));
});

router.get('/6-drive-num.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '6-drive-num.html'));
});

router.get('/7-drive-route.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '7-drive-route.html'));
});

router.get('/8-drive-style.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '8-drive-style.html'));
});

router.get('/9-drive-record.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '9-drive-record.html'));
})

router.get('/10-1mypage.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '10-1mypage.html'));
})

router.get('/10-2mypage.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '10-2mypage.html'));
})

router.get('/10-3mypage.html', (req, res) => {
    res.sendFile(path.join(__dirname, '../views', '10-3mypage.html'));
})

module.exports = router;