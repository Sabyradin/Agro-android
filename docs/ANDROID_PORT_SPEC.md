# Agroland Android — Native Kotlin/Compose порт спецификациясы

**Мақсат:** Agroland мобиль қосымшасын Android үшін **нативті** (Kotlin + Jetpack Compose) жазып шығу. Толық функционалдық паритет, нативті UX, Flutter-ге тәуелсіз.

**Күні:** 2026-09-14 · **Автор:** Agroland (iOS нативті порт негізінде) · **Тіл:** kk/ru/en/zh

---

## 1. Контекст және дереккөздер

| Не | Қайда | Рөлі |
|---|---|---|
| Flutter қосымшасы (прод) | `~/code/agro/Agroland/mobile-app` (v1.0.43+107, 29 модуль, 65 экран) | **Мінез-құлықтың эталоны** — күмән болса, Dart кодына қара |
| iOS нативті порт (дайын) | `~/code/agro/Agroland/mobile-ios` (SwiftUI, 2026-09) | **Референс имплементация** — барлық backend қырсықтары осында шешілген |
| Backend | `~/code/agro/Agroland/backend` (FastAPI) | API дереккөзі, `src/app/route.py` — толық маршрут кестесі |
| Толық модуль картасы | `docs/SWIFT_REWRITE_SPEC.md` (23 модуль, 65 экран, ~220 эндпоинт) | **Міндетті оқу** — экран/эндпоинт тізімдері сонда |
| Подсистема карталары | `docs/SWIFT_REWRITE_MAPS.json` | Әр подсистеманың терең сипаттамасы |

> Бұл файл — Android-ға арналған **қосымша қабат**: стек, модуль құрылымы, фазалар, қабылдау критерийлері және iOS портында табылған нақты қателіктер. Экран/эндпоинт тізімін `SWIFT_REWRITE_SPEC.md`-тен ал.

**Backend base URL:**
- prod: `https://backend-237397542353.europe-central2.run.app/api/v1`
- dev: `https://backend-test-42ygumvdeq-lm.a.run.app/api/v1`

---

## 2. Техникалық стек (міндетті)

| Қабат | Таңдау | Ескерту |
|---|---|---|
| Тіл / JDK | Kotlin 2.1+, JDK 17 | Flutter жобасында да jvmTarget 17 |
| UI | Jetpack Compose (BOM соңғы тұрақты) + Material 3 | XML layout **жоқ** |
| Навигация | Navigation Compose, type-safe routes (`@Serializable` route объектілері) | iOS-тағы `Route` enum-ының баламасы |
| DI | Hilt | |
| Желі | Retrofit 2 + OkHttp 5 + kotlinx.serialization | Interceptor тізбегі — §4 |
| Күй | ViewModel + `StateFlow` + immutable UI-state data class | MVI емес, қарапайым MVVM жеткілікті |
| Пагинация | Paging 3 (немесе қолмен — тізімдер шағын) | |
| Сурет | Coil 3 | |
| Локалды сақтау | DataStore Preferences (баптаулар) + `EncryptedSharedPreferences`/Keystore (токендер, PIN хэші) | |
| Socket | Engine.IO v4 / Socket.IO v5 — `io.socket:socket.io-client:2.1.x` **немесе** OkHttp WebSocket үстіне өз клиент | iOS-та өз клиент жазылды (`SocketIOClient.swift`), протокол §6-да |
| WebRTC | `io.getstream:stream-webrtc-android` | |
| Қоңырау UI | Telecom `ConnectionService` + full-screen intent + `CallStyle` notification | iOS CallKit баламасы |
| Push | Firebase Cloud Messaging + notification channels | flavor-ға қарай `google-services.json` |
| Аудио | `MediaRecorder` (m4a AAC-LC 44.1kHz mono) + Media3 `ExoPlayer` | дауыстық хабарлама, қоңырау дыбыстары |
| Камера / QR | CameraX + ML Kit Barcode Scanning | |
| Карта | Google Maps Compose (`maps-compose`) + `FusedLocationProviderClient` | |
| Диаграмма | Vico (немесе Compose Canvas) | дилер аналитикасы |
| Биометрия | `androidx.biometric` | app-lock |
| Фон тапсырмалары | WorkManager | push реплей, кезекке тұрған жіберулер |

**Gradle:** AGP 8.x, version catalog (`libs.versions.toml`), Kotlin DSL.
**minSdk 25, targetSdk 36, compileSdk 36**, abiFilters `arm64-v8a`, `armeabi-v7a` (Flutter нұсқасымен бірдей).

---

## 3. Жоба құрылымы

```
agroland-android/
├─ app/                        # Application, MainActivity, навигация графы, DI хосты
├─ core/
│  ├─ network/                 # Retrofit, interceptor тізбегі, Failure, JSON helpers
│  ├─ auth/                    # токен қоймасы, refresh, app-lock, биометрия
│  ├─ ui/                      # дизайн-жүйе: түстер, типографика, ортақ компоненттер
│  ├─ l10n/                    # strings.xml (kk/ru/en/zh), locale→backend мэппинг
│  └─ common/                  # форматтаушылар (баға, күн, сан), валидаторлар
└─ feature/
   ├─ home/  announcements/  create/  search/  favorites/
   ├─ auth/  profile/  company/  verification/
   ├─ cart/  checkout/  orders/  payments/  wallet/
   ├─ chat/  call/  notifications/
   ├─ dealer/  promo/  stories/  reviews/  china/  services/  demand/
   └─ media/                   # фото/видео/PDF/QR көрсеткіштері
```

**Flavors:** `dev` (applicationId `com.agroland.app.dev`, «AgroLand Dev») және `prod` (`com.agroland.app`) — Flutter нұсқасындағыдай. Әр flavor-дың өз `google-services.json` файлы (`app/src/dev/`, `app/src/prod/`) бар, оларды `mobile-app/android/app/src/{dev,prod}/` ішінен алуға болады.

---

## 4. Желі қабаты (ең маңызды бөлік)

**Interceptor реті (осы ретпен!):** `Auth (401 refresh)` → `Monitoring` → `Retry` → `TariffLimit`.

1. **Auth / refresh:** 401 келгенде `POST /auth/refresh`. Refresh токен **бір реттік**, сондықтан қатар жүрген 401-дер **бір ғана** refresh сұрауын бөлісуі керек (Mutex + бөлісілетін `Deferred`). Сәтсіз болса — толық sign-out. `/auth/refresh` мен `DELETE /device` 401-дерінде refresh жасалмайды.
2. **Retry:** тек GET, макс 2 рет, 800ms × 2^(n-1), тек timeout/connection қателерінде.
3. **TariffLimit:** 403 + `error_code` `TARIFF_LIMIT_` префиксімен → бүкіл қосымша деңгейінде «тарифті жаңарту» диалогы.
4. **Monitoring:** әр эндпоинт бойынша сәттілік/қате оқиғасын fire-and-forget жібереді.

**Қате моделі** — `sealed interface Failure { Server, Connection, Local, Fatal }`. Backend денесі: `{error_code, message, balance, required, missing}` және ішкі `{error: {error_code, feature, current, limit}}`. Дене мүлде JSON болмауы да мүмкін (кәдімгі жол) — парсер **кешірімді** болуы керек.

**Қырсықтар (iOS портында тексерілген):**
- Backend жауаптары тұрақсыз типті: бір өріс бірде сан, бірде жол, бірде объект болады. iOS-та `JSON` деген кешірімді enum жазылды — Android-та да `JsonElement` үстінен `.asIntOrNull()` тәрізді көмекшілер жаса, қатаң `@Serializable` data class-қа тікелей парсуға сенбе.
- `GET /cart` ішінде **`announcement` объектісі жоқ** — тек `title`/`main_image_url`. Жеткізу аймақтары, `pickup_available`, `pickup_address` керек болса, әр элемент үшін `GET /announcement/{id}` шақырып толықтыр (iOS: `CartStore.load`).
- Кейбір эндпоинттер `items` массивін, кейбірі тікелей массив қайтарады — екеуін де қолда.
- Base URL мен path екеуі де `/api/v1` ұстайды (Flutter-дегі баг) — бір ғана жерде префикс болсын.

---

## 5. Локализация және дизайн-жүйе

- Тілдер: **kk (әдепкі)**, ru, en, zh. ~1100 кілт. Flutter ARB файлдары: `mobile-app/lib/l10n/*.arb`; iOS-та олар `Localizable.xcstrings`-ке көшірілген (`mobile-ios/agroland/Resources/`).
- ARB ішінде **ICU plural** жолдары бар (`{n, plural, one{...} few{...} many{...} other{...}}`) — Android `<plurals>` ресурсына айналдыр (ru/kk үшін `one/few/many/other` дұрыс болсын).
- **Backend локаль мэппингі (кризистік маңызды):** `kk → kz`, `zh → ch`. Backend `names: {ru, kz, en, ch}` объектілерін қайтарады; дұрыс кілтті таңдамасаң, атаулар бос шығады.
- Тіл socket auth-та (`language`) және push құрылғысын тіркеуде де жіберіледі.
- Түс токендері: primary `#147F26`, primaryLight `#7AB30E`, accent `#FFCC00`, error `#C20B0B`, text1 `#222222`, text2 `#939393`, background `#F9F9F9` + қараңғы тема жиынтығы. Material 3 `ColorScheme` + қосымша токендер (`divider`, `card`, `secondaryText`) — `CompositionLocal` арқылы бер.
- Шрифт: **OpenSans** (12 стиль) — `res/font` арқылы. Тема режимі (system/light/dark) DataStore-да сақталады.

---

## 6. Chat (Socket.IO) — ең қауіпті подсистема

Транспорт: **websocket only**, Engine.IO v4 / Socket.IO v5.
- Қосылу: `40{"Authorization":"Bearer <token>","language":"kk"}`
- Оқиға: `42["event_name", payload]`, heartbeat `2`/`3`.
- Шексіз қайта қосылу (backoff), қосылмаған кезде эмиттерді буферге жина.

**Оқиғалар:** `connect_chat_success`, `send_updated_chat_list`, `join_chat` → `join_chat_success {room_id, messages, announcement_id}`, `chat_message`, `edit_message`, `mark_chat_as_read`, `typing_start/stop`, `load_older_messages`, `chat_deleted`, `chat_history_cleared`, `message_deleted`, `message_edited`, `message_read`, `user_online/offline`, және `call:*` (§7).

**iOS портында анықталған нақты мінез-құлықтар:**
- Backend аутентификация қатесін `CONNECT_ERROR` арқылы емес, **кәдімгі** `42["connect_error", …]` оқиғасы + disconnect ретінде жібереді.
- Чат тізімінде `sender_id` **әрқашан ағымдағы пайдаланушы** болып келеді → «Сіз:» префиксін шығаруға болмайды.
- `is_checked` = «менің соңғы хабарым оқылды ма», **верификация белгісі емес**.
- Хабар типі `message_type: "text"` болып келсе де, URL кеңейтіміне қарай image/video/audio/file/location деп **клиент анықтайды** (`infer_message_type.dart` 1:1 портталуы керек).
- URL ішінде literal CR/LF болады (backend багы) — тазалау керек.
- Оптимистік жіберу: `localId` → сервер эхосымен алмастыру; қайта қосылғанда кезектегі мәтіндерді жіберу; `join_chat` экспоненциалды қайталау 4/8/16s.
- Жүйелік чаттар: 31 (оператор), 1001–1005 (жаңалық, гид, маркет т.б.) — арнайы аватар/иконка, тек оқуға арналғандары бар.

---

## 7. Дауыстық қоңырау (WebRTC)

- Сигналинг — сол socket арқылы: `call_invite/accept/reject/hangup/offer/answer/ice_candidate` → `call:incoming/invite_ack/accepted/rejected/hungup/peer_offer/peer_answer/peer_ice`.
- TURN: `GET /turn/credentials` (5 мин кэш) → сигналингтен келген `ice_servers` → конфигтегі TURN → соңында STUN-only.
- ICE restart: тек offerer жағында, бір рет, 20с fallback; ерте келген ICE кандидаттарын буферге жина.
- Кіріс қоңырау UI: Telecom `ConnectionService` (немесе full-screen intent + `CallStyle`), `AudioManager` режимі `MODE_IN_COMMUNICATION`, динамик/құлаққап маршруты, `RECORD_AUDIO` рұқсаты.
- Өткізіп алған қоңыраулар FCM арқылы келеді + `deliver_pending_calls` реплейі бар.

---

## 8. Төлемдер

- **Halyk ePay:** тапсырыс → `invoice_url` → Custom Tabs/WebView → `GET /orders/{id}` `payment_status` поллингі (2с, 60с лимит). Сәтті болғанда TikTok `trackPurchase`.
- **Баланс арқылы төлеу:** 95/5 бөлінісі backend жағында.
- **BCC (legacy) 3D Secure:** WebView + HMAC-SHA1 `P_SIGN`.
- Секреттер бос болса — mock режим (қосымша құлап қалмауы керек).
- Қосымша жабылып қалса: күтіп тұрған тапсырыс id-і DataStore-да сақталып, келесі ашылуда төлем нәтижесі экраны қалпына келеді.
- Төлем сұрауларында `X-Platform: android` тақырыбы жіберілсін (iOS-та `ios`).

---

## 9. Себет / чекаут — iOS портында түзетілген нәрселер

- `GET /cart/preview` **бүкіл себетті** есептейді, тек таңдалған тауарларды емес → ескертулерді (`warnings`) ағымдағы чекаут элементтеріне сүзу керек.
- `warnings` — әзірлеушіге арналған ағылшын жолдары (`cart_item 70: no delivery zone available for the chosen district`). Оларды пайдаланушыға **шикі күйінде көрсетуге болмайды**: `cart_item {id}` → тауар атауы, мәтін → локализацияланған хабар. Үш нұсқасы бар: жеткізу аймағы жоқ / таңдалған аймақ байланбаған / тауар қолжетімсіз.
- `pickup=true` жіберілсе, backend `pickup_address`-ты **міндетті** етеді (`PICKUP_ADDRESS_REQUIRED`). Сондықтан «Өзі алу» опциясын тек `pickup_available == true && pickup_address != null` болғанда ғана ұсын; екеуі де жоқ болса, төлем батырмасын бұғаттап, себебін көрсет.
- Тапсырыс күйі (`status`) мен төлем күйі (`payment_status`) — **бір-біріне тәуелсіз**.
- Бір Halyk тапсырысы = бір жеткізуші (multi-supplier болса `MULTI_SUPPLIER_CART` қатесі → жеткізушіні таңдау парағы).

---

## 10. Пікірлер (жаңа мінез-құлық, iOS-та қосылды)

Backend-те «менің пікірлерім» эндпоинті **жоқ**. iOS-та ол клиент жағында жиналады және **Чат** қойындысының басындағы «Менің пікірлерім» жолынан ашылады:
- үміткерлер = сатып алушының тапсырыстары (`GET /orders/?role=buyer`) + жергілікті «қаралған жарнамалар» тізімі;
- әрқайсысы үшін `GET /reviews/{announcement_id}` → ішінде өз `user_id`-і бар пікір бар ма;
- екі қойынды: «Қалдырылмаған» (ішінде «Пікір қалдыру» батырмасы) және «Қалдырылған» (жұлдыз + мәтін + күн);
- чат тізіміндегі жолда «қалдырылмаған» саны бейдж ретінде көрінеді.

Android-та дәл осылай қайталау керек (файлдар: `mobile-ios/agroland/Features/Reviews/MyReviewsStore.swift`, `MyReviewsView.swift`).

---

## 11. Push, deep-link, тіркеу

- FCM токені → `POST /device` (тіл, платформа, құрылғы id). Sign-out кезінде `DELETE /device`.
- Notification channels: чат, тапсырыс, қоңырау (full-screen), маркетинг.
- Deep-link бағыттау: чат → бөлме, `order_id` → тапсырыс, `announcement_id` → жарнама, `notification_id+type` → хабарламалар тізімі, verification/balance/review_request.
- Қосымша **өлі** күйде push басылса, әрекет сақталып, келесі ашылуда орындалуы керек.
- Бейдж/оқылмаған сан — чат + хабарламалар қосындысы.

---

## 12. Фазалар (ұсынылған тәртіп)

| # | Фаза | Күтілетін нәтиже |
|---|---|---|
| 1 | Қаңқа + дизайн-жүйе | Compose тема, түс/шрифт токендері, ортақ компоненттер, 4 таб + ортадағы «+» |
| 2 | Желі + күй инфрақұрылымы | Retrofit, interceptor тізбегі, Failure, токен қоймасы, flavors |
| 3 | Auth + app-lock | Телефон + OTP, MFA (HATEOAS `task_id`+links), PIN (SHA-256+salt), биометрия |
| 4 | Профиль, компания, KYC | Профиль/компания өңдеу (multipart), верификация құжаттары |
| 5 | Маркетплейс (оқу) | Категориялар, лента (VIP 4+2 араластыру), іздеу (debounce 500ms), сүзгі, таңдаулылар |
| 6 | Маркетплейс (жазу) | Жарнама құру/өңдеу (multipart images[]+video), тариф шектеуі, AI мәтін |
| 7 | Локация / карта | Google Maps, reverse geocode → каталог ID-лері, ел/облыс/аудан таңдау |
| 8 | Себет / чекаут / тапсырыстар | Smart Calculator preview, §9 ережелері, тапсырыс тарихы, timeline |
| 9 | Төлемдер | Halyk ePay, баланспен төлеу, BCC 3DS, poll + resume |
| 10 | Әмиян | Баланс, hold, транзакциялар, шығару (мин 5000 ₸, IBAN/BIK/BIN) |
| 11 | Push | FCM, каналдар, deep-link, өлі күй реплейі |
| 12 | **Чат** | §6 толығымен — ең көп уақыт осында кетеді |
| 13 | **Қоңырау** | §7 толығымен — тәуекелі ең жоғары |
| 14 | Stories, баннерлер | Толық экранды сторис (5с прогресс, ұзақ басу — пауза, свайп жабу) |
| 15 | Промо v2 | Backend басқаратын каталог (`activate_endpoint` + метод диспетчері, 409 → PATCH) |
| 16 | Дилер консолі | Тауарлар, тапсырыстар, жеткізу аймақтары, қызметкерлер, аналитика |
| 17 | Қытай (MercuryX), сұраныс, EGOV | Прокси каталог, телефон 11 цифр/BIN 12 цифр валидациясы, VIN `^[A-HJ-NPR-Z0-9]{17}$` |
| 18 | Пікірлер, медиа, QR | §10 + фото/видео/PDF көрсеткіштері, QR сканер |
| 19 | Аналитика | Firebase Analytics/Crashlytics, TikTok Business SDK (Android нұсқасы) |
| 20 | Полиш + релиз | Force-update, рұқсаттар, Play Console (AAB, signing, dev/prod flavors) |

**Бағалау:** тәжірибелі бір Android әзірлеуші үшін ~10–14 апта (чат + қоңырау ≈ 4 апта). Екеу болса ~7–9 апта.

---

## 13. Қабылдау критерийлері (Definition of Done)

1. 23 модуль, 65 экран — Flutter нұсқасымен функционалдық паритет; «кейін жасаймыз» деген бос экран (stub) **жоқ**.
2. Барлық мәтін 4 тілде, ешбір жерде хардкод жол жоқ (lint-пен тексеріледі).
3. Қараңғы тема барлық экранда дұрыс, кішкентай экранда (360dp) да, планшетте де сынбайды.
4. Backend қателері пайдаланушыға адам түсінетін тілде көрсетіледі (шикі `error_code` / ағылшын техникалық мәтін көрінбейді).
5. Чат: жіберу/қабылдау, медиа, дауыстық хабар, оқылды белгісі, typing, ескі хабарларды тарту, қайта қосылу — нақты құрылғыда тексерілген.
6. Қоңырау: кіріс/шығыс, фонда, экран құлыптаулы кезде, өткізіп алған қоңырау пушы — нақты екі құрылғыда тексерілген.
7. Төлем: Halyk тест картасымен толық цикл + қосымша жабылып қалғанда нәтиже қалпына келуі.
8. Push: 4 deep-link түрі, өлі күйден ашылу.
9. Unit-тестілер: форматтаушылар, валидаторлар, хабар типін анықтау, ICU plural, Failure парсері, себет ескертулері (iOS-та 25 тест бар — соларды қайтала).
10. `dev` және `prod` flavor-лар бөлек орнатылады, әрқайсысы өз Firebase жобасымен; release AAB Play Console-ге жүктеуге дайын.

---

## 14. Жұмыс тәртібі

- Репозиторий: жаңа **private** repo (`agroland-android`), `main` қорғалған, әр фаза = бөлек PR.
- Әр PR-де: не істелді, қай экрандар, скриншот (light+dark), тексерілген құрылғы/API деңгейі.
- Backend-ті өзгертуге болмайды — қажет болса Agroland командасына сұраныс жаз (мысалы «менің пікірлерім» эндпоинті қосылса, §10 жеңілдейді).
- Дизайн: Flutter қосымшасының макеті негіз, бірақ **нативті Material 3 мінез-құлқы** артық көрінеді (нативті back-gesture, ripple, нативті пикерлер, edge-to-edge).
- Күмәнді жағдайда: 1) Flutter коды, 2) iOS Swift коды, 3) backend коды — осы ретпен қара.
