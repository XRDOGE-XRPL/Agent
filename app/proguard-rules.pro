# Autonomer Entwicklungsagent – Freigaberegeln
-keep class de.xrdoge.agent.bruecke.NativeBruecke { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
