import { Fragment } from "react";
import { MapContainer, TileLayer, CircleMarker, Polyline, Popup, useMapEvents } from "react-leaflet";
import { useTranslation } from "react-i18next";
import type { DroneTelemetry } from "../types/drone";
import type { Mission, Waypoint } from "../types/mission";
import { droneColor, droneStatusBadgeVariant } from "../utils/droneVisuals";
import { Badge } from "@/components/ui/badge";
import "leaflet/dist/leaflet.css";

// Riyadh, per the brief's demo-area decision.
const RIYADH_CENTER: [number, number] = [24.71, 46.68];
const DEFAULT_ZOOM = 12;

interface DroneMapProps {
  drones: DroneTelemetry[];
  missions: Mission[];
  pickingDroneId: string | null;
  draftWaypoints: Waypoint[];
  onMapClick: (lat: number, lon: number) => void;
}

function MapClickHandler({ enabled, onClick }: { enabled: boolean; onClick: (lat: number, lon: number) => void }) {
  useMapEvents({
    click: (e) => {
      if (enabled) {
        onClick(e.latlng.lat, e.latlng.lng);
      }
    },
  });
  return null;
}

export function DroneMap({ drones, missions, pickingDroneId, draftWaypoints, onMapClick }: DroneMapProps) {
  const { t } = useTranslation();
  const activeMissions = missions.filter(
    (m) => (m.status === "ASSIGNED" || m.status === "IN_PROGRESS") && m.waypoints.length > 0,
  );

  return (
    <MapContainer
      center={RIYADH_CENTER}
      zoom={DEFAULT_ZOOM}
      style={{ height: "100%", width: "100%", cursor: pickingDroneId ? "crosshair" : undefined }}
      // Leaflet's zoom/attribution controls are positioned by CSS and are LTR-agnostic;
      // no RTL-specific map handling is needed here.
    >
      <MapClickHandler enabled={pickingDroneId !== null} onClick={onMapClick} />
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {activeMissions.map((mission) => {
        const drone = drones.find((d) => d.droneId === mission.droneId);
        const positions: [number, number][] = [
          ...(drone ? ([[drone.position.lat, drone.position.lon]] as [number, number][]) : []),
          ...mission.waypoints.map((w): [number, number] => [w.lat, w.lon]),
        ];
        return (
          <Fragment key={mission.missionId}>
            <Polyline positions={positions} pathOptions={{ color: "#2563eb", weight: 2, dashArray: "6 6" }} />
            {mission.waypoints.map((wp, i) => (
              <CircleMarker
                key={i}
                center={[wp.lat, wp.lon]}
                radius={4}
                pathOptions={{ color: "#1d4ed8", weight: 1, fillColor: "#3b82f6", fillOpacity: 1 }}
              />
            ))}
          </Fragment>
        );
      })}
      {draftWaypoints.length > 0 && (
        <>
          <Polyline
            positions={draftWaypoints.map((w): [number, number] => [w.lat, w.lon])}
            pathOptions={{ color: "#16a34a", weight: 2, dashArray: "4 6" }}
          />
          {draftWaypoints.map((wp, i) => (
            <CircleMarker
              key={i}
              center={[wp.lat, wp.lon]}
              radius={5}
              pathOptions={{ color: "#15803d", weight: 1, fillColor: "#22c55e", fillOpacity: 1 }}
            />
          ))}
        </>
      )}
      {drones.map((drone) => (
        <CircleMarker
          key={drone.droneId}
          center={[drone.position.lat, drone.position.lon]}
          radius={9}
          pathOptions={{
            color: "#1f2937",
            weight: 1,
            fillColor: droneColor(drone),
            fillOpacity: 0.9,
          }}
        >
          <Popup>
            <div dir="rtl" className="min-w-44 font-sans text-sm text-popover-foreground">
              <div className="flex items-center justify-between gap-2 border-b border-border pb-1.5">
                <strong className="font-heading text-sm">{drone.droneId}</strong>
                <Badge variant={droneStatusBadgeVariant(drone)}>{t(`status.${drone.status}`)}</Badge>
              </div>
              <dl className="mt-1.5 grid grid-cols-2 gap-x-2 gap-y-1">
                <dt className="text-muted-foreground">{t("drone.battery")}</dt>
                <dd className="text-end">
                  {drone.batteryPct.toFixed(0)} {t("drone.units.percent")}
                </dd>
                <dt className="text-muted-foreground">{t("drone.altitude")}</dt>
                <dd className="text-end">
                  {drone.altitudeM.toFixed(0)} {t("drone.units.meters")}
                </dd>
                <dt className="text-muted-foreground">{t("drone.speed")}</dt>
                <dd className="text-end">
                  {drone.speedMps.toFixed(1)} {t("drone.units.metersPerSecond")}
                </dd>
                <dt className="text-muted-foreground">{t("drone.heading")}</dt>
                <dd className="text-end">
                  {drone.headingDeg.toFixed(0)} {t("drone.units.degrees")}
                </dd>
                <dt className="text-muted-foreground">{t("drone.mission")}</dt>
                <dd className="text-end">{drone.missionId ?? t("drone.noMission")}</dd>
              </dl>
            </div>
          </Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
}
