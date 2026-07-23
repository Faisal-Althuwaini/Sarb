import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { AlertsPanel } from "./components/AlertsPanel";
import { AssistantPanel } from "./components/AssistantPanel";
import { DroneMap } from "./components/DroneMap";
import { MissionsPanel } from "./components/MissionsPanel";
import { useAlerts } from "./hooks/useAlerts";
import { useAssistant } from "./hooks/useAssistant";
import { useFleetTelemetry } from "./hooks/useFleetTelemetry";
import { useMissions } from "./hooks/useMissions";
import type { Waypoint } from "./types/mission";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

// Waypoint altitude isn't picked in the UI (clicks only give lat/lon) - every
// drone-assigned waypoint flies at this fixed altitude.
const DEFAULT_WAYPOINT_ALTITUDE_M = 80;

function App() {
  const { t } = useTranslation();
  const { drones, status } = useFleetTelemetry();
  const { alerts } = useAlerts();
  const { missions, createMission, cancelMission } = useMissions();
  const { turns, ask } = useAssistant();
  const droneList = useMemo(() => Array.from(drones.values()), [drones]);

  const [pickingDroneId, setPickingDroneId] = useState<string | null>(null);
  const [draftWaypoints, setDraftWaypoints] = useState<Waypoint[]>([]);
  const [assistantOpen, setAssistantOpen] = useState(false);

  const statusVariant =
    status === "connected" ? "default" : status === "connecting" ? "secondary" : "destructive";

  const handleMapClick = (lat: number, lon: number) => {
    if (pickingDroneId === null) return;
    setDraftWaypoints((prev) => [...prev, { lat, lon, altitudeM: DEFAULT_WAYPOINT_ALTITUDE_M }]);
  };

  const handleSubmitDraft = () => {
    if (pickingDroneId === null || draftWaypoints.length === 0) return;
    createMission(pickingDroneId, draftWaypoints).catch((err) => console.error("Failed to create mission", err));
    setPickingDroneId(null);
    setDraftWaypoints([]);
  };

  const handleCancelDraft = () => {
    setPickingDroneId(null);
    setDraftWaypoints([]);
  };

  const handleCancelMission = (missionId: number) => {
    cancelMission(missionId).catch((err) => console.error("Failed to cancel mission", err));
  };

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
        <Button size="sm" variant="secondary" onClick={() => setAssistantOpen((v) => !v)}>
          {t("assistant.openButton")}
        </Button>
      </header>
      <main className="relative flex min-h-0 flex-1">
        <div className="min-w-0 flex-1">
          <DroneMap
            drones={droneList}
            missions={Array.from(missions.values())}
            pickingDroneId={pickingDroneId}
            draftWaypoints={draftWaypoints}
            onMapClick={handleMapClick}
          />
        </div>
        {assistantOpen && (
          <AssistantPanel turns={turns} onAsk={ask} onClose={() => setAssistantOpen(false)} />
        )}
        <aside className="flex w-80 shrink-0 flex-col divide-y">
          <div className="min-h-0 flex-1">
            <AlertsPanel alerts={alerts} />
          </div>
          <div className="min-h-0 flex-1">
            <MissionsPanel
              missions={missions}
              drones={droneList}
              pickingDroneId={pickingDroneId}
              draftWaypointCount={draftWaypoints.length}
              onStartPicking={setPickingDroneId}
              onSubmitDraft={handleSubmitDraft}
              onCancelDraft={handleCancelDraft}
              onCancelMission={handleCancelMission}
            />
          </div>
        </aside>
      </main>
    </div>
  );
}

export default App;
