# Checkstyle — S08-G4 static analysis

Blocking quality rules for every Java subproject. Wired into Gradle `check`
(and therefore `application-ci.yml` via `./gradlew build`).

| Piece | Role |
|---|---|
| `checkstyle.xml` | The rule set, including CODING-STANDARD §7 Naming (EPIC-007 / QA-015). Failures are errors (`maxWarnings = 0`). |
| `suppressions.xml` | Tracked baseline for pre-existing violations (S08-E02-S03; MechanicallyGenerated `$` names and Flyway `V{n}__*` types under QA-015). |
| Spotless (root `build.gradle.kts`) | Formatting half. Google Java Format on every Java file. No ratchet. |

```bash
./gradlew checkstyleMain checkstyleTest spotlessCheck
# demonstrate a breach: introduce a star import, then
./gradlew :libs:bank-common-error:checkstyleMain   # must fail
```
