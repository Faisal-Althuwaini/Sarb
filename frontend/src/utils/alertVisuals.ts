import type { AlertSeverity } from "../types/alert";

export function alertSeverityBadgeVariant(severity: AlertSeverity): "destructive" | "caution" {
  return severity === "HIGH" ? "destructive" : "caution";
}
