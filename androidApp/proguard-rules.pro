# --- Compose ---

# ComposeView and its constructors are loaded reflectively by the runtime.
-keepclassmembers class * extends androidx.compose.ui.platform.ComposeView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# --- Kotlinx Serialization ---
# Keep serializers for all @Serializable classes (including generated ones).
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static final kotlinx.serialization.KSerializer$Companion Companion;
}
-keep,includedescriptorclasses class *$$serializer { *; }
-keepclassmembers class *$$serializer {
    long serialVersionUID;
    static ** INSTANCE;
}
-keep,includedescriptorclasses class by.ciszkin.herdmanager.**$$serializer { *; }

# --- Koin (Dependency Injection) ---
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }

# --- App ---
# Keep all app classes so constructor-injected Koin components and data
# classes (used via serialization/reflection) are never stripped.
-keep class by.ciszkin.herdmanager.** { *; }