# vibe-coded-monthly-balance

Claude Code와 함께 바이브 코딩으로 만드는 안드로이드 월간 잔고 앱(MonthlyBalance)이다. 수입·지출·실제 잔고를 수동으로 기록하고, 이번 기간의 잔고 추이를 꺾은선 그래프로 보여준다.

설계와 개발 단계는 [PLAN.md](PLAN.md)에 있다.

## 빌드

Android Studio에서 열면 된다. 터미널에서는 시스템 JDK(17 이상, JDK 25에서 확인)로 바로 빌드된다. GraalVM은 jlink 단계에서 실패하므로, GraalVM이 기본이면 `JAVA_HOME`을 Android Studio의 JBR(`C:\Program Files\Android\Android Studio\jbr`)로 지정한다.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

### 릴리스 APK

```powershell
.\gradlew.bat assembleRelease
# 결과: app\build\outputs\apk\release\app-release.apk
```

서명 정보는 루트의 `keystore.properties`(git에 올리지 않음)에서 읽고, 키는 `C:\Users\chick\.android-keys\monthly-balance.jks`에 있다. 이 두 파일을 잃으면 설치된 앱을 같은 앱으로 업데이트할 수 없으므로 함께 백업해 둔다. `keystore.properties`가 없으면 서명 없이 빌드된다.

디버그 빌드가 설치된 기기에는 서명이 달라 릴리스 APK를 덮어 설치할 수 없다. 디버그 앱을 지운 뒤(기록도 지워진다) 설치한다.

디버그 빌드에서 샘플 기록을 넣으려면 `adb shell am start -n com.balance.monthly/.debug.SeedSampleActivity`를 실행한다(기존 기록은 지워진다. `--ez clear true`를 붙이면 지우기만 한다).

## 시작 속도

- Baseline Profile·Startup Profile 다시 만들기: `.\gradlew.bat :app:generateBaselineProfile` (연결된 기기 필요, 기기의 기록이 샘플로 바뀐다)
- 콜드 스타트 측정: `.\gradlew.bat :benchmark:connectedBenchmarkReleaseAndroidTest`

### 측정 기록

| 날짜 | 기기 | 빌드 | 모드 | TTID 중앙값 (최소~최대) | TTFD 중앙값 |
| --- | --- | --- | --- | --- | --- |
| 2026-10-20 | 에뮬레이터 Medium Phone API 36.1 (x86_64) | benchmarkRelease (R8) | 프로파일 없음 | 989.8ms (910.3~1,157.8) | 1,097.8ms |
| 2026-10-20 | 에뮬레이터 Medium Phone API 36.1 (x86_64) | benchmarkRelease (R8) | Baseline Profile | 904.1ms (778.2~2,402.7) | 1,068.6ms |
| 2026-10-21 | 에뮬레이터 Medium Phone API 36.1 (x86_64) | benchmarkRelease (R8), 예측 포함 | 프로파일 없음 | 887.6ms (831.1~1,017.9) | 1,027.3ms |
| 2026-10-21 | 에뮬레이터 Medium Phone API 36.1 (x86_64) | benchmarkRelease (R8), 예측 포함 | Baseline Profile | 825.5ms (748.1~1,393.9) | 931.1ms |

콜드 스타트 10회. 에뮬레이터 수치라 절대값은 실기기와 다르다. 목표(중급 실기기 TTID 500ms 이하)는 실기기에서 다시 잰다.

2026-10-20 측정은 샘플 데이터 화면이 측정용 빌드에서 빠져 있던 때라, 기록이 없는 빈 홈으로 측정됐을 수 있다(프로파일도 같은 상태로 생성). 2026-10-21 측정은 이를 고친 뒤 샘플 기록 약 120건으로 프로파일을 다시 만들고 잰 값이다.
