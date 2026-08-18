# ===================================================================
# ===              R8 / ProGuard pravila za BrainTrainer          ===
# ===================================================================
# Release build koristi isMinifyEnabled + isShrinkResources, pa sve što se
# koristi refleksijom ili preko imena mora ovde eksplicitno da se sačuva.

# --- Čitljivi stack trace-ovi u Play Console -----------------------
# Bez ovoga su prijavljeni padovi praktično neupotrebljivi.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization -----------------------------------------
# Zagonetke se parsiraju iz assets JSON-a preko generisanih serijalizatora.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.program.braintrainer.**$$serializer { *; }

# --- Model klase ----------------------------------------------------
# Problem/Solution/Module/Difficulty se deserijalizuju po imenu polja.
-keep class com.program.braintrainer.chess.model.** { *; }

# --- Enumi ----------------------------------------------------------
# Imena enum konstanti se čuvaju kao stringovi:
#   - Module/Difficulty u navigacionim rutama (Module.valueOf(...))
#   - AchievementId u DataStore-u (AchievementId.valueOf(...))
#   - SettingsManager.AppTheme u DataStore-u
# Ako ih R8 preimenuje, sačuvani podaci korisnika postaju nečitljivi.
-keepclassmembers enum com.program.braintrainer.** { *; }

# --- Google Play Billing --------------------------------------------
-keep class com.android.billingclient.api.** { *; }
