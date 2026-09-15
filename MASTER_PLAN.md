# Agroland Android — Мастер-жоспар (20 фаза)

**Мақсат:** Agroland маркетплейсін Android үшін нативті (Kotlin + Compose) толық порттау — Flutter-ге тәуелсіз, 65 экран, ~220 эндпоинт, 4 тіл.

**Дереккөздер:** `docs/ANDROID_PORT_SPEC.md` (стек/қырсықтар), `docs/SWIFT_REWRITE_SPEC.md` + `docs/SWIFT_REWRITE_MAPS.json` (модуль/экран/эндпоинт картасы). Мінез-құлық эталоны — Flutter (`mobile-app`), референс — iOS (`mobile-ios`); олар бұл машинада жоқ, сондықтан спек-документтер — бірінші дереккөз, күмән жерлер ISSUES.md-ке жазылады.

**Тәртіп:** фазаны аяқтау = компиляция таза + `./gradlew test` жасыл + құрылғы/эмулятор + light/dark. Әр фаза = commit. Stub экран жоқ; backend өзгертілмейді; PROD-қа тимейді.

---

## Статус

| # | Фаза | Мазмұны | Статус |
|---|------|---------|--------|
| 1 | Қаңқа + дизайн-жүйе | Gradle қаңқа, flavors, тема/түс/қаріп токендері, ортақ компоненттер, 5-таб shell + «+», Splash, Тіл | ✅ |
| 2 | Желі + күй инфрақұрылымы | Retrofit/OkHttp, interceptor тізбегі (Auth single-flight 401 → Monitoring → Retry → TariffLimit), Failure, кешірімді JSON парсер, TokenStore, AppConfig, URL resolver | ✅ |
| 3 | Auth + app-lock | Телефон+OTP HATEOAS (task_id+links), signup individual/dealer, PIN SHA-256+salt, биометрия, DealerTerms гейт | ✅ ( DealerTerms гейт — фаза 4-ке қалды) |
| 4 | Профиль, компания, KYC | Профиль хаб, өңдеу (additive PATCH, avatar multipart), мекенжай CRUD, компания саб-ресурстары, VerificationPage | ✅ |
| 5 | Маркетплейс (оқу) | Категориялар (6 bucket), лента (VIP 4+2 аралас), recommended (order_random+country_id=4), іздеу debounce 500ms, сүзгі, таңдаулылар, деталь | ✅ |
| 6 | Маркетплейс (жазу) | Create/Preview/Edit (multipart images+video), activate/deactivate/delete, bulk-upload, AI ad-content, MakeOffer, status беттері | ✅ |
| 7 | Локация / карта | Картамен таңдау, reverse geocode → каталог ID, Country/Region/District селекторлары, app-region persist | ✅ |
| 8 | Себет / чекаут / тапсырыстар | CartPage (оптимистік qty), Smart Calculator preview (warnings сүзу+локализация), SupplierPicker, Checkout, OrderDetail+timeline, тарих қойындылары | ⬜ |
| 9 | Төлемдер | Halyk ePay (invoice_url+poll 2с/60с), баланспен төлеу, BCC 3DS (HMAC-SHA1 P_SIGN), mock режим, pending resume, X-Platform: android | ⬜ |
| 10 | Әмиян | Balance, hold, транзакциялар, шығару (мин 5000₸, IBAN/BIK), /business/* жолдар | ⬜ |
| 11 | Push | FCM, каналдар, POST /device, deep-link 4 түрі, өлі күй реплейі, бейдж | ⬜ |
| 12 | Чат | Socket.IO client (websocket-only, auth Bearer+language), тізім (merge, other_user_name, жүйелік чат 31/1001-1005), бөлме (join retry 4/8/16, оптимистік localId, infer_message_type 1:1, CR/LF санитайзер, typing, load_older, дауыстық, медиа) | ⬜ |
| 13 | Қоңырау | stream-webrtc-android, call:* сигналинг, TURN кэш 5мин, ICE restart (offerer, 1 рет), Telecom ConnectionService + full-screen intent + CallStyle, missed-call реплей | ⬜ |
| 14 | Stories, баннерлер | BannerStoryViewer (5с, прогресс, пауза, свайп), StoriesListView, markViewed (auth), маркетинг клик/көру, 4 статикалық промо | ⬜ |
| 15 | Промо v2 | Catalog (activate_endpoint диспетчер, 409→PATCH), AdvertiseAd, PromoBannerForm (multipart), CurrentStatus chips, Promoted/Hot ленталары | ⬜ |
| 16 | Дилер консолі | Products/Orders табтары, delivery-zones CRUD, қызметкерлер, TeamPool claim, аналитика (timeseries, Vico) | ⬜ |
| 17 | Қытай + сұраныс + EGOV | MercuryX каталог/себет/тапсырыс (consent, phone 11д 77…, BIN 12д, Int64), Demand CRUD, VIN `^[A-HJ-NPR-Z0-9]{17}$` | ⬜ |
| 18 | Пікірлер, медиа, QR | «Менің пікірлерім» (клиент жағынан, 2 қойынды+бейдж), seller/announcement пікірлері, Photo/Video/PDF/WebView/YouTube viewer, QR сканер | ⬜ |
| 19 | Аналитика | Firebase Analytics/Crashlytics, TikTok Business SDK (Android нұсқасы), мониторинг оқиғалары, search/log | ⬜ |
| 20 | Полиш + релиз | Force-update (GET /app-version), рұқсаттар, edge-to-edge, Play Console дайындық (AAB, signing), соңғы тексеру | ⬜ |

---

## Фаза аяқталғанда толтырылатын журнал (кезекте)

_(фаза commit хабарламаларында: не істелді, қай экрандар, қандай эндпоинттер, нені тексердім)_

## Тығыздық-ережелер (кез кезген фазада ұмытылмайды)

1. Мәтіндер — strings.xml-те (kk әдепкі, ru/en/zh); ICU plural → `<plurals>`. Backend локалі: kk→kz, zh→ch.
2. Қате хабарламасы — ешқашан шикі error_code/ағылшын техникалық мәтін UI-ға шықпайды.
3. user_type API-де `dealer`/`business`, UI-да «Бизнес»; `/business/*` target.
4. URL толықтау — бір ғана resolver (core:network).
5. Refresh токен single-flight; `items` массиві немесе тікелей массив — екеуі де қолдауда.
6. Fulfillment status ↔ payment_status тәуелсіз.
7. Тек DEV ортада тест; PROD — тек оқу.
8. Күмән → ISSUES.md; backend-керек → BACKEND_REQUESTS.md.