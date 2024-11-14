import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout
from tensorflow.keras.optimizers import Adamax
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping
from sklearn.metrics import accuracy_score, classification_report
from imblearn.over_sampling import SMOTE
import numpy as np
import matplotlib.pyplot as plt


# 데이터 로드
data = pd.read_csv('OBD Data.csv')

# 기존 클래스 레이블을 3개 클래스로 통합하는 매핑 정의
class_mapping = {  
    1: 1,  
    2: 1,  
    3: 1,  
    4: 2,  
    5: 2,  
    6: 2,  
    7: 2,  
    8: 3,  
    9: 3,
    10: 3   
}
data['Reduced Driver Behavior rating'] = data['Driver Behavior rating'].map(class_mapping)

# 운전습관 분석에 사용할 데이터 유형 선택
features = ['Speed (km/h)', 
            'Acceleration (m/s²)', 
            'Braking intensity (%)',
            'Cornering speed (km/h)', 
            'Throttle position (%)',
            'Energy consumption (kWh/km)']

X = data[features]  # 입력 데이터
y = data['Reduced Driver Behavior rating']  # 새 행동 순위 값

# 결측치 처리 (중앙값으로 대체)
data[features].fillna(data[features].median(), inplace=True)

# One-Hot Encoding
encoder = OneHotEncoder(sparse_output=False)
y_encoded = encoder.fit_transform(y.values.reshape(-1, 1))

# 데이터 정규화
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# SMOTE를 이용한 오버샘플링
smote = SMOTE()
X_scaled, y_encoded = smote.fit_resample(X_scaled, y_encoded)

# 데이터 증강 함수 정의
def augment_data(X, y, noise_level):
    # 가우시안 노이즈 추가
    noise = np.random.normal(0, noise_level, X.shape)
    X_augmented = X + noise
    
    # 원본 데이터와 증강 데이터를 함께 결합
    X_combined = np.vstack((X, X_augmented))
    y_combined = np.vstack((y, y))
    return X_combined, y_combined

# 데이터 증강 적용
X_scaled, y_encoded = augment_data(X_scaled, y_encoded, 0.05)

# 데이터를 훈련 세트와 테스트 세트로 나눔
X_train, X_test, y_train, y_test = train_test_split(X_scaled, y_encoded, test_size=0.2, random_state=42)

# DNN 모델 생성
model = Sequential()
model.add(Dense(512, input_dim=X_train.shape[1], activation='relu'))
model.add(Dropout(0.4))
model.add(Dense(256, activation='relu'))
model.add(Dropout(0.4))
model.add(Dense(128, activation='relu'))
model.add(Dropout(0.4))
model.add(Dense(3, activation='softmax'))  # 출력층, 클래스 수는 3개

# 모델 컴파일
model.compile(optimizer=Adamax(learning_rate=0.0005), loss='categorical_crossentropy', metrics=['accuracy'])

# 모델 체크포인트 콜백
checkpoint = ModelCheckpoint(
    'best_model2.keras', 
    monitor='val_accuracy', 
    verbose=1, 
    save_best_only=True, 
    mode='max'
)

# EarlyStopping 콜백 설정
early_stopping = EarlyStopping(
    monitor='val_loss',  # 검증 손실을 모니터링
    patience=10,         # 개선이 없더라도 10 에포크 더 기다림
    verbose=1,           # 진행 상황 출력
    restore_best_weights=True  # 최적의 가중치를 복원
)

# 모델 학습
history = model.fit(
    X_train, y_train,
    validation_data=(X_test, y_test),
    epochs=1500,
    batch_size=64,
    callbacks=[checkpoint]
)

# 최적의 모델 불러오기
best_model = tf.keras.models.load_model('best_model2.keras')

# 예측
y_pred_prob = best_model.predict(X_test)
y_pred = np.argmax(y_pred_prob, axis=1)
y_test_class = np.argmax(y_test, axis=1)

# 평가 (정확도 계산)
accuracy = accuracy_score(y_test_class, y_pred)

# 결과 출력
print(f"Test Accuracy: {accuracy:.2f}")
print(classification_report(y_test_class, y_pred))


# 학습 시 history 객체에서 손실 함수 및 정확도 변화를 가져옴
loss = history.history['loss']
val_loss = history.history['val_loss']
accuracy = history.history['accuracy']
val_accuracy = history.history['val_accuracy']
epochs = range(1, len(loss) + 1)

# 손실 함수 그래프
plt.figure(figsize=(12, 5))
plt.subplot(1, 2, 1)
plt.plot(epochs, loss, label='Training Loss')
plt.plot(epochs, val_loss, label='Validation Loss')
plt.xlabel('Epochs')
plt.ylabel('Loss')
plt.title('Training and Validation Loss')
plt.legend()

# 정확도 그래프
plt.subplot(1, 2, 2)
plt.plot(epochs, accuracy, label='Training Accuracy')
plt.plot(epochs, val_accuracy, label='Validation Accuracy')
plt.xlabel('Epochs')
plt.ylabel('Accuracy')
plt.title('Training and Validation Accuracy')
plt.legend()

plt.tight_layout()
plt.show()