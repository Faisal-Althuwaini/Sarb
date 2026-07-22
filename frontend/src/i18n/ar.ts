// Arabic (ar) resource bundle — the ONLY source of user-facing strings.
// Enum/status codes stay English on the wire; this file maps them to Arabic labels.
export const ar = {
  translation: {
    appName: "سرب",
    appTagline: "منصة عمليات أسطول الطائرات المسيّرة",
    map: {
      title: "الخريطة الحية",
      connecting: "جارٍ الاتصال بخدمة تتبّع الأسطول...",
      connected: "متصل — البث الحي نشط",
      disconnected: "انقطع الاتصال، جارٍ إعادة المحاولة...",
      droneCount: "عدد الطائرات: {{count}}",
    },
    drone: {
      id: "معرّف الطائرة",
      battery: "البطارية",
      altitude: "الارتفاع",
      speed: "السرعة",
      heading: "الاتجاه",
      status: "الحالة",
      mission: "المهمة",
      noMission: "بلا مهمة",
      lastUpdate: "آخر تحديث",
      units: {
        meters: "م",
        metersPerSecond: "م/ث",
        degrees: "°",
        percent: "%",
      },
    },
    status: {
      IN_FLIGHT: "في الجو",
      IDLE: "خامل",
      LOW_BATTERY: "بطارية منخفضة",
      LOST_SIGNAL: "انقطاع الإشارة",
      LANDED: "هابطة",
    },
  },
};

export type ArTranslation = typeof ar.translation;
