# vibe-coded-monthly-balance

Claude Code와 함께 바이브 코딩으로 만드는 안드로이드 월간 잔고 앱(MonthlyBalance)이다. 수입·지출·실제 잔고를 수동으로 기록하고, 이번 기간의 잔고 추이를 꺾은선 그래프로 보여준다.

설계와 개발 단계는 [PLAN.md](PLAN.md)에 있다.

## 빌드

Android Studio에서 열면 된다. 터미널에서 빌드할 때는 `JAVA_HOME`을 Android Studio의 JBR로 지정한다(GraalVM은 jlink 단계에서 실패한다).

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```
