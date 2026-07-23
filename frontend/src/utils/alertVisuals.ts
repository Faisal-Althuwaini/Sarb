import type { AlertSeverity } from "../types/alert";

export function alertSeverityBadgeVariant(severity: AlertSeverity): "destructive" | "secondary" {
  return severity === "HIGH" ? "destructive" : "secondary";
}
