# Agroland Android — AI агентке (Claude Code / Cursor) арналған промпт

> Төмендегі мәтінді тұтас көшіріп, Android репозиторийінің түбінде ашылған Claude Code сессиясына бер.
> Алдымен `docs/ANDROID_PORT_SPEC.md`, `docs/SWIFT_REWRITE_SPEC.md`, `docs/SWIFT_REWRITE_MAPS.json` файлдарын сол репоға көшір (немесе `agroland` монорепосын қатар клонда).

---

## PROMPT (копипаст)

Сен — тәжірибелі Android әзірлеушісің (Kotlin + Jetpack Compose). Тапсырма: **Agroland** маркетплейс қосымшасын Android үшін нативті түрде жазып шығу — толық функционалдық паритетпен, Flutter-ге тәуелсіз.

### Дереккөздер (қатаң осы ретпен қара)
1. `docs/ANDROID_PORT_SPEC.md` — стек, модуль құрылымы, фазалар, қабылдау критерийлері, backend қырсықтары. **Бірінші осыны толық оқы.**
2. `docs/SWIFT_REWRITE_SPEC.md` + `docs/SWIFT_REWRITE_MAPS.json` — 23 модуль, 65 экран, ~220 эндпоинттің толық картасы.
3. `mobile-app/` (Flutter, прод) — мінез-құлықтың эталоны. Күмән болса, Dart кодын оқы.
4. `mobile-ios/` (SwiftUI, дайын нативті порт) — референс имплементация: бірдей модельдер, бірдей ағындар, backend қателерінің шешімдері сонда тұр.
5. `backend/src/app/route.py` — маршруттардың толық кестесі; нақты жауап пішімін API кодынан тексер.

### Техникалық талаптар
- Kotlin 2.1+, JDK 17, Compose + Material 3, Navigation Compose (type-safe routes), Hilt, Retrofit+OkHttp+kotlinx.serialization, Coil 3, DataStore + EncryptedSharedPreferences, Paging 3, Media3, CameraX+ML Kit, WebRTC (`stream-webrtc-android`), FCM.
- minSdk 25, targetSdk 36, flavors `dev` (`com.agroland.app.dev`) / `prod` (`com.agroland.app`), әрқайсысының `google-services.json`-ы бөлек.
- Архитектура: feature-модуль → ViewModel + StateFlow + immutable UI state; желі қабаты `core/network`; XML layout жоқ.
- Барлық мәтін `strings.xml`-де (kk әдепкі, ru/en/zh), ICU plural → `<plurals>`; backend локалі: `kk→kz`, `zh→ch`.

### Жұмыс тәртібі
- `docs/ANDROID_PORT_SPEC.md` §12-дегі 20 фазаны **ретімен** орында. Әр фазаның соңында: компиляция таза, `./gradlew test` жасыл, эмуляторда/құрылғыда тексерілген, скриншот (light+dark).
- Әр фаза = бөлек commit/PR, сипаттамасында: не істелді, қай экрандар, қандай эндпоинттер, нені тексердім.
- **Stub жазба.** Экран жасалса — нақты API-мен жұмыс істейтін, тірі деректі көрсететін болсын. Уақыт жетпесе, фазаны бөл, бірақ жартылай экран қалдырма.
- Backend кодын өзгертпе. Эндпоинт жетіспесе — соны айт және клиент жағында шешім тап (мысалы «менің пікірлерім» §10).
- Қателерді пайдаланушыға адам тілінде көрсет: шикі `error_code` немесе ағылшын техникалық мәтін UI-ға шықпайды.

### Ерекше назар (iOS портында қанмен жазылған сабақтар)
1. **Чат (§6)** — ең қиын бөлігі. `message_type: "text"` келгенімен, хабар типін URL-ден клиент анықтайды. `sender_id` әрқашан ағымдағы пайдаланушы — «Сіз:» деп жазба. `is_checked` = менің хабарым оқылды. Backend `connect_error`-ды қарапайым оқиға ретінде жібереді. URL ішінде CR/LF болады — тазала.
2. **Қоңырау (§7)** — ICE restart тек offerer жағында бір рет; TURN креденшелдері 5 минут кэш; кіріс қоңырау Telecom/full-screen intent арқылы.
3. **Себет (§9)** — `/cart/preview` бүкіл себетті есептейді, ескертулерді сүз және локализацияла; `pickup=true` үшін `pickup_address` міндетті.
4. **Желі (§4)** — refresh токен бір реттік: қатар келген 401-дер бір ғана refresh сұрауын бөлісуі керек.
5. **JSON** — backend типтері тұрақсыз (бірде сан, бірде жол). Кешірімді парсер жаз.

### Бірінші қадам
Ештеңе жазбас бұрын: `docs/ANDROID_PORT_SPEC.md` мен `docs/SWIFT_REWRITE_SPEC.md`-ті оқы, содан кейін **Фаза 1 жоспарын** (файл ағашы, тәуелділіктер, бірінші экрандар) қысқаша ұсын. Мақұлдағаннан кейін ғана код жаз.

---

## Әзірлеушіге қосымша ескертпе (адамға)

- Backend dev ортасы: `https://backend-test-42ygumvdeq-lm.a.run.app/api/v1` — тест аккаунт пен телефон нөмірін Agroland командасынан ал.
- Flutter қосымшасының keystore/Firebase жобалары бар — жаңа қосымша **сол applicationId-ді** (`com.agroland.app`) иемденеді, сондықтан релиз алдында signing кілттері мен Play Console қатынасы Agroland жағынан беріледі.
- TikTok Business SDK Android нұсқасы бар (`mobile-app/android/.../TikTokBusinessChannel.kt` — көшіруге болады).
- Сұрақтарды осы чатқа жаз: экран макеті, API мінез-құлығы, приоритет — бәрі талқыға ашық.
