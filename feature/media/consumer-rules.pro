# Project-specific consumer rules for the media feature module
# These rules will be applied to projects that consume this library

# Keep serialization annotations
-keepattributes *Annotation*

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep Kotlin metadata
-keepattributes Metadata
