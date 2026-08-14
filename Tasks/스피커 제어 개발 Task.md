# 스피커 제어 개발 Task

작성일: 2026-08-14  
대상 프로젝트: AIGlassFood  
기획서: `Docs/스피커 제어 기능 기획서.md`

## 목표

AIGlassFood 앱에 Android TTS 기반 스피커 테스트 기능을 추가한다. 사용자가 입력한 문장을 `읽기` 버튼으로 재생하고, Meta Ray-Ban이 휴대폰의 Bluetooth 미디어 출력 장치로 선택되어 있으면 안경 스피커에서 음성이 출력되도록 한다.

## 구현 범위

- [x] `SpeakerTestScreen` Compose 화면 추가
- [x] `SpeakerTestViewModel` 추가
- [x] `TtsSpeakerController` 추가
- [x] `AudioOutputDeviceMonitor` 추가
- [x] 홈 화면에서 스피커 테스트 화면 진입 버튼 추가
- [x] TTS 재생/중지/완료 상태 표시
- [x] Bluetooth 오디오 출력 장치 감지 및 안내 문구 표시
- [x] 기본 테스트 문장, 속도/톤 조절 UI 추가
- [x] Bluetooth 설정 이동 버튼 추가
- [x] 문자열 리소스 추가
- [x] 빌드 검증

## 개발 메모

- DAT SDK에 스피커 전용 capability를 가정하지 않는다.
- Android `TextToSpeech` + `AudioAttributes.USAGE_MEDIA`를 사용한다.
- Ray-Ban Meta 출력 장치 강제 전환은 하지 않고, 연결 상태 확인과 설정 이동 UX를 제공한다.
- Android 12 이상에서는 Bluetooth 장치명 확인에 `BLUETOOTH_CONNECT` 권한이 필요하다.
