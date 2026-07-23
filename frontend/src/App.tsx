import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { AlertsPanel } from "./components/AlertsPanel";
import { DroneMap } from "./components/DroneMap";
import { useAlerts } from "./hooks/useAlerts";
import { useFleetTelemetry } from "./hooks/useFleetTelemetry";
import { Badge } from "@/components/ui/badge";

function App() {
  const { t } = useTranslation();
  const { drones, status } = useFleetTelemetry();
  const { alerts } = useAlerts();
  const droneList = useMemo(() => Array.from(drones.values()), [drones]);

  const statusVariant =
    status === "connected" ? "default" : status === "connecting" ? "secondary" : "destructive";

  return (
    <div className="flex h-svh w-full flex-col bg-background text-foreground">
      <header className="flex items-center gap-4 border-b bg-card px-6 py-3">
        <div className="flex flex-col">
          <h1 className="font-heading text-xl font-bold text-foreground">{t("appName")}</h1>
          <p className="text-sm text-muted-foreground">{t("appTagline")}</p>
        </div>
        <Badge variant={statusVariant} className="ms-auto">
          {status === "connecting" && t("map.connecting")}
          {status === "connected" && t("map.connected")}
          {status === "disconnected" && t("map.disconnected")}
        </Badge>
        <Badge variant="outline">{t("map.droneCount", { count: droneList.length })}</Badge>
      </header>
      <main className="flex min-h-0 flex-1">
        <div className="min-w-0 flex-1">
          <DroneMap drones={droneList} />
        </div>
        <aside className="w-80 shrink-0">
          <AlertsPanel alerts={alerts} />
        </aside>
      </main>
    </div>
  );
}

export default App;
