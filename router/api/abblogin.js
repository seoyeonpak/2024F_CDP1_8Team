const router = require("express").Router();
const con = require("../../../db");

//차량번호로 회원 id 가져오기
router.get("/user_car_number", function(req, res) {
  var data = req.query;
  var sql = `SELECT * FROM abb.member where veh_number = '${data.id}'`;
  console.log(sql);
  con.query(sql, function (err, result){
    if(err) console.log("query is not excuted: " + err);
    else res.send(result);
  });
})

//운전자 이름으로 회원 id 가져오기
router.get("/user_driver_name", function(req, res) {
  var data = req.query;
  var sql = `SELECT * FROM abb.member where user_name = '${data.id}'`;
  console.log(sql);
  con.query(sql, function (err, result){
    if(err) console.log("query is not excuted: " + err);
    else res.send(result);
  });
})

//주행횟수확인 (월간)
router.get("/drive_count_by_month", function(req, res){
  var data = req.query;
  var sql = `
  WITH RECURSIVE T AS (
    SELECT 1 AS NUM
    UNION ALL
    SELECT NUM + 1 
    FROM T
    WHERE NUM < DAY(LAST_DAY('${data.month}-01'))
  )
  SELECT 
    DATE_FORMAT(CONCAT('${data.month}-', T.NUM), '%Y-%m-%d') AS DATE,
    IFNULL(B.CNT, 0) AS CNT
  FROM T
  LEFT OUTER JOIN (
    SELECT COUNT(*) AS CNT,
           DATE_FORMAT(drive_date, '%Y-%m-%d') AS DATE 
    FROM abb.vehicle_trips vt 
    WHERE DATE_FORMAT(drive_date, '%Y-%m') = '${data.month}'
      AND vt.member_id = '${data.id}'  -- userId 필터링
    GROUP BY DATE
  ) B ON DATE_FORMAT(CONCAT('${data.month}-', T.NUM), '%Y-%m-%d') = B.DATE
  WHERE DATE_FORMAT(CONCAT('${data.month}-', T.NUM), '%Y-%m-%d') <= CURDATE()
  ORDER BY DATE DESC;
  `;
  console.log(sql);
  con.query(sql, function (err, result) {
    if (err) console.log("query is not excuted: " + err);
    else res.send(result);
  });
});


//주행횟수확인 (연간)
router.get("/drive_count_by_year", function(req, res){
  var data = req.query;
  var sql = `select * from
  (WITH RECURSIVE months AS (
    SELECT '${data.year}-01' AS MON  
    UNION ALL
    SELECT DATE_FORMAT(DATE_ADD(CONCAT(MON, '-01'), INTERVAL 1 MONTH), '%Y-%m') 
    FROM months
    WHERE MON < '${data.year}-12'
)
SELECT 
    m.MON AS MONTH,
    IFNULL(t.CNT, 0) AS CNT
FROM months m
LEFT JOIN (
    SELECT DATE_FORMAT(drive_date, '%Y-%m') AS MON, COUNT(*) AS CNT 
    FROM abb.vehicle_trips
    WHERE member_id = ${data.id} 
      AND DATE_FORMAT(drive_date, '%Y') = '${data.year}'
    GROUP BY MON
) t ON m.MON = t.MON
ORDER BY m.MON desc)

AS RES where RES.MONTH <= (SELECT DATE_FORMAT(CURDATE(), '%Y-%m')) ORDER BY RES.MONTH DESC;`;
  console.log(sql);
  con.query(sql, function (err, result) {
    if (err) console.log("query is not excuted: " + err);
    else res.send(result);
  });
})

//운전스타일 확인(일간)
router.get('/drive_style_by_date', function(req, res){
  var data = req.query;
  var sql = `select * from abb.vehicle_trips where member_id = ${data.id} and drive_date = '${data.date}'`;
  console.log(sql);
  con.query(sql, function(err, result) {
    if(err) console.log("query is not excuted: " + err);
    else res.send(result);
  })
})

//운전스타일 확인(월간)
router.get('/drive_style_by_month', function(req, res){
  var data = req.query;
  var sql = `select * from abb.vehicle_trips where member_id = ${data.id} AND  drive_date like '${data.date}%'`;
  console.log(sql);
  con.query(sql, function(err, result) {
    if(err) console.log("query is not excuted: " + err);
    else res.send(result);
  })
})

//주행기록 확인 (일간)
router.get('/drive_record_by_date', function(req, res){
  var data = req.query;
  var sql = `select *, ROW_NUMBER() OVER (ORDER BY drive_date DESC, id ASC) AS new_id from abb.vehicle_trips where member_id = ${data.id} and drive_date = '${data.date}' order by drive_date desc, id asc;`;
  console.log(sql);
  con.query(sql, function(err, result) {
    if(err) console.log("query is not excuted: " + err);
    else res.send(result);
  })
})

//주행기록 확인 (월간)
router.get('/drive_record_by_month', function(req, res){
  var data = req.query;
  var sql = `SELECT *, ROW_NUMBER() OVER (ORDER BY drive_date DESC, id ASC) AS new_id 
  FROM abb.vehicle_trips 
  WHERE member_id = '${data.id}' 
  AND drive_date BETWEEN '${data.date}-01' AND '${data.date}-31'
  ORDER BY drive_date DESC, id ASC;`;
  console.log(sql);
  con.query(sql, function(err, result) {
    if(err) console.log("query is not excuted: " + err);
    else res.send(result);
  })
})

// //주행기록 확인 (상세보기)
// router.get('/drive_record_by_detail', function(req, res){
//   var data = req.query;
//   var sql = `SELECT *, ROW_NUMBER() OVER (ORDER BY drive_date DESC) AS new_id 
//   FROM abb.vehicle_trips 
//   WHERE member_id = '${data.userid}' and id ='${data.id}' `;
//   console.log(sql);
//   con.query(sql, function(err, result) {
//     if(err) console.log("query is not excuted: " + err);
//     else res.send(result);
//   })
// })

// 실시간 위치 api
router.get("/reallocation", function (req, res){
  var sql = `
    SELECT *
    FROM abb.vehicle_realtime_location v
    WHERE mod_date = (
        SELECT MAX(mod_date)
        FROM abb.vehicle_realtime_location
        WHERE member_id = v.member_id
    )
    ORDER BY mod_date DESC;
    `;
  con.query(sql, function (err, result) {
      if (err) console.log("query is not excuted: " + err);
      else res.send(result);
    });
});

// 특정 차량 현재 위치 api
router.get("/nowlocation", function (req, res){
  var data = req.query;
  var sql = `
    SELECT *
    FROM abb.vehicle_realtime_location v
    WHERE mod_date = (
        SELECT MAX(mod_date)
        FROM abb.vehicle_realtime_location
        WHERE member_id = v.member_id
    )
    AND member_id = '${data.id}'
    ORDER BY mod_date DESC;
    `;
  con.query(sql, function (err, result) {
      if (err) console.log("query is not excuted: " + err);
      else res.send(result);
    });
});

// 운행 순위(일간) api
router.get('/drive_rank_by_date', function(req, res) {
  var data = req.query;
  var sql = `
    SELECT 
        ROW_NUMBER() OVER(ORDER BY vt.score_drive DESC, vt.drive_date DESC) AS rank,
        m.id AS member_id,
        m.user_name,
        m.veh_number,
        vt.score_drive
    FROM 
        member m
    JOIN 
        abb.vehicle_trips vt 
        ON m.id = vt.member_id
    WHERE 
        vt.drive_date = '${data.date}'
        AND m.id = '${data.id}'
    ORDER BY 
        vt.score_drive DESC, vt.drive_date DESC;
    `;
  console.log(sql);
  con.query(sql, function(err, result){
    if(err) console.log("query is not excuted: "+ err);
    else res.send(result);
  })
})

// 운행 순위(월간) api
router.get('/drive_rank_by_month', function(req, res) {
  var data = req.query;
  var sql = `
    SELECT 
        ROW_NUMBER() OVER(ORDER BY vt.score_drive DESC, vt.drive_date DESC) AS rank,
        m.id AS member_id,
        m.user_name,
        m.veh_number,
        vt.score_drive
    FROM 
        member m
    JOIN 
        abb.vehicle_trips vt 
        ON m.id = vt.member_id
    WHERE 
        vt.drive_date BETWEEN '${data.month}-01' AND LAST_DAY('${data.month}-01')
        AND m.id = '${data.id}'
    ORDER BY 
        vt.score_drive DESC, vt.drive_date DESC;
    `;
  console.log(sql);
  con.query(sql, function(err, result){
    if(err) console.log("query is not excuted: "+ err);
    else res.send(result);
  })
})

// 전체 운행 순위(일간) api
router.get('/all_drive_rank_by_date', function(req, res) {
  var data = req.query;
  var sql = `
    SELECT 
        ROW_NUMBER() OVER(ORDER BY vt.score_drive DESC, vt.drive_date DESC) AS rank,
        m.id AS member_id,
        m.user_name,
        m.veh_number,
        vt.score_drive
    FROM 
        member m
    JOIN 
        abb.vehicle_trips vt 
        ON m.id = vt.member_id
    WHERE 
        vt.drive_date = '${data.date}'
    ORDER BY 
        vt.score_drive DESC, vt.drive_date DESC;
    `;
  console.log(sql);
  con.query(sql, function(err, result){
    if(err) console.log("query is not excuted: "+ err);
    else res.send(result);
  })
})

// 전체 운행 순위(월간) api
router.get('/all_drive_rank_by_month', function(req, res) {
  var data = req.query;
  var sql = `
    SELECT 
        ROW_NUMBER() OVER(ORDER BY vt.score_drive DESC, vt.drive_date DESC) AS rank,
        m.id AS member_id,
        m.user_name,
        m.veh_number,
        vt.score_drive
    FROM 
        member m
    JOIN 
        abb.vehicle_trips vt 
        ON m.id = vt.member_id
    WHERE 
        vt.drive_date BETWEEN '${data.month}-01' AND LAST_DAY('${data.month}-01')
    ORDER BY 
        vt.score_drive DESC, vt.drive_date DESC;
    `;
  console.log(sql);
  con.query(sql, function(err, result){
    if(err) console.log("query is not excuted: "+ err);
    else res.send(result);
  })
})

// 전체 차량번호 목록 api
router.get('/all_veh_number', function(req, res){
  var sql = `SELECT veh_number FROM abb.member;`;
  console.log(sql);
  con.query(sql, function(err, result) {
    if(err) console.log("query is not excuted: " + err);
    else res.send(result);
  })
})

// 경로
router.get('/drive_path', function(req, res){
  var data = req.query;
  var sql = 
    `WITH filtered_data AS (
        SELECT latitude, longitude, mod_date
        FROM abb.vehicle_realtime_location
        WHERE member_id = ${data.id}
          AND mod_date BETWEEN '${data.start}' AND '${data.stop}'
        ORDER BY mod_date
    ),
    numbered_data AS (
        SELECT latitude, longitude, mod_date,
              ROW_NUMBER() OVER (ORDER BY mod_date) AS row_num,
              COUNT(*) OVER () AS total_rows
        FROM filtered_data
    ),
    sampled_data AS (
        SELECT latitude, longitude, mod_date
        FROM numbered_data
        WHERE row_num = 1 -- 첫 번째 데이터
          OR row_num = total_rows -- 마지막 데이터
          OR MOD(row_num - 2, CEIL((total_rows - 2) / (0.5 * total_rows - 2))) = 0
          AND row_num NOT IN (1, total_rows) -- 첫 번째와 마지막 데이터 제외
    )
    SELECT latitude, longitude, mod_date
    FROM sampled_data
    ORDER BY mod_date;
    `;

  console.log(sql);
  con.query(sql, function(err, result) {
    if(err) console.log("query is not excuted: " + err);
    else res.send(result);
  })
})

module.exports = router;