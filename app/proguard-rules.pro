# Keep wallpaper service
-keep class com.saithanyam.wallpaper.service.SaithanyamWallpaperService { *; }

# Compose — keep @Composable lambdas from being removed
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Kotlin metadata needed by Compose compiler
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
