import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, OneHotEncoder
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
            'Energy consumption (kWh/km)',
            'Heading (°)']


X = data[features]  # 입력 데이터
y = data['Driver Behavior rating']  # 행동 순위 값 (예측 대상)

# One-Hot Encoding 
encoder = OneHotEncoder(sparse_output=False)
y_encoded = encoder.fit_transform(y.values.reshape(-1, 1))


# 데이터 정규화
scaler = StandardScaler()  # 스케일러 객체 생성
X_scaled = scaler.fit_transform(X)  # 입력 데이터 정규화

# 데이터를 훈련 세트와 테스트 세트로 나눔
X_train, X_test, y_train, y_test = train_test_split(X_scaled, y_encoded, test_size=0.2, random_state=42)

# DNN 모델 생성 (Softmax 활성화 함수 사용)
model = Sequential()
model.add(Dense(128, input_dim=X_train.shape[1], activation='relu'))  # 첫 번째 은닉층
model.add(Dense(64, activation='relu'))  # 두 번째 은닉층
model.add(Dense(32, activation='relu'))  # 세 번째 은닉층
model.add(Dense(16, activation='relu'))  # 네 번째 은닉층
model.add(Dense(10, activation='softmax'))  # 출력층, 클래스 수는 10개

# 모델 컴파일 (손실 함수는 다중 클래스 분류에 적합한 Categorical Crossentropy 사용)
model.compile(optimizer=Adam(learning_rate=0.0005), loss='categorical_crossentropy', metrics=['accuracy'])

# 모델 학습
history = model.fit(X_train, y_train, validation_data=(X_test, y_test), epochs=100, batch_size=32)

# 예측
y_pred_prob = model.predict(X_test)
y_pred = np.argmax(y_pred_prob, axis=1)
y_test_class = np.argmax(y_test, axis=1)

# 평가 (정확도 계산)
accuracy = accuracy_score(y_test_class, y_pred)

print(f'Accuracy: {accuracy}')
