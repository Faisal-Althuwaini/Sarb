import { useCallback, useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { Mission, MissionEvent, Waypoint } from "../types/mission";

// Phase 4: telemetry-service relays mission.events over the same WebSocket as
// telemetry/alerts; mission-service (which owns the missions table) serves
// the REST API for the initial list and for create/cancel actions.
const WS_URL = import.meta.env.VITE_WS_URL ?? "http://localhost:8083/ws";
const MISSIONS_API_URL = import.meta.env.VITE_MISSIONS_API_URL ?? "http://localhost:8085";

function applyEvent(missions: Map<number, Mission>, event: MissionEvent): Map<number, Mission> {
  const next = new Map(missions);
  const existing = next.get(event.missionId);

  if (event.type === "ASSIGNED") {
    next.set(event.missionId, {
      missionId: event.missionId,
      droneId: event.droneId,
      status: "ASSIGNED",
      waypoints: event.waypoints,
      createdAt: event.timestamp,
      startedAt: null,
      completedAt: null,
    });
    return next;
  }

  const base: Mission = existing ?? {
    missionId: event.missionId,
    droneId: event.droneId,
    status: "ASSIGNED",
    waypoints: [],
    createdAt: event.timestamp,
    startedAt: null,
    completedAt: null,
  };

  if (event.type === "STARTED") {
    next.set(event.missionId, { ...base, status: "IN_PROGRESS", startedAt: event.timestamp });
  } else if (event.type === "COMPLETED") {
    next.set(event.missionId, { ...base, status: "COMPLETED", completedAt: event.timestamp });
  } else if (event.type === "CANCELLED") {
    next.set(event.missionId, { ...base, status: "CANCELLED", completedAt: event.timestamp });
  }
  return next;
}

export function useMissions() {
  const [missions, setMissions] = useState<Map<number, Mission>>(new Map());
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    fetch(`${MISSIONS_API_URL}/api/missions`)
      .then((res) => res.json())
      .then((initial: Mission[]) => {
        setMissions((prev) => {
          const next = new Map(prev);
          for (const mission of initial) {
            next.set(mission.missionId, mission);
          }
          return next;
        });
      })
      .catch((err) => console.error("Failed to load missions", err));

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
      reconnectDelay: 2000,
      onConnect: () => {
        client.subscribe("/topic/missions", (message: IMessage) => {
          try {
            const event: MissionEvent = JSON.parse(message.body);
            setMissions((prev) => applyEvent(prev, event));
          } catch (err) {
            console.error("Failed to parse mission event", err);
          }
        });
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
    };
  }, []);

  const createMission = useCallback(async (droneId: string, waypoints: Waypoint[]) => {
    const res = await fetch(`${MISSIONS_API_URL}/api/missions`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ droneId, waypoints }),
    });
    if (!res.ok) {
      throw new Error(`Failed to create mission: ${res.status}`);
    }
    const mission: Mission = await res.json();
    setMissions((prev) => new Map(prev).set(mission.missionId, mission));
    return mission;
  }, []);

  const cancelMission = useCallback(async (missionId: number) => {
    const res = await fetch(`${MISSIONS_API_URL}/api/missions/${missionId}/cancel`, { method: "POST" });
    if (!res.ok) {
      throw new Error(`Failed to cancel mission: ${res.status}`);
    }
    const mission: Mission = await res.json();
    setMissions((prev) => new Map(prev).set(mission.missionId, mission));
    return mission;
  }, []);

  return { missions, createMission, cancelMission };
}
