// Mirrors the alert JSON contract published by simulator-service on /topic/alerts
// and returned by GET /api/alerts/active. Field names stay English (wire format);
// Arabic labels live in i18n. resolvedAt === null means the alert is still active.

export type AlertType = "GEOFENCE_BREACH" | "ALTITUDE_VIOLATION" | "LOW_BATTERY" | "CRITICAL_BATTERY";

export type AlertSeverity = "MEDIUM" | "HIGH";

export interface AlertRecord {
  alertId: number;
  droneId: string;
  type: AlertType;
  severity: AlertSeverity;
  message: string;
  triggeredAt: string; // ISO-8601
  resolvedAt: string | null; // ISO-8601
}
