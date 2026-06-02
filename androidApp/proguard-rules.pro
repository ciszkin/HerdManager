# Keep Presenter's constructors
-keepclassmembers class * extends androidx.compose.ui.platform.ComposeView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep data classes
-keepclassmembers class by.ciszkin.herdmanager.** {
    public <init>(...);
}

# Keep serialization
-keepattributes InnerClasses
-keepattributes Signature
-keepclassmembers class by.ciszkin.herdmanager.**$** {
    public static final kotlinx.serialization.json.Json$Default **;
}
