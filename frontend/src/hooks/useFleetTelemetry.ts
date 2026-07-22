import { useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { DroneTelemetry, FleetTelemetryMessage } from "../types/drone";

const WS_URL = import.meta.env.VITE_WS_URL ?? "http://localhost:8082/ws";

export type ConnectionStatus = "connecting" | "connected" | "disconnected";

export function useFleetTelemetry() {
  const [drones, setDrones] = useState<Map<string, DroneTelemetry>>(new Map());
  const [status, setStatus] = useState<ConnectionStatus>("connecting");
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
      reconnectDelay: 2000,
      onConnect: () => {
        setStatus("connected");
        client.subscribe("/topic/telemetry", (message: IMessage) => {
          try {
            const payload: FleetTelemetryMessage | DroneTelemetry[] = JSON.parse(message.body);
            const list: DroneTelemetry[] = Array.isArray(payload) ? payload : payload.drones;
            setDrones((prev) => {
              const next = new Map(prev);
              for (const d of list) {
                next.set(d.droneId, d);
              }
              return next;
            });
          } catch (err) {
            console.error("Failed to parse telemetry frame", err);
          }
        });
      },
      onDisconnect: () => setStatus("disconnected"),
      onWebSocketClose: () => setStatus("disconnected"),
    });

    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
    };
  }, []);

  return { drones, status };
}
