import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { DroneTelemetry } from "../types/drone";
import type { Mission, MissionStatus } from "../types/mission";
import { missionStatusBadgeVariant } from "../utils/missionVisuals";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";

const ACTIVE_STATUSES: MissionStatus[] = ["ASSIGNED", "IN_PROGRESS"];

interface MissionsPanelProps {
  missions: Map<number, Mission>;
  drones: DroneTelemetry[];
  pickingDroneId: string | null;
  draftWaypointCount: number;
  onStartPicking: (droneId: string) => void;
  onSubmitDraft: () => void;
  onCancelDraft: () => void;
  onCancelMission: (missionId: number) => void;
}

export function MissionsPanel({
  missions,
  drones,
  pickingDroneId,
  draftWaypointCount,
  onStartPicking,
  onSubmitDraft,
  onCancelDraft,
  onCancelMission,
}: MissionsPanelProps) {
  const { t, i18n } = useTranslation();
  const [selectedDroneId, setSelectedDroneId] = useState<string>("");

  const sortedMissions = useMemo(
    () => Array.from(missions.values()).sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    [missions],
  );

  const timeFormatter = useMemo(
    () => new Intl.DateTimeFormat(i18n.language, { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
    [i18n.language],
  );

  const assignableDrones = useMemo(() => {
    const busyDroneIds = new Set(
      Array.from(missions.values())
        .filter((m) => ACTIVE_STATUSES.includes(m.status))
        .map((m) => m.droneId),
    );
    return drones.filter((d) => !busyDroneIds.has(d.droneId));
  }, [drones, missions]);

  return (
    <Card className="flex h-full min-h-0 flex-col rounded-none border-0 border-e">
      <CardHeader className="border-b pb-3">
        <CardTitle className="flex items-center justify-between">
          <span>{t("mission.title")}</span>
          <Badge variant="outline">{sortedMissions.length}</Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="flex min-h-0 flex-1 flex-col gap-2 px-(--card-spacing) pt-3">
        {pickingDroneId === null ? (
          <div className="flex items-center gap-2">
            <select
              value={selectedDroneId}
              onChange={(e) => setSelectedDroneId(e.target.value)}
              className="h-8 flex-1 rounded-sm border border-border bg-background px-2 font-mono text-sm"
            >
              <option value="" disabled>
                {t("mission.selectDrone")}
              </option>
              {assignableDrones.map((d) => (
                <option key={d.droneId} value={d.droneId}>
                  {d.droneId}
                </option>
              ))}
            </select>
            <Button
              size="sm"
              disabled={selectedDroneId === ""}
              onClick={() => onStartPicking(selectedDroneId)}
            >
              {t("mission.assign")}
            </Button>
          </div>
        ) : (
          <div className="flex items-center justify-between gap-2 rounded-sm border border-primary/40 bg-card p-2 text-sm glow-primary">
            <div>
              <p className="font-mono text-foreground">{pickingDroneId}</p>
              <p className="text-xs text-muted-foreground">
                {t("mission.pickWaypoints")} — {t("mission.waypointCount", { count: draftWaypointCount })}
              </p>
            </div>
            <div className="flex gap-1.5">
              <Button size="sm" variant="outline" onClick={onCancelDraft}>
                {t("mission.cancelDraft")}
              </Button>
              <Button size="sm" disabled={draftWaypointCount === 0} onClick={onSubmitDraft}>
                {t("mission.submit")}
              </Button>
            </div>
          </div>
        )}

        <ScrollArea className="min-h-0 flex-1">
          {sortedMissions.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">{t("mission.empty")}</p>
          ) : (
            <ul className="flex flex-col gap-2 py-2">
              {sortedMissions.map((mission) => (
                <li
                  key={mission.missionId}
                  className="rounded-sm border bg-card p-2.5 text-sm"
                >
                  <div className="flex items-center justify-between gap-2">
                    <strong className="font-mono text-xs tracking-wide text-foreground">{mission.droneId}</strong>
                    <Badge variant={missionStatusBadgeVariant(mission.status)}>
                      {t(`mission.status.${mission.status}`)}
                    </Badge>
                  </div>
                  <p className="mt-1 text-foreground">
                    {t("mission.waypointCount", { count: mission.waypoints.length })}
                  </p>
                  <div className="mt-0.5 flex items-center justify-between">
                    <p className="font-mono text-[0.7rem] text-muted-foreground">{timeFormatter.format(new Date(mission.createdAt))}</p>
                    {ACTIVE_STATUSES.includes(mission.status) && (
                      <Button size="xs" variant="destructive" onClick={() => onCancelMission(mission.missionId)}>
                        {t("mission.cancelMission")}
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </ScrollArea>
      </CardContent>
    </Card>
  );
}
