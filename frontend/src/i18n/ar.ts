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
    alerts: {
      title: "التنبيهات",
      empty: "لا توجد تنبيهات نشطة",
      active: "نشط",
      resolved: "تمت المعالجة",
      type: {
        GEOFENCE_BREACH: "تجاوز المنطقة المسموحة",
        ALTITUDE_VIOLATION: "تجاوز الحد الأقصى للارتفاع",
        LOW_BATTERY: "بطارية منخفضة",
        CRITICAL_BATTERY: "بطارية حرجة",
      },
      severity: {
        MEDIUM: "متوسط",
        HIGH: "عالٍ",
      },
    },
    assistant: {
      openButton: "المساعد الذكي",
      title: "المساعد الذكي",
      close: "إغلاق",
      empty: "اسأل عن لائحة الطيران المسيّر أو إجراءات التشغيل الداخلية",
      placeholder: "اكتب سؤالك هنا...",
      send: "إرسال",
      thinking: "جارٍ البحث والإجابة...",
      error: "تعذّر الحصول على إجابة، حاول مرة أخرى",
      citations: "المصادر",
      sourceType: {
        regulation: "لائحة GACAR",
        sop: "إجراء داخلي",
      },
    },
    mission: {
      title: "المهام",
      empty: "لا توجد مهام",
      assign: "تعيين",
      selectDrone: "اختر طائرة",
      pickWaypoints: "انقر على الخريطة لإضافة نقاط المسار",
      waypointCount: "نقاط المسار: {{count}}",
      submit: "إرسال",
      cancelDraft: "إلغاء",
      cancelMission: "إلغاء المهمة",
      status: {
        ASSIGNED: "معيّنة",
        IN_PROGRESS: "قيد التنفيذ",
        COMPLETED: "مكتملة",
        CANCELLED: "ملغاة",
      },
    },
  },
};

export type ArTranslation = typeof ar.translation;
