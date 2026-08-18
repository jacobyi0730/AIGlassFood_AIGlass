# AIGlassFood 마이크 제어 및 STT 기능 기획서

작성일: 2026-08-18  
대상 앱: AIGlassFood  
패키지명: com.mtvs.food  
대상 디바이스: Meta Ray-Ban AI Glasses

## 1. 개요

AIGlassFood 앱에 Meta Ray-Ban AI Glasses의 마이크 입력 테스트 기능을 추가한다. 사용자는 앱 화면에서 `녹음` 버튼을 누르고 Ray-Ban Meta를 착용한 상태로 말한다. 앱은 Android의 음성 인식 기능을 이용해 입력 음성을 문자열로 변환하고, 인식 결과를 앱 화면에 표시한다.

이 기능의 핵심 목적은 Ray-Ban Meta가 Android 앱의 외부 오디오 입력 장치로 정상 동작하는지 검증하고, 현재 구현된 스피커 테스트 기능과 함께 AI 글래스의 음성 입출력 진단 화면을 구성하는 것이다.

## 2. 배경

현재 AIGlassFood 앱은 `meta-wearables-dat-android`의 CameraAccess 샘플을 기반으로 구성되어 있으며, DAT SDK를 통해 Ray-Ban Meta 등록, 권한, 세션, 카메라 스트리밍, 영상 녹화 시 오디오 권한 요청 흐름을 이미 포함하고 있다.

스피커 제어는 Android TTS와 시스템 오디오 출력 경로를 활용해 구현되어 있다. 마이크 제어도 동일하게 Ray-Ban Meta 전용 마이크 API를 직접 호출하기보다 Android 표준 오디오 입력 및 음성 인식 경로를 우선 사용한다.

단, Android 앱이 Bluetooth 마이크를 항상 강제로 선택할 수 있는 것은 아니다. `AudioRecord.setPreferredDevice()`로 원하는 입력 장치를 지정할 수 있지만, 실제 라우팅은 기기, Android 버전, Bluetooth 프로파일, 사용자 설정에 따라 달라질 수 있다. 따라서 앱은 Ray-Ban Meta 후보 입력 장치를 탐지하고, 가능한 경우 선호 입력 장치로 지정하며, 실제 라우팅 결과를 화면에 안내하는 방식으로 설계한다.

## 3. 목표

- 앱에서 `녹음` 버튼을 눌러 음성 입력을 시작하고, 다시 눌러 입력을 종료한다.
- Ray-Ban Meta의 마이크가 Android 입력 장치로 감지되는지 확인한다.
- Android STT 기능으로 음성을 문자열로 변환해 앱 화면에 출력한다.
- 인식 중 부분 결과와 최종 결과를 구분해 표시한다.
- 현재 구현된 스피커 테스트 기능과 마이크 테스트 기능을 한 화면 또는 홈 메뉴에서 자연스럽게 접근할 수 있게 UI를 구성한다.
- Ray-Ban Meta 마이크 사용 여부가 불확실할 때 사용자가 조치할 수 있는 안내를 제공한다.

## 4. 비목표

- 앱이 Android 시스템 정책을 우회해 Ray-Ban Meta 마이크를 강제로 독점 사용하지 않는다.
- Meta AI 앱의 페어링 절차 또는 Ray-Ban Meta 펌웨어 기능을 앱 내부에서 대체하지 않는다.
- DAT SDK에 공개되어 있지 않은 마이크 전용 API를 가정하지 않는다.
- 서버 기반 STT, 자체 음성 인식 모델, 음성 명령 자연어 처리까지 MVP 범위에 포함하지 않는다.
- 장시간 백그라운드 녹음이나 상시 대기형 음성 호출 기능은 포함하지 않는다.

## 5. 사용자 시나리오

### 5.1 정상 흐름

1. 사용자는 Ray-Ban Meta를 휴대폰에 Bluetooth로 연결한다.
2. 사용자는 AIGlassFood 앱에서 `마이크 테스트` 화면으로 이동한다.
3. 앱은 Bluetooth 입력 장치 목록에서 Ray-Ban Meta 후보 장치를 확인한다.
4. 사용자는 `녹음` 버튼을 누른다.
5. 앱은 마이크 권한을 확인하고, 필요하면 `RECORD_AUDIO` 권한을 요청한다.
6. 앱은 Android 음성 인식을 시작하고 화면 상태를 `인식 중`으로 표시한다.
7. 사용자가 Ray-Ban Meta 마이크를 향해 말한다.
8. 앱은 부분 인식 결과를 실시간으로 표시한다.
9. 사용자가 다시 `녹음` 버튼을 누른다.
10. 앱은 음성 인식을 종료하고 최종 STT 결과를 화면에 문자열로 출력한다.

### 5.2 Ray-Ban Meta 입력 장치 미감지 흐름

1. 사용자가 `마이크 테스트` 화면에 진입한다.
2. 앱이 Ray-Ban Meta로 추정되는 Bluetooth 입력 장치를 찾지 못한다.
3. 화면 상단에 `Ray-Ban Meta 마이크 입력을 확인할 수 없습니다.` 안내를 표시한다.
4. 사용자는 `Bluetooth 설정 열기` 버튼으로 시스템 설정에 이동한다.
5. Ray-Ban Meta 연결을 확인한 뒤 앱으로 돌아와 다시 테스트한다.

### 5.3 STT 엔진 미지원 흐름

1. 사용자가 `녹음` 버튼을 누른다.
2. 앱이 `SpeechRecognizer.isRecognitionAvailable()` 결과를 확인한다.
3. 사용 가능한 음성 인식 서비스가 없으면 오류 상태를 표시한다.
4. 화면에는 `Android 음성 인식 서비스를 사용할 수 없습니다. Google 앱 또는 음성 입력 설정을 확인해주세요.` 안내를 제공한다.

### 5.4 마이크 라우팅 불확실 흐름

1. Ray-Ban Meta는 연결되어 있지만 실제 입력이 휴대폰 마이크일 수 있다.
2. 앱은 `입력 장치가 Ray-Ban Meta인지 실제 발화 거리로 확인해주세요.` 안내를 표시한다.
3. 사용자는 휴대폰을 멀리 두고 Ray-Ban Meta를 착용한 상태에서 테스트 문장을 말한다.
4. 인식이 잘 되면 Ray-Ban Meta 마이크 입력 가능성이 높다고 판단한다.

## 6. 기능 요구사항

### 6.1 마이크 테스트 화면

- 화면 이름: `Microphone Test`
- 진입 위치: 기존 홈 화면 또는 디버그/테스트 메뉴에 `마이크 테스트` 항목 추가
- 구성 요소:
  - Ray-Ban Meta 입력 장치 상태 표시
  - `녹음 시작` / `녹음 종료` 토글 버튼
  - STT 부분 결과 표시 영역
  - 최종 변환 문자열 표시 영역
  - 결과 복사 버튼
  - 결과 초기화 버튼
  - Bluetooth 설정 열기 버튼
  - Android 음성 입력 설정 안내 버튼

### 6.2 녹음 버튼 동작

- 최초 상태의 버튼 문구는 `녹음 시작`으로 표시한다.
- 사용자가 버튼을 누르면 마이크 권한과 STT 가능 여부를 확인한다.
- 조건이 충족되면 버튼 문구를 `녹음 종료`로 변경하고 음성 인식을 시작한다.
- 사용자가 다시 버튼을 누르면 현재 인식 세션을 종료한다.
- 종료 후 최종 결과를 `인식 결과` 영역에 표시한다.
- 인식 중 화면 이탈 시 세션을 안전하게 종료하고 리소스를 해제한다.

### 6.3 STT 처리

- Android `SpeechRecognizer` API를 우선 사용한다.
- `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`와 `LANGUAGE_MODEL_FREE_FORM`을 사용한다.
- 한국어 인식을 우선한다.
  - `RecognizerIntent.EXTRA_LANGUAGE = "ko-KR"`
- 부분 결과를 받기 위해 `RecognizerIntent.EXTRA_PARTIAL_RESULTS = true`를 설정한다.
- 오프라인 인식 선호 옵션은 사용자가 선택할 수 있게 확장 가능하게 둔다.
  - `RecognizerIntent.EXTRA_PREFER_OFFLINE`
- 최종 결과는 신뢰도가 가장 높은 첫 번째 문자열을 기본 표시한다.
- 복수 후보가 제공되면 `후보 결과` 영역에 최대 3개까지 표시할 수 있다.

### 6.4 녹음 파일 처리 정책

요구사항에는 `녹음 종료 후 녹음된 내용을 STT`하는 흐름이 포함되어 있다. 다만 Android 기본 `SpeechRecognizer`는 일반적으로 실시간 마이크 입력 기반으로 동작하며, 녹음 파일을 모든 기기에서 안정적으로 전사하는 공개 표준 경로가 아니다.

따라서 MVP는 사용자 경험상 `녹음 시작/종료` 버튼을 제공하되, 내부 구현은 `SpeechRecognizer` 실시간 인식 세션으로 처리한다. 즉, 사용자가 말하는 동안 STT가 수행되고, 종료 시 최종 결과를 확정한다.

녹음 파일 저장이 필요한 경우 2단계 확장으로 분리한다.

- `AudioRecord`로 PCM 데이터를 캡처한다.
- WAV 파일로 임시 저장한다.
- API 33 이상에서 `RecognizerIntent.EXTRA_AUDIO_SOURCE` 기반 파일 디스크립터 입력 지원 여부를 검증한다.
- 기기별 STT 서비스가 파일 입력을 지원하지 않으면 자체 STT 서버 또는 온디바이스 ML 모델 도입을 검토한다.

### 6.5 입력 장치 확인

- Android `AudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)`로 입력 장치 목록을 확인한다.
- Bluetooth 계열 입력 장치를 탐지한다.
  - `TYPE_BLUETOOTH_SCO`
  - `TYPE_BLE_HEADSET`
  - `TYPE_BLE_BROADCAST`
- 장치명에 `Ray-Ban`, `Meta`, `Glasses` 등이 포함되면 Ray-Ban Meta 후보로 표시한다.
- `AudioRecord` 확장 구현 시 후보 장치를 `setPreferredDevice()`로 지정한다.
- 실제 입력 경로는 `AudioRecord.getRoutedDevice()` 또는 `getActiveMicrophones()`로 가능한 범위에서 확인한다.

### 6.6 상태 표시

- `준비됨`: STT 엔진 사용 가능, 입력 대기
- `마이크 권한 필요`: `RECORD_AUDIO` 권한이 없음
- `Ray-Ban 입력 감지`: Ray-Ban Meta 후보 입력 장치 있음
- `입력 확인 필요`: Bluetooth 입력은 있으나 Ray-Ban Meta 여부가 불확실함
- `인식 중`: 음성 인식 세션 진행 중
- `부분 결과 수신`: 실시간 인식 텍스트 갱신 중
- `완료`: 최종 STT 결과 확정
- `오류`: STT 엔진 없음, 권한 거부, 네트워크/엔진 오류, 입력 없음 등

## 7. 권한 및 설정

### 7.1 Android 권한

기존 AIGlassFood 앱에는 마이크 테스트에 필요한 주요 권한이 이미 포함되어 있다.

- `android.permission.RECORD_AUDIO`
- `android.permission.BLUETOOTH`
- `android.permission.BLUETOOTH_CONNECT`
- `android.permission.INTERNET`
- `android.permission.MODIFY_AUDIO_SETTINGS`

Android 12 이상에서는 Bluetooth 장치명과 연결 상태 확인을 위해 `BLUETOOTH_CONNECT` 런타임 권한이 필요하다. STT 및 녹음 시작 전에는 `RECORD_AUDIO` 권한이 반드시 허용되어야 한다.

### 7.2 시스템 설정 이동

앱은 문제 해결을 위해 다음 설정 화면으로 이동할 수 있는 버튼을 제공한다.

- Bluetooth 설정: `Settings.ACTION_BLUETOOTH_SETTINGS`
- 앱 상세 설정: `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`
- 음성 입력 설정: 사용 가능한 Intent가 없을 수 있으므로 기본 설정 화면 fallback 제공

## 8. 화면 설계

### 8.1 홈 화면 진입 구조

기존 홈 화면에는 `Connect my glasses`와 `Speaker test` 버튼이 있다. 여기에 `Microphone test` 버튼을 추가한다.

권장 구성:

- `Connect my glasses`
- `Speaker test`
- `Microphone test`

추후 통합 진단 화면으로 확장할 경우 `Audio test` 화면 안에 `스피커` / `마이크` 탭을 구성한다.

### 8.2 마이크 테스트 레이아웃

- 상단: 뒤로가기, 마이크 아이콘, 화면 제목 `마이크 테스트`, 새로고침 버튼
- 상태 영역:
  - 입력 장치 상태
  - STT 엔진 상태
  - 권한 상태
- 제어 영역:
  - 큰 토글 버튼 `녹음 시작` / `녹음 종료`
  - `초기화`
  - `복사`
- 인식 결과 영역:
  - 부분 결과: 인식 중 흐린 텍스트 또는 보조 색상으로 표시
  - 최종 결과: 여러 줄 읽기 영역으로 표시
- 설정 영역:
  - `Bluetooth 설정 열기`
  - `앱 권한 설정 열기`

### 8.3 스피커/마이크 통합 UI

현재 구현된 스피커 제어와 신규 마이크 제어를 함께 테스트할 수 있도록 다음 구조를 권장한다.

- 홈 화면 단순 버튼 방식
  - 구현 난이도가 낮고 현재 앱 구조와 잘 맞는다.
  - `SpeakerTestScreen`, `MicrophoneTestScreen`을 각각 독립 화면으로 유지한다.
- 통합 오디오 테스트 화면
  - 상단 탭: `스피커`, `마이크`
  - 공통 상태 패널: Bluetooth 연결 상태, Ray-Ban 후보 장치, 권한 상태
  - 탭별 본문: TTS 입력/재생, STT 녹음/결과

MVP에서는 독립 화면 방식을 적용하고, 2단계에서 통합 오디오 테스트 화면으로 정리한다.

### 8.4 UX 원칙

- 테스트 화면은 현장 검증용이므로 한 손으로 빠르게 조작할 수 있어야 한다.
- `녹음 시작` 버튼은 화면에서 가장 눈에 띄는 주 제어 요소로 배치한다.
- 인식 중 상태는 색상, 아이콘, 버튼 문구가 모두 함께 바뀌어야 한다.
- Ray-Ban Meta 마이크 사용 여부가 확실하지 않을 때는 기술 설명보다 테스트 방법을 안내한다.
- 권한 거부, STT 미지원, 입력 없음은 사용자가 다음 행동을 바로 알 수 있는 문장으로 표시한다.

## 9. 기술 설계

### 9.1 주요 컴포넌트

- `MicrophoneTestScreen`
  - Compose UI 화면
  - 녹음 토글, 상태 표시, 부분/최종 결과 표시 담당
- `MicrophoneTestViewModel`
  - STT 상태, 권한 상태, 입력 장치 상태, 결과 문자열 관리
- `SpeechToTextController`
  - `SpeechRecognizer` 초기화, `startListening`, `stopListening`, `cancel`, `destroy` 담당
  - `RecognitionListener` 이벤트를 ViewModel로 전달
- `AudioInputDeviceMonitor`
  - `AudioManager` 기반 입력 장치 목록 조회
  - Bluetooth/Ray-Ban 후보 장치 탐지
- `RecordedAudioController` 또는 `AudioRecordController`
  - 2단계 확장용
  - PCM/WAV 저장 및 입력 장치 라우팅 검증 담당

### 9.2 STT 처리 흐름

1. 화면 진입 시 `SpeechRecognizer.isRecognitionAvailable(context)` 확인
2. `AudioInputDeviceMonitor.refresh()`로 입력 장치 상태 조회
3. 사용자가 `녹음 시작` 클릭
4. `RECORD_AUDIO` 권한 확인 및 요청
5. `SpeechToTextController.startListening()` 호출
6. `onReadyForSpeech` 수신 시 상태를 `인식 준비됨`으로 전환
7. `onBeginningOfSpeech` 수신 시 상태를 `인식 중`으로 전환
8. `onPartialResults` 수신 시 부분 결과 갱신
9. 사용자가 `녹음 종료` 클릭
10. `SpeechRecognizer.stopListening()` 호출
11. `onResults` 수신 시 최종 결과 표시
12. 세션 종료 후 버튼을 `녹음 시작`으로 복구

### 9.3 STT Intent 예시

```kotlin
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
    )
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
}

speechRecognizer.startListening(intent)
```

### 9.4 입력 장치 탐지 예시

```kotlin
val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
val bluetoothInputs = inputDevices.filter { device ->
    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
}

val rayBanCandidate = bluetoothInputs.firstOrNull { device ->
    val name = device.productName?.toString().orEmpty()
    name.contains("Ray-Ban", ignoreCase = true) ||
        name.contains("Meta", ignoreCase = true) ||
        name.contains("Glasses", ignoreCase = true)
}
```

### 9.5 상태 모델 예시

```kotlin
data class MicrophoneUiState(
    val inputRouteStatus: InputRouteStatus = InputRouteStatus.Unknown,
    val recognitionStatus: RecognitionStatus = RecognitionStatus.Idle,
    val selectedDeviceName: String? = null,
    val partialText: String = "",
    val finalText: String = "",
    val resultCandidates: List<String> = emptyList(),
    val lastError: MicrophoneError? = null,
)
```

## 10. 예외 처리

### 10.1 마이크 권한 없음

- 메시지: `마이크 권한이 필요합니다. 권한을 허용한 뒤 다시 시도해주세요.`
- 조치: `RECORD_AUDIO` 런타임 권한 요청

### 10.2 STT 엔진 없음

- 메시지: `Android 음성 인식 서비스를 사용할 수 없습니다. 음성 입력 설정을 확인해주세요.`
- 조치: 앱 상세 설정 또는 기본 설정 화면 이동

### 10.3 Ray-Ban Meta 입력 미감지

- 메시지: `Ray-Ban Meta 마이크 입력을 확인할 수 없습니다. Bluetooth 연결을 확인해주세요.`
- 조치: Bluetooth 설정 열기

### 10.4 음성이 감지되지 않음

- 메시지: `음성이 감지되지 않았습니다. 안경을 착용하고 다시 말해주세요.`
- 조치: 재시도 버튼 제공

### 10.5 네트워크 또는 STT 서비스 오류

- 메시지: `음성 인식 중 오류가 발생했습니다. 네트워크 또는 음성 입력 설정을 확인해주세요.`
- 조치: 오프라인 인식 옵션 또는 재시도 제공

### 10.6 인식 중 화면 이탈

- 조치: `SpeechRecognizer.cancel()` 호출 후 `destroy()` 또는 화면 생명주기에 맞춘 정리 수행
- 결과: 부분 결과는 임시 보관하지 않고 세션을 종료한다.

## 11. 테스트 계획

### 11.1 단위 테스트

- STT 사용 불가 상태에서 `녹음 시작`이 차단되는지 확인
- 마이크 권한 거부 시 오류 상태가 표시되는지 확인
- 부분 결과 수신 시 `partialText`가 갱신되는지 확인
- 최종 결과 수신 시 `finalText`와 후보 목록이 갱신되는지 확인
- 입력 장치 목록에서 Ray-Ban 후보 탐지 로직 검증

### 11.2 수동 테스트

- Ray-Ban Meta 미연결 상태에서 `마이크 테스트` 진입 시 안내 확인
- Ray-Ban Meta 연결 후 입력 장치 후보 표시 확인
- `녹음 시작` 클릭 시 권한 요청 및 인식 시작 확인
- 말하는 동안 부분 결과가 표시되는지 확인
- `녹음 종료` 클릭 시 최종 문자열이 표시되는지 확인
- 휴대폰을 멀리 두고 Ray-Ban Meta 착용 상태에서 인식률 확인
- 권한 거부 후 앱 설정 이동이 가능한지 확인
- 화면 이탈 또는 뒤로가기 시 STT 세션이 정리되는지 확인

### 11.3 실제 디바이스 테스트 기준

- 테스트 휴대폰: Android 12 이상
- 테스트 안경: Ray-Ban Meta
- 사전 조건:
  - Meta AI 앱 설치
  - Ray-Ban Meta Bluetooth 페어링 완료
  - AIGlassFood 앱 Bluetooth 권한 허용
  - AIGlassFood 앱 마이크 권한 허용
  - Android 음성 인식 서비스 사용 가능

## 12. 성공 기준

- `녹음 시작` 후 2초 이내에 STT 인식 대기 상태로 진입한다.
- 사용자가 말한 한국어 문장이 앱 화면에 문자열로 표시된다.
- 인식 중에는 부분 결과 또는 명확한 진행 상태가 표시된다.
- `녹음 종료` 후 최종 결과가 안정적으로 확정된다.
- Ray-Ban Meta 입력 장치가 감지되지 않거나 STT 엔진을 사용할 수 없는 경우 사용자가 다음 조치를 이해할 수 있다.
- 반복 테스트 시 앱이 크래시 없이 동작한다.

## 13. 구현 우선순위

### 1단계: MVP

- `MicrophoneTestScreen` 추가
- 홈 화면에 `Microphone test` 버튼 추가
- `SpeechRecognizer` 기반 STT 구현
- `녹음 시작` / `녹음 종료` 토글 구현
- 부분 결과와 최종 결과 표시
- `RECORD_AUDIO` 권한 요청 흐름 연결
- Bluetooth 입력 장치 탐지 및 기본 안내 구현

### 2단계: 테스트 편의 기능

- 결과 복사 버튼
- 결과 초기화 버튼
- 후보 결과 최대 3개 표시
- 오프라인 인식 선호 옵션
- Android 설정 이동 버튼
- Ray-Ban Meta 입력 검증용 테스트 가이드 문구 추가

### 3단계: 녹음 파일 기반 확장

- `AudioRecord` 기반 PCM 캡처
- WAV 임시 파일 저장
- API 33 이상 `EXTRA_AUDIO_SOURCE` 기반 전사 가능성 검증
- 기기별 STT 서비스 호환성 표 작성
- 필요 시 서버 STT 또는 온디바이스 STT 모델 연동 검토

### 4단계: 통합 AI 글래스 진단 화면

- `AudioTestScreen` 추가
- `스피커` / `마이크` 탭 구성
- 공통 Bluetooth 장치 상태 패널
- 스피커 TTS 결과와 마이크 STT 결과를 하나의 테스트 리포트로 저장
- 안경다리 터치 이벤트 테스트와 연결

## 14. 리스크 및 대응

| 리스크 | 설명 | 대응 |
|---|---|---|
| Bluetooth 마이크 라우팅 불확실 | Android 기기와 Bluetooth 프로파일에 따라 실제 입력이 휴대폰 마이크로 잡힐 수 있다. | 입력 장치 후보 표시, 실제 착용 테스트 안내, `AudioRecord` 확장 검증을 제공한다. |
| STT 엔진 차이 | 제조사, Google 앱 버전, 네트워크 상태에 따라 결과와 동작이 다를 수 있다. | `SpeechRecognizer.isRecognitionAvailable()` 확인, 오류 안내, 재시도 UX를 제공한다. |
| 녹음 파일 전사 호환성 | 기본 `SpeechRecognizer`의 파일 입력 지원은 Android 버전과 인식 서비스 구현에 따라 다를 수 있다. | MVP는 실시간 STT로 구현하고 파일 전사는 2단계 검증 과제로 분리한다. |
| 권한 거부 | 사용자가 마이크 또는 Bluetooth 권한을 거부하면 기능이 동작하지 않는다. | 권한 필요 이유와 앱 설정 이동을 제공한다. |
| 주변 소음 | 착용 환경 소음으로 인식률이 낮아질 수 있다. | 부분 결과 표시, 재시도, 짧은 테스트 문장 가이드를 제공한다. |
| 세션 정리 누락 | 화면 이탈 시 STT 리소스가 남으면 오류나 크래시가 발생할 수 있다. | Compose 생명주기에 맞춰 `cancel()` 및 `destroy()`를 호출한다. |

## 15. 향후 확장

- 음식 기록을 음성으로 입력하는 기능
- “다시 읽어줘”, “다음”, “저장” 같은 음성 명령 처리
- 스피커 TTS와 마이크 STT를 결합한 대화형 식사 가이드
- 안경다리 터치 입력으로 녹음 시작/종료 제어
- STT 결과를 식단 분석 또는 음식 검색 기능과 연결
- 테스트 결과를 QA 리포트로 저장하고 공유

## 16. 참고 자료

- Android Developers: `SpeechRecognizer`
  - https://developer.android.com/reference/android/speech/SpeechRecognizer
- Android Developers: `RecognizerIntent`
  - https://developer.android.com/reference/android/speech/RecognizerIntent
- Android Developers: `AudioRecord`
  - https://developer.android.com/reference/android/media/AudioRecord
- Android Developers: Bluetooth audio recording
  - https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-recording
