import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense
from tensorflow.keras.optimizers import Adam
from sklearn.metrics import mean_squared_error, r2_score
import numpy as np

# 데이터 로드
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
y = data['Driver Behavior rating']  # 행동 순위 값 (예측되는 결과 값)

# 데이터 정규화
scaler = StandardScaler()  # 스케일러 객체 생성
X_scaled = scaler.fit_transform(X)  # 데이터 정규화

# 데이터를 훈련 세트와 테스트 세트로 나눔
X_train, X_test, y_train, y_test = train_test_split(X_scaled, y, test_size=0.2, random_state=42)

# DNN 모델 생성 (은닉층 추가 및 수 변경으로 복잡성 조절)
model = Sequential()
model.add(Dense(128, input_dim=X_train.shape[1], activation='relu'))  # 첫 번째 은닉층
model.add(Dense(64, activation='relu'))  # 두 번째 은닉층
model.add(Dense(32, activation='relu'))  # 세 번째 은닉층
model.add(Dense(16, activation='relu'))  # 세 번째 은닉층
model.add(Dense(1))  # 출력층 (회귀 문제이므로 활성화 함수 없음)

# 모델 컴파일 (회귀 문제이므로 손실 함수로 MSE 사용) - 학습률 조정으로 최적화 확인
model.compile(optimizer=Adam(learning_rate=0.001), loss='mean_squared_error')

# 모델 학습
history = model.fit(X_train, y_train, validation_data=(X_test, y_test), epochs=100, batch_size=32)

# 예측
y_pred = model.predict(X_test)

# 평가 (평균 제곱 오차 및 결정 계수 R²)
mse = mean_squared_error(y_test, y_pred)
r2 = r2_score(y_test, y_pred)

print(f'Mean Squared Error: {mse}')
print(f'R² Score: {r2}')
