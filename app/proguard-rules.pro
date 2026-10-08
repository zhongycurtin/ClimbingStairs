# Proguard rules for StairStep
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
