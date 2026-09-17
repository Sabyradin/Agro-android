# Agroland Android — R8 ережелері (Flutter android/app/proguard-rules.pro parity).

# TikTok Business Android SDK (Фаза 19) — Flutter-дегі бірдей keep.
-keep class com.tiktok.** { *; }

# TikTok SDK опционал тәуелділіктері (billing client / install referrer) —
# біздің қосымшада bundle емес, R8 «Missing class»-ын ренжітпеу керек.
-dontwarn com.android.billingclient.**
-dontwarn com.android.installreferrer.**

# Firebase — плейсхолдер конфигте де crash болмасын (guard-пен қоса).
-keep class com.google.firebase.** { *; }

# kotlinx.serialization — @Serializable модельдер рефлексия арқылы емес,
# генерланған serializer арқылы жүреді; JSON кілттері статикалық.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**