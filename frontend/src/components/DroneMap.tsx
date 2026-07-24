import { Fragment } from "react";
import L from "leaflet";
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Marker,
  Polyline,
  Popup,
  useMapEvents,
} from "react-leaflet";
import { useTranslation } from "react-i18next";
import type { DroneTelemetry } from "../types/drone";
import type { Mission, Waypoint } from "../types/mission";
import {
  droneColor,
  droneIsCritical,
  droneStatusBadgeVariant,
} from "../utils/droneVisuals";
import { Badge } from "@/components/ui/badge";
import "leaflet/dist/leaflet.css";

// Riyadh, per the brief's demo-area decision.
const RIYADH_CENTER: [number, number] = [24.71, 46.68];
const DEFAULT_ZOOM = 12;

const DRONE_ICON_SIZE = 30;

// Quadcopter glyph (neutral) + a colored status dot pinned to the icon's
// top-right corner - color/state lives on the dot (and the critical pulse),
// not the whole glyph, so markers stay legible against busy satellite tiles.
function buildDroneIcon(color: string, critical: boolean): L.DivIcon {
  return L.divIcon({
    className: "drone-icon-wrapper",
    iconSize: [DRONE_ICON_SIZE, DRONE_ICON_SIZE],
    iconAnchor: [DRONE_ICON_SIZE / 2, DRONE_ICON_SIZE / 2],
    popupAnchor: [0, -DRONE_ICON_SIZE / 2],
    html: `
      <div class="drone-icon" style="--drone-color:${color}">
        ${critical ? '<div class="drone-icon-halo pulse-critical"></div>' : ""}
        <svg viewBox="0 0 24 24" width="26" height="26" xmlns="http://www.w3.org/2000/svg">
          <g fill="#eef1ee">
            <rect x="10.7" y="3.3" width="2.6" height="7.4" rx="1.3" transform="rotate(45 12 12)" />
            <rect x="10.7" y="3.3" width="2.6" height="7.4" rx="1.3" transform="rotate(135 12 12)" />
            <rect x="10.7" y="3.3" width="2.6" height="7.4" rx="1.3" transform="rotate(225 12 12)" />
            <rect x="10.7" y="3.3" width="2.6" height="7.4" rx="1.3" transform="rotate(315 12 12)" />
          </g>
          <g fill="none" stroke="#eef1ee" stroke-width="1.4">
            <circle cx="18" cy="6" r="2.6" />
            <circle cx="6" cy="6" r="2.6" />
            <circle cx="6" cy="18" r="2.6" />
            <circle cx="18" cy="18" r="2.6" />
          </g>
          <rect x="9" y="9" width="6" height="6" rx="1.8" fill="#0d0f0d" stroke="${color}" stroke-width="1.6" />
        </svg>
        <span class="drone-status-dot"></span>
      </div>
    `,
  });
}

interface DroneMapProps {
  drones: DroneTelemetry[];
  missions: Mission[];
  pickingDroneId: string | null;
  draftWaypoints: Waypoint[];
  onMapClick: (lat: number, lon: number) => void;
}

function MapClickHandler({
  enabled,
  onClick,
}: {
  enabled: boolean;
  onClick: (lat: number, lon: number) => void;
}) {
  useMapEvents({
    click: (e) => {
      if (enabled) {
        onClick(e.latlng.lat, e.latlng.lng);
      }
    },
  });
  return null;
}

export function DroneMap({
  drones,
  missions,
  pickingDroneId,
  draftWaypoints,
  onMapClick,
}: DroneMapProps) {
  const { t } = useTranslation();
  const activeMissions = missions.filter(
    (m) =>
      (m.status === "ASSIGNED" || m.status === "IN_PROGRESS") &&
      m.waypoints.length > 0,
  );

  return (
    <MapContainer
      center={RIYADH_CENTER}
      zoom={DEFAULT_ZOOM}
      style={{
        height: "100%",
        width: "100%",
        cursor: pickingDroneId ? "crosshair" : undefined,
      }}
      // Leaflet's zoom/attribution controls are positioned by CSS and are LTR-agnostic;
      // no RTL-specific map handling is needed here.
    >
      <MapClickHandler enabled={pickingDroneId !== null} onClick={onMapClick} />
      <TileLayer
        attribution="Tiles &copy; Esri &mdash; Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community"
        url="https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        maxZoom={19}
      />
      {activeMissions.map((mission) => {
        const drone = drones.find((d) => d.droneId === mission.droneId);
        const positions: [number, number][] = [
          ...(drone
            ? ([[drone.position.lat, drone.position.lon]] as [number, number][])
            : []),
          ...mission.waypoints.map((w): [number, number] => [w.lat, w.lon]),
        ];
        return (
          <Fragment key={mission.missionId}>
            <Polyline
              positions={positions}
              pathOptions={{
                color: "#22d3ee",
                weight: 4,
                opacity: 0.9,
                dashArray: "14 10",
                lineCap: "round",
              }}
            />
            {mission.waypoints.map((wp, i) => (
              <CircleMarker
                key={i}
                center={[wp.lat, wp.lon]}
                radius={4}
                pathOptions={{
                  color: "#0891b2",
                  weight: 1,
                  fillColor: "#22d3ee",
                  fillOpacity: 1,
                }}
              />
            ))}
          </Fragment>
        );
      })}
      {draftWaypoints.length > 0 && (
        <>
          <Polyline
            positions={draftWaypoints.map((w): [number, number] => [
              w.lat,
              w.lon,
            ])}
            pathOptions={{
              color: "#39ff88",
              weight: 4,
              opacity: 0.95,
              dashArray: "10 8",
              lineCap: "round",
            }}
          />
          {draftWaypoints.map((wp, i) => (
            <CircleMarker
              key={i}
              center={[wp.lat, wp.lon]}
              radius={5}
              pathOptions={{
                color: "#15803d",
                weight: 1,
                fillColor: "#39ff88",
                fillOpacity: 1,
              }}
            />
          ))}
        </>
      )}
      {drones.map((drone) => {
        const color = droneColor(drone);
        const critical = droneIsCritical(drone);
        return (
          <Fragment key={drone.droneId}>
            <Marker
              position={[drone.position.lat, drone.position.lon]}
              icon={buildDroneIcon(color, critical)}
            >
              <Popup>
                <div
                  dir="rtl"
                  className="min-w-48 font-sans text-sm text-popover-foreground"
                >
                  <div className="flex items-center justify-between gap-2 border-b border-border pb-1.5">
                    <strong className="font-mono text-xs tracking-wide">
                      {drone.droneId}
                    </strong>
                    <Badge variant={droneStatusBadgeVariant(drone)}>
                      {t(`status.${drone.status}`)}
                    </Badge>
                  </div>
                  <dl className="mt-1.5 grid grid-cols-2 gap-x-2 gap-y-1 font-mono text-xs">
                    <dt className="font-sans text-muted-foreground">
                      {t("drone.battery")}
                    </dt>
                    <dd className="text-end">
                      {drone.batteryPct.toFixed(0)} {t("drone.units.percent")}
                    </dd>
                    <dt className="font-sans text-muted-foreground">
                      {t("drone.altitude")}
                    </dt>
                    <dd className="text-end">
                      {drone.altitudeM.toFixed(0)} {t("drone.units.meters")}
                    </dd>
                    <dt className="font-sans text-muted-foreground">
                      {t("drone.speed")}
                    </dt>
                    <dd className="text-end">
                      {drone.speedMps.toFixed(1)}{" "}
                      {t("drone.units.metersPerSecond")}
                    </dd>
                    <dt className="font-sans text-muted-foreground">
                      {t("drone.heading")}
                    </dt>
                    <dd className="text-end">
                      {drone.headingDeg.toFixed(0)} {t("drone.units.degrees")}
                    </dd>
                    <dt className="font-sans text-muted-foreground">
                      {t("drone.mission")}
                    </dt>
                    <dd className="text-end">
                      {drone.missionId ?? t("drone.noMission")}
                    </dd>
                  </dl>
                </div>
              </Popup>
            </Marker>
          </Fragment>
        );
      })}
    </MapContainer>
  );
}
