async function getData(apiUrl, params){
    try{
        const respons = await axios.get(apiUrl, {
            params: params,
            headers: {
                'Content-Type': 'application/json'
            }
        });
        return respons.data;
    } catch (err) {
        console.error('Error!! :', err);
        return null;
    }
}

async function postData(apiUrl, data) {
    try {
        const response = await axios.post(apiUrl, data, {
            headers: {
                'Content-Type': 'application/json'
            }
        });
        return response.data;
    } catch (err) {
        console.error('Error!! :', err);
        return null;
    }
}

async function patchData(apiUrl, params){
    try{
        const respons = await axios.patch(apiUrl, params, {
            headers: {
                'Content-Type': 'application/json'
            }
        });
        return respons.data;
    } catch (err) {
        console.error('Error!! :', err);
        return null;
    }
}

function getCurrentTimestamp() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const date = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    const milliseconds = String(now.getMilliseconds()).padStart(3, '0');
    return `${year}-${month}-${date} ${hours}:${minutes}:${seconds}.${milliseconds}`;
}

//차량번호를 userId로 변환
async function getUserIdbyCarNumber(carNumber) {
    const requestTime = new Date();
    console.log('차량번호를 userId로 변환 확인- 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/user_car_number';
    const params = { id: carNumber };
    // API 호출하여 데이터 가져오기
    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인',data)
    return data;
}

//운전자 이름을 userId로 변환
async function getUserIdbyDriverName(driverName) {
    const requestTime = new Date();
    console.log('운전자 이름을 userId로 변환 확인- 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/user_driver_name';
    const params = { id: driverName };
    // API 호출하여 데이터 가져오기
    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인',data)
    return data;
}

//주행횟수 (월간)
async function loadDriveCountMonth(userId, month){
    const requestTime = new Date();
    console.log('주행횟수 확인-월간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_count_by_month';
    const params = {id: userId, month: month};
    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

//주행횟수 (연간)
async function loadDriveCountYear(userId, year) {
    const requestTime = new Date();
    console.log('주행횟수 확인-연간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_count_by_year';
    const params = { id: userId, year: year};
    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인',data)
    return data;
}

//운전스타일 (일간)
async function loadDriveStyleDate(userId, date){
    const requestTime = new Date();
    console.log('운전스타일 확인-일간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_style_by_date';
    const params = { id: userId, date: date};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data)
    return data;
}

//운전스타일(월간)
async function loadDriveStyleMonth(userId, date){
    const requestTime = new Date();
    console.log('운전스타일 확인-월간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_style_by_month';
    const params = { id: userId, date: date};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data)
    return data;
}

//주행기록 (일간)
async function loadDriveRecordDate(userId, date){
    const requestTime = new Date();
    console.log('주행기록 확인-일간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_record_by_date';
    const params = { id: userId, date: date};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

//주행기록 (월간)
async function loadDriveRecordMonth(userId, date){
    const requestTime = new Date();
    console.log('주행기록 확인-월간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_record_by_month';
    const params = {id: userId, date: date};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);


    // console.log('확인', data);
    return data;
}

// 실시간 위치
async function loadDriveRealLocation(){
    const requestTime = new Date();
    console.log('실시간 위치 확인- 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/reallocation';
    const params = {};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

// 특정 차량 현재 위치
async function loadDriveNowLocation(userId){
    const requestTime = new Date();
    console.log('차량 현재 위치 확인- 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/nowlocation';
    const params = {id: userId};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

// 운행 순위(일간)
async function loadDriveRankingDate(userId, date) {
    const requestTime = new Date();
    console.log('운행 순위 확인-일간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_rank_by_date';
    const params = {id: userId, date: date};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

// 운행 순위(월간)
async function loadDriveRankingMonth(userId, month) {
    const requestTime = new Date();
    console.log('운행 순위 확인-월간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_rank_by_month';
    const params = {id: userId, month: month};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}


// 전체 운행 순위(일간)
async function loadAllDriveRankingDate(date) {
    const requestTime = new Date();
    console.log('전체 운행 순위 확인-일간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/all_drive_rank_by_date';
    const params = {date: date};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

// 전체 운행 순위(월간)
async function loadAllDriveRankingMonth(month) {
    const requestTime = new Date();
    console.log('전체 운행 순위 확인-월간 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/all_drive_rank_by_month';
    const params = {month: month};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

// 전체 차량번호 목록
async function loadAllCarNumber() {
    const requestTime = new Date();
    console.log('전체 차량번호 목록 확인- 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/all_veh_number';
    const params = {};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

// 경로
async function loadPath(userId, drive_start, drive_stop) {
    const requestTime = new Date();
    console.log('경로 확인- 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/drive_path';
    const params = {id: userId, start: drive_start, stop: drive_stop};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);

    // console.log('확인', data);
    return data;
}

//프로필
async function loadDriveProfile(userId) {
    const requestTime = new Date();
    console.log('프로필 확인- 요청/응답 시각 확인');
    console.log('요청시각:', getCurrentTimestamp());

    const apiUrl = '/api/abblogin/user_profile';
    const params = { id: userId};

    const data = await getData(apiUrl, params);

    const responseTime = new Date();
    console.log('응답시각:', getCurrentTimestamp());

    const timeDifference = (responseTime - requestTime) / 1000; // 초 단위
    console.log(`처리시간: ${timeDifference.toFixed(3)}초`);
    // console.log('확인',data)
    return data;
}