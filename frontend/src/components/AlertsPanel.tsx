import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { AlertRecord } from "../types/alert";
import { alertSeverityBadgeVariant } from "../utils/alertVisuals";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";

interface AlertsPanelProps {
  alerts: Map<number, AlertRecord>;
}

export function AlertsPanel({ alerts }: AlertsPanelProps) {
  const { t, i18n } = useTranslation();

  const activeAlerts = useMemo(
    () =>
      Array.from(alerts.values())
        .filter((a) => a.resolvedAt === null)
        .sort((a, b) => b.triggeredAt.localeCompare(a.triggeredAt)),
    [alerts],
  );

  const timeFormatter = useMemo(
    () => new Intl.DateTimeFormat(i18n.language, { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
    [i18n.language],
  );

  return (
    <Card className="flex h-full min-h-0 flex-col rounded-none border-0 border-s">
      <CardHeader className="border-b pb-3">
        <CardTitle className="flex items-center justify-between">
          <span>{t("alerts.title")}</span>
          <Badge variant="outline">{activeAlerts.length}</Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="min-h-0 flex-1 px-0">
        <ScrollArea className="h-full px-(--card-spacing)">
          {activeAlerts.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">{t("alerts.empty")}</p>
          ) : (
            <ul className="flex flex-col gap-2 py-2">
              {activeAlerts.map((alert) => (
                <li
                  key={alert.alertId}
                  className="rounded-lg border bg-card p-2.5 text-sm ring-1 ring-foreground/5"
                >
                  <div className="flex items-center justify-between gap-2">
                    <strong className="font-heading">{alert.droneId}</strong>
                    <Badge variant={alertSeverityBadgeVariant(alert.severity)}>
                      {t(`alerts.severity.${alert.severity}`)}
                    </Badge>
                  </div>
                  <p className="mt-1 text-foreground">{t(`alerts.type.${alert.type}`)}</p>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {timeFormatter.format(new Date(alert.triggeredAt))}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </ScrollArea>
      </CardContent>
    </Card>
  );
}
