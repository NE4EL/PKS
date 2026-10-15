# Gson читает DTO через reflection; сохраняются только имена транспортных моделей.
-keep class ru.mirea.studenthub.android.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn javax.annotation.**
