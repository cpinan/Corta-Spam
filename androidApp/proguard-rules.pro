# R8 rules for the release build.
#
# Most of what this app does survives shrinking untouched. The exceptions are the places where
# something outside our code decides what to instantiate or what a name means:

# kotlinx.serialization generates a synthetic $$serializer for every @Serializable class and
# looks it up reflectively. BackupData and BlockReason are both serialized, and BlockReason ends
# up in the database -- a renamed @SerialName would make existing call-log rows undecodable.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    public static ** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Telecom instantiates these by name from the manifest.
-keep class org.carlospinan.bloqueador.app.telecom.PassthroughInCallService { *; }
-keep class org.carlospinan.bloqueador.app.telecom.CallActionReceiver { *; }
-keep class org.carlospinan.bloqueador.app.telecom.InCallActivity { *; }
-keep class org.carlospinan.bloqueador.app.MainActivity { *; }
-keep class org.carlospinan.bloqueador.app.CortaSpamApp { *; }

# SQLDelight's generated database and the driver load implementations reflectively.
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# Koin resolves bindings by type; keep the constructors it calls.
-keepclassmembers class * {
    @org.koin.core.annotation.* *;
}

# Everything below is a "this dependency references classes that don't exist on Android"
# suppression, not a keep rule.
-dontwarn org.slf4j.**
-dontwarn java.lang.instrument.**
