# 마이크 제어 개발 Task

작성일: 2026-08-18  
대상 프로젝트: AIGlassFood  
기획서: `Docs/마이크 제어 및 STT 기능 기획서.md`

## 목표

AIGlassFood 앱에 Meta Ray-Ban 마이크 기반 STT 테스트 기능을 추가한다. 사용자가 `녹음 시작` 버튼을 누르면 Ray-Ban Meta로 추정되는 Bluetooth 입력 장치가 감지된 경우에만 Android 음성 인식을 시작하고, `녹음 종료` 버튼을 누르면 최종 인식 결과를 앱 화면에 문자열로 출력한다.

## 구현 범위

- [x] `MicrophoneTestScreen` Compose 화면 추가
- [x] `MicrophoneTestViewModel` 추가
- [x] `SpeechToTextController` 추가
- [x] `AudioInputDeviceMonitor` 추가
- [x] Ray-Ban Meta 후보 입력 장치 감지
- [x] Android 단말기 마이크만 있는 상태에서는 STT 시작 차단
- [x] Bluetooth 통신 오디오 장치 라우팅 요청
- [x] `녹음 시작` / `녹음 종료` 토글 구현
- [x] 부분 결과 및 최종 STT 결과 표시
- [x] 결과 복사 및 초기화 UI 추가
- [x] 홈 화면에서 마이크 테스트 화면 진입 버튼 추가
- [x] 문자열 리소스 추가
- [x] 빌드 검증

## 개발 메모

- DAT SDK에 마이크 전용 capability를 가정하지 않는다.
- Android `SpeechRecognizer`는 입력 장치를 직접 지정하는 API가 없으므로, Ray-Ban Meta 후보 Bluetooth 입력 장치를 먼저 탐지하고 Bluetooth 통신 라우팅을 요청한 뒤 STT를 시작한다.
- Ray-Ban Meta 후보 입력 장치가 없으면 휴대폰 내장 마이크로 fallback하지 않고 기능을 차단한다.
- 실제 오디오 라우팅은 Android 기기, Bluetooth 프로파일, 사용자 설정에 영향을 받을 수 있으므로 화면에 Ray-Ban 착용 상태에서 테스트하라는 안내를 표시한다.
