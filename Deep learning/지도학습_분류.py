import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense
from tensorflow.keras.optimizers import Adam
from sklearn.metrics import accuracy_score
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

# 분류 문제를 위해 행동 순위를 범주형 데이터로 변환
# 여기서 범주형 등급으로 변환 (예: 1=나쁨, 2=보통, 3=좋음)
bins = [0, 3, 7, 10]  # 예를 들어, 점수 기준으로 범위 나눔
labels = [0, 1, 2]  # 나쁨, 보통, 좋음으로 분류 (0, 1, 2로 레이블)
y_binned = pd.cut(y, bins=bins, labels=labels, include_lowest=True)

# 레이블 인코딩 (필요한 경우)
le = LabelEncoder()
y_encoded = le.fit_transform(y_binned)

# 데이터 정규화
scaler = StandardScaler()  # 스케일러 객체 생성
X_scaled = scaler.fit_transform(X)  # 데이터 정규화

# 데이터를 훈련 세트와 테스트 세트로 나눔
X_train, X_test, y_train, y_test = train_test_split(X_scaled, y_encoded, test_size=0.2, random_state=42)

# DNN 모델 생성 (분류 문제)
model = Sequential()
model.add(Dense(128, input_dim=X_train.shape[1], activation='relu'))  # 첫 번째 은닉층
model.add(Dense(64, activation='relu'))  # 두 번째 은닉층
model.add(Dense(32, activation='relu'))  # 세 번째 은닉층
model.add(Dense(16, activation='relu'))  # 네 번째 은닉층
model.add(Dense(3, activation='softmax'))  # 출력층 (3개의 클래스, softmax 활성화 함수)

# 모델 컴파일 (분류 문제이므로 손실 함수는 sparse_categorical_crossentropy)
model.compile(optimizer=Adam(learning_rate=0.001), loss='sparse_categorical_crossentropy', metrics=['accuracy'])

# 모델 학습
history = model.fit(X_train, y_train, validation_data=(X_test, y_test), epochs=100, batch_size=32)

# 예측
y_pred = np.argmax(model.predict(X_test), axis=1)

# 평가 (정확도)
accuracy = accuracy_score(y_test, y_pred)

print(f'Accuracy: {accuracy}')

