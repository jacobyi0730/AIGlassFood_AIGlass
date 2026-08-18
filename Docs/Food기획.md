# Food 앱 고도화 기획서

작성일: 2026-08-18  
대상 앱: Food Android App  
대상 디바이스: Meta Ray-Ban AI Glasses  
연동 서버: PC Local Server  
AI 연동: NVIDIA Nemotron API

## 1. 개요

Food 앱은 Meta Ray-Ban AI Glasses의 카메라, 마이크, 스피커를 활용해 사용자가 눈앞의 음식 재료를 촬영하고, 음성으로 원하는 조건을 말하면, PC 서버를 통해 NVIDIA Nemotron API에 레시피 생성을 요청하는 Android 앱이다.

사용자는 Ray-Ban Meta 카메라로 음식 이미지를 촬영하고, Food 앱의 녹음 버튼을 눌러 Ray-Ban Meta 마이크로 “이 재료로 다이어트 저녁 메뉴 추천해줘” 같은 프롬프트를 입력한다. 앱은 이미지와 STT 텍스트를 PC 서버로 전송하고, 서버는 NVIDIA Nemotron API 응답을 받아 Food 앱으로 반환한다. 사용자는 앱 화면에서 레시피를 읽거나, TTS 버튼으로 Ray-Ban Meta 스피커를 통해 음성 안내를 들을 수 있다.

## 2. 서비스 목표

- Ray-Ban Meta 카메라로 음식물 또는 식재료 이미지를 촬영한다.
- Ray-Ban Meta 마이크 입력을 Android STT로 변환해 레시피 요청 프롬프트를 만든다.
- Food 앱 화면에 STT 프롬프트 텍스트를 표시하고 사용자가 확인할 수 있게 한다.
- `레시피 조회` 버튼으로 촬영 이미지와 프롬프트 텍스트를 PC 서버에 전송한다.
- PC 서버는 NVIDIA Nemotron API로 이미지와 텍스트를 전달해 레시피 답변을 생성한다.
- Food 앱은 서버에서 받은 레시피 결과 텍스트를 화면에 표시한다.
- Food 앱은 레시피 TTS 버튼을 통해 Android TTS를 Ray-Ban Meta 스피커로 출력한다.

## 3. 사용자 시나리오

### 3.1 정상 사용 흐름

1. 사용자는 Ray-Ban Meta를 착용하고 Food 앱을 실행한다.
2. 앱에서 Ray-Ban Meta 연결 및 카메라 세션을 시작한다.
3. 사용자는 음식물 또는 식재료를 Ray-Ban Meta 카메라로 촬영한다.
4. 촬영된 이미지가 Food 앱 화면에 미리보기로 표시된다.
5. 사용자는 `녹음 시작` 버튼을 누른다.
6. Ray-Ban Meta 마이크 입력이 활성화되고 Android STT가 시작된다.
7. 사용자는 “이 재료로 20분 안에 만들 수 있는 한식 레시피 알려줘”라고 말한다.
8. 앱은 STT 결과를 프롬프트 텍스트로 화면에 표시한다.
9. 사용자는 `레시피 조회` 버튼을 누른다.
10. 앱은 촬영 이미지와 프롬프트 텍스트를 PC 서버로 전송한다.
11. PC 서버는 NVIDIA Nemotron API에 멀티모달 레시피 생성을 요청한다.
12. 서버는 생성된 레시피 텍스트를 Food 앱으로 반환한다.
13. Food 앱은 레시피 결과를 화면에 표시한다.
14. 사용자는 `레시피 TTS` 버튼을 누른다.
15. Android TTS가 레시피 텍스트를 음성으로 변환하고 Ray-Ban Meta 스피커로 출력한다.

### 3.2 프롬프트 수정 흐름

1. STT 결과가 화면에 표시된다.
2. 사용자가 인식 결과에 오타가 있거나 조건을 추가하고 싶다.
3. 사용자는 텍스트 입력 영역에서 프롬프트를 직접 수정한다.
4. 수정된 프롬프트로 `레시피 조회`를 요청한다.

### 3.3 서버 연결 실패 흐름

1. 사용자가 `레시피 조회` 버튼을 누른다.
2. Food 앱이 PC 서버에 연결하지 못한다.
3. 앱은 `PC 서버에 연결할 수 없습니다. 같은 Wi-Fi에 연결되어 있는지 확인해주세요.` 메시지를 표시한다.
4. 사용자는 서버 주소 설정 또는 네트워크 상태를 확인한다.

## 4. 비목표

- Food 앱이 NVIDIA API 키를 직접 보관하지 않는다.
- Food 앱에서 NVIDIA Nemotron API를 직접 호출하지 않는다.
- Ray-Ban Meta 내부 펌웨어나 Meta AI 앱 기능을 대체하지 않는다.
- MVP에서는 사용자 계정, 식단 기록 DB, 영양소 정밀 계산, 알레르기 의료 판단을 포함하지 않는다.
- 레시피 결과는 참고용이며 의료 또는 영양 전문 진단으로 제공하지 않는다.

## 5. 전체 아키텍처

```text
Ray-Ban Meta
  ├─ Camera: 음식 이미지 촬영
  ├─ Microphone: 사용자 요청 음성 입력
  └─ Speaker: 레시피 TTS 출력

Food Android App
  ├─ DAT Camera Session
  ├─ Android STT
  ├─ Prompt Editor
  ├─ Recipe Result View
  └─ Android TTS

PC Local Server
  ├─ /api/recipe endpoint
  ├─ image + prompt validation
  ├─ NVIDIA API key 관리
  └─ NVIDIA Nemotron API 호출

NVIDIA Nemotron API
  └─ image + text 기반 레시피 생성
```

## 6. 화면 구성

### 6.1 메인 레시피 화면

Food 앱은 하나의 Activity 안에서 주요 작업을 한 화면에 배치한다.

- 상단: Ray-Ban Meta 연결 상태
- 카메라 영역:
  - Ray-Ban Meta 카메라 프리뷰
  - `촬영` 버튼
  - 촬영 이미지 미리보기
- 프롬프트 영역:
  - `녹음 시작` / `녹음 종료` 버튼
  - STT 결과 텍스트 입력창
  - `초기화` 버튼
- 요청 영역:
  - `레시피 조회` 버튼
  - 요청 진행 상태 표시
- 결과 영역:
  - 레시피 결과 텍스트
  - `레시피 TTS` 버튼
  - `TTS 중지` 버튼

### 6.2 권장 UI 배치

```text
[Ray-Ban 연결 상태]

[카메라 프리뷰 또는 촬영 이미지]
[촬영] [다시 촬영]

[녹음 시작/종료]
[프롬프트 텍스트 입력/수정 영역]

[레시피 조회]

[레시피 결과 텍스트]
[레시피 TTS] [중지]
```

## 7. 기능 요구사항

### 7.1 음식 이미지 촬영

- Ray-Ban Meta 카메라 세션을 시작한다.
- 사용자가 `촬영` 버튼을 누르면 현재 음식 이미지를 캡처한다.
- 촬영 성공 시 이미지 미리보기를 표시한다.
- `다시 촬영` 버튼으로 기존 이미지를 교체할 수 있다.
- 레시피 조회 전 이미지가 없으면 `음식 이미지를 먼저 촬영해주세요.` 메시지를 표시한다.

### 7.2 Ray-Ban Meta 마이크 STT

- 사용자가 `녹음 시작` 버튼을 누르면 Ray-Ban Meta 후보 Bluetooth 입력 장치를 확인한다.
- Ray-Ban Meta 마이크 입력이 감지되지 않으면 휴대폰 마이크로 fallback하지 않는다.
- Android `SpeechRecognizer`로 한국어 STT를 수행한다.
- 부분 결과를 표시하되, 최종 결과를 프롬프트 텍스트 입력창에 반영한다.
- 사용자는 STT 결과를 직접 수정할 수 있다.
- 빈 프롬프트 상태에서 레시피 조회를 누르면 `요청 내용을 말하거나 입력해주세요.` 메시지를 표시한다.

### 7.3 레시피 조회

- `레시피 조회` 버튼 클릭 시 Food 앱은 다음 데이터를 PC 서버로 전송한다.
  - 촬영 이미지
  - 프롬프트 텍스트
  - 선택 언어
  - 요청 옵션
- 요청 중에는 버튼을 비활성화하고 로딩 상태를 표시한다.
- 서버 응답 성공 시 레시피 결과 텍스트를 화면에 표시한다.
- 서버 응답 실패 시 사용자가 조치 가능한 오류 메시지를 표시한다.

### 7.4 레시피 결과 표시

- 결과 텍스트는 단계별로 읽기 쉽게 표시한다.
- 서버가 구조화 응답을 제공하면 다음 섹션으로 나누어 보여준다.
  - 요리명
  - 예상 조리 시간
  - 필요한 재료
  - 조리 단계
  - 팁
  - 주의사항
- MVP에서는 서버가 반환한 텍스트를 그대로 표시하고, 2단계에서 구조화 UI를 적용한다.

### 7.5 Ray-Ban Meta 스피커 TTS

- 사용자가 `레시피 TTS` 버튼을 누르면 레시피 결과 텍스트를 Android TTS로 재생한다.
- Android 미디어 출력이 Ray-Ban Meta로 선택되어 있으면 안경 스피커로 출력된다.
- Ray-Ban Meta 출력 장치가 감지되지 않으면 `Android 미디어 출력에서 Ray-Ban Meta를 선택해주세요.` 안내를 표시한다.
- 긴 레시피는 전체를 한 번에 읽기보다 단계별 읽기 확장이 가능해야 한다.

## 8. Android 앱 기술 설계

### 8.1 주요 패키지

```text
food/
  camera/
    FoodCameraController.kt
    FoodImageState.kt
  microphone/
    FoodSttController.kt
    AudioInputDeviceMonitor.kt
  speaker/
    RecipeTtsController.kt
    AudioOutputDeviceMonitor.kt
  recipe/
    RecipeRepository.kt
    RecipeApiClient.kt
    RecipeUiState.kt
  ui/
    FoodRecipeScreen.kt
    RecipeResultView.kt
```

### 8.2 UI 상태 모델

```kotlin
data class FoodRecipeUiState(
    val isGlassesRegistered: Boolean = false,
    val isCameraSessionActive: Boolean = false,
    val capturedImageUri: Uri? = null,
    val capturedImageBytes: ByteArray? = null,
    val isListening: Boolean = false,
    val promptText: String = "",
    val partialSttText: String = "",
    val isRequestingRecipe: Boolean = false,
    val recipeText: String = "",
    val isSpeakingRecipe: Boolean = false,
    val errorMessage: String? = null,
)
```

`ByteArray`는 Compose 상태 비교에 불리하므로 실제 구현에서는 파일 `Uri` 또는 repository 내부 캐시로 관리하는 것을 권장한다.

### 8.3 레시피 조회 UseCase

```kotlin
suspend fun requestRecipe(
    imageUri: Uri,
    prompt: String,
): Result<String> {
    val imageBytes = contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
        ?: return Result.failure(IllegalStateException("Image is empty"))

    return recipeApiClient.requestRecipe(
        imageBytes = imageBytes,
        prompt = prompt,
        language = "ko-KR",
    )
}
```

### 8.4 Android에서 서버로 전송

MVP에서는 `multipart/form-data`를 사용한다.

```http
POST /api/recipe HTTP/1.1
Content-Type: multipart/form-data

image: food.jpg
prompt: 이 재료로 20분 안에 만들 수 있는 한식 레시피 알려줘
language: ko-KR
```

응답 예시:

```json
{
  "recipeText": "요리명: ...\n재료: ...\n조리 방법: ...",
  "model": "nvidia/llama-3.1-nemotron-nano-vl-8b-v1",
  "elapsedMs": 3200
}
```

## 9. PC 서버 기술 설계

### 9.1 서버 역할

- Food 앱 요청 수신
- 이미지 크기 및 파일 타입 검증
- 프롬프트 텍스트 검증
- NVIDIA API Key 보호
- NVIDIA Nemotron API 호출
- 레시피 응답 정리 후 Food 앱에 반환

### 9.2 서버 환경 변수

```text
NVIDIA_API_KEY=...
NVIDIA_BASE_URL=https://integrate.api.nvidia.com/v1
NVIDIA_MODEL=nvidia/llama-3.1-nemotron-nano-vl-8b-v1
SERVER_PORT=8000
```

모델명은 실제 NVIDIA API Catalog에서 사용 가능한 멀티모달 Nemotron 계열 모델로 교체 가능해야 한다.

### 9.3 서버 API

#### `POST /api/recipe`

요청:

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `image` | file | Y | Ray-Ban Meta로 촬영한 음식 이미지 |
| `prompt` | string | Y | STT 또는 직접 입력 프롬프트 |
| `language` | string | N | 기본값 `ko-KR` |

응답:

```json
{
  "recipeText": "string",
  "model": "string",
  "elapsedMs": 0
}
```

오류:

```json
{
  "message": "PC 서버에서 레시피를 생성하지 못했습니다.",
  "code": "NVIDIA_API_FAILED"
}
```

### 9.4 Node.js 서버 예시

```javascript
import express from "express";
import multer from "multer";
import OpenAI from "openai";

const app = express();
const upload = multer({ limits: { fileSize: 8 * 1024 * 1024 } });

const nvidia = new OpenAI({
  apiKey: process.env.NVIDIA_API_KEY,
  baseURL: process.env.NVIDIA_BASE_URL ?? "https://integrate.api.nvidia.com/v1",
});

app.post("/api/recipe", upload.single("image"), async (req, res) => {
  try {
    const prompt = String(req.body.prompt ?? "").trim();
    if (!req.file || !prompt) {
      return res.status(400).json({ message: "image and prompt are required" });
    }

    const imageBase64 = req.file.buffer.toString("base64");
    const model = process.env.NVIDIA_MODEL ?? "nvidia/llama-3.1-nemotron-nano-vl-8b-v1";

    const startedAt = Date.now();
    const completion = await nvidia.chat.completions.create({
      model,
      messages: [
        {
          role: "system",
          content:
            "너는 음식 이미지와 사용자 조건을 바탕으로 한국어 레시피를 작성하는 요리 도우미다.",
        },
        {
          role: "user",
          content: [
            {
              type: "text",
              text:
                `사용자 요청: ${prompt}\n` +
                "이미지 속 재료를 추정하고, 안전하게 먹을 수 있는 일반 레시피를 작성해줘.",
            },
            {
              type: "image_url",
              image_url: {
                url: `data:${req.file.mimetype};base64,${imageBase64}`,
              },
            },
          ],
        },
      ],
      temperature: 0.4,
      max_tokens: 1200,
    });

    const recipeText = completion.choices?.[0]?.message?.content ?? "";
    res.json({
      recipeText,
      model,
      elapsedMs: Date.now() - startedAt,
    });
  } catch (error) {
    console.error(error);
    res.status(500).json({
      message: "PC 서버에서 레시피를 생성하지 못했습니다.",
      code: "NVIDIA_API_FAILED",
    });
  }
});

app.listen(Number(process.env.SERVER_PORT ?? 8000), "0.0.0.0");
```

## 10. 프롬프트 설계

서버는 앱에서 받은 사용자 프롬프트를 그대로 전달하지 않고, 레시피 생성에 필요한 시스템 지시를 함께 구성한다.

### 10.1 시스템 프롬프트

```text
너는 음식 이미지와 사용자 요청을 바탕으로 한국어 레시피를 작성하는 요리 도우미다.
이미지에서 보이는 재료를 추정하되, 확실하지 않은 재료는 "추정"이라고 표현한다.
위험한 조리법, 알레르기, 상한 음식 여부는 단정하지 말고 주의 문구를 제공한다.
결과는 요리명, 재료, 조리 시간, 조리 단계, 팁 순서로 작성한다.
```

### 10.2 사용자 프롬프트 템플릿

```text
사용자 요청:
{promptText}

출력 형식:
1. 요리명
2. 이미지에서 확인한 재료
3. 추가하면 좋은 재료
4. 예상 조리 시간
5. 단계별 조리법
6. 맛/영양 팁
7. 주의사항
```

## 11. 상태 및 오류 처리

| 상황 | 앱 메시지 | 대응 |
|---|---|---|
| Ray-Ban 미등록 | `Ray-Ban Meta를 먼저 연결해주세요.` | Meta AI 앱 등록 흐름 |
| 이미지 없음 | `음식 이미지를 먼저 촬영해주세요.` | 촬영 버튼 안내 |
| 프롬프트 없음 | `요청 내용을 말하거나 입력해주세요.` | STT 또는 직접 입력 |
| 마이크 미감지 | `Ray-Ban Meta 마이크 입력을 확인할 수 없습니다.` | Bluetooth 설정 이동 |
| 스피커 미감지 | `Android 미디어 출력에서 Ray-Ban Meta를 선택해주세요.` | 사운드/Bluetooth 설정 이동 |
| 서버 연결 실패 | `PC 서버에 연결할 수 없습니다.` | IP, 포트, Wi-Fi 확인 |
| NVIDIA 오류 | `레시피 생성에 실패했습니다. 잠시 후 다시 시도해주세요.` | 서버 로그 확인 |
| 응답 지연 | `레시피를 생성하는 중입니다.` | 로딩 UI, timeout |

## 12. 보안 및 개인정보

- NVIDIA API Key는 PC 서버에만 저장한다.
- Android 앱에는 API Key를 포함하지 않는다.
- 이미지와 음성 인식 결과는 사용자가 `레시피 조회`를 누를 때만 서버로 전송한다.
- 서버는 MVP에서 요청 이미지를 디스크에 저장하지 않고 메모리에서 처리한다.
- 로그에는 이미지 원본, 전체 프롬프트, API Key를 남기지 않는다.
- 같은 Wi-Fi 로컬 테스트에서는 서버를 `0.0.0.0`으로 열되 방화벽 허용 범위를 개발 PC로 제한한다.

## 13. 구현 우선순위

### 1단계: MVP

- Ray-Ban Meta 카메라 촬영
- Ray-Ban Meta 마이크 STT
- STT 프롬프트 텍스트 표시 및 수정
- PC 서버 주소 설정
- `레시피 조회` 요청
- 서버에서 NVIDIA Nemotron API 호출
- 레시피 결과 텍스트 표시
- 레시피 TTS 출력

### 2단계: 사용성 개선

- 촬영 이미지 자르기 또는 다시 촬영
- 프롬프트 예시 버튼
- 레시피 결과 구조화 카드 UI
- TTS 단계별 읽기
- 요청 취소
- 서버 연결 테스트 버튼

### 3단계: Food 앱 고도화

- 식사 기록 저장
- 즐겨찾기 레시피
- 알레르기/선호 식단 설정
- 칼로리 및 영양 정보 추정
- 다국어 응답
- 안경다리 터치로 다음 단계 읽기

## 14. 테스트 계획

### 14.1 Android 앱 테스트

- Ray-Ban Meta 연결 전/후 화면 상태 확인
- 음식 이미지 촬영 및 미리보기 확인
- Ray-Ban Meta 마이크 STT 인식 확인
- 휴대폰 내장 마이크 fallback 차단 확인
- 프롬프트 직접 수정 확인
- 서버 연결 실패 메시지 확인
- 레시피 결과 표시 확인
- Ray-Ban Meta 스피커 TTS 출력 확인

### 14.2 서버 테스트

- 이미지 없는 요청 거부
- 프롬프트 없는 요청 거부
- 8MB 이상 이미지 거부
- NVIDIA API Key 누락 시 오류 응답
- NVIDIA API timeout 처리
- 정상 요청 시 `recipeText` 반환

### 14.3 실제 시나리오 테스트

- 냉장고 속 재료 촬영 후 레시피 요청
- 식탁 위 완성 음식 촬영 후 비슷한 레시피 요청
- “매운맛 빼고”, “10분 안에”, “아이도 먹을 수 있게” 같은 제약 조건 STT 입력
- 결과를 Ray-Ban Meta 스피커로 들으며 조리 단계 확인

## 15. 성공 기준

- 사용자는 한 화면에서 촬영, 음성 프롬프트, 레시피 조회, TTS 재생을 완료할 수 있다.
- 촬영 이미지와 프롬프트가 PC 서버로 정상 전송된다.
- 서버는 NVIDIA Nemotron API 응답을 받아 10초 내외로 앱에 반환한다.
- Food 앱은 레시피 결과를 읽기 쉬운 텍스트로 표시한다.
- Ray-Ban Meta가 미디어 출력 장치로 선택된 상태에서 레시피가 안경 스피커로 재생된다.
- Ray-Ban Meta 마이크가 감지되지 않으면 휴대폰 마이크로 대체하지 않는다.

## 16. 참고

- NVIDIA NIM LLM API는 OpenAI 호환 Chat Completions 인터페이스를 제공한다.
- 이미지 입력에는 NVIDIA API Catalog에서 사용 가능한 Nemotron 계열 비전-언어 모델을 선택해야 한다.
- 현재 AIGlassFood의 기기 제어 구현 참고 문서: `기기제어.md`
