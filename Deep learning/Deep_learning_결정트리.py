import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.preprocessing import MinMaxScaler  # MinMaxScaler 추가

# 데이터 불러오기
data = pd.read_csv('OBD Data.csv')

# 운전습관 분석에 사용할 데이터 유형 선택
features = ['Speed (km/h)', 
            'Acceleration (m/s²)', 
            'Braking intensity (%)',
            'Cornering speed (km/h)', 
            'Throttle position (%)',
            'Regen braking level (%)', 
            'Energy consumption (kWh/km)']

X = data[features]  # 입력 데이터
y = data['Driver Behavior rating']  # 행동 순위 값


# 데이터를 훈련 세트와 테스트 세트로 나눔
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 모델 생성
model = RandomForestRegressor(n_estimators=150, random_state=42)

# 모델 훈련
model.fit(X_train, y_train)
y_pred = model.predict(X_test)

# 평가 (평균 제곱 오차 및 결정 계수 R²)
mse = mean_squared_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)

print(f'Mean Squared Error: {mse}')
print(f'R² Score: {r2}')
