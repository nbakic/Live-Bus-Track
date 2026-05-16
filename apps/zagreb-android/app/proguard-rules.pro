# ProGuard pravila za :app release build.
# Ktor i kotlinx.serialization trebaju zadržati svoje generirane serializere.
-keep class hr.zet.transit.data.remote.dto.** { *; }
-keepclassmembers class hr.zet.transit.data.remote.dto.** { *; }
