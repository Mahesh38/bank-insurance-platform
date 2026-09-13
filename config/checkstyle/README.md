# Checkstyle — S08-G4 static analysis

Blocking quality rules for every Java subproject. Wired into Gradle `check`
(and therefore `application-ci.yml` via `./gradlew build`).

| Piece | Role |
|---|---|
| `checkstyle.xml` | The rule set. Failures are errors (`maxWarnings = 0`). |
| `suppressions.xml` | Tracked baseline for pre-existing violations (S08-E02-S03). Empty at introduction. |
| Spotless (root `build.gradle.kts`) | Formatting half; `ratchetFrom("origin/main")` so only touched files must be clean. |

```bash
./gradlew checkstyleMain checkstyleTest spotlessCheck
# demonstrate a breach: introduce a star import, then
./gradlew :libs:bank-common-error:checkstyleMain   # must fail
```
