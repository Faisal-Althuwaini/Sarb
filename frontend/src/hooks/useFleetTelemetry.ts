import { useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { DroneTelemetry } from "../types/drone";

// Phase 3: telemetry-service (not simulator-service) hosts the WebSocket -
// it's the Kafka consumer that relays drone.telemetry to the frontend.
// Phase 6: this connection bypasses the gateway (WebSocket proxying isn't
// supported by Spring Cloud Gateway's MVC variant) - the JWT is sent as a
// STOMP CONNECT header instead and checked directly by telemetry-service.
const WS_URL = import.meta.env.VITE_WS_URL ?? "http://localhost:8083/ws";

export type ConnectionStatus = "connecting" | "connected" | "disconnected";

export function useFleetTelemetry(token: string | null) {
  const [drones, setDrones] = useState<Map<string, DroneTelemetry>>(new Map());
  const [status, setStatus] = useState<ConnectionStatus>("connecting");
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      onConnect: () => {
        setStatus("connected");
        client.subscribe("/topic/telemetry", (message: IMessage) => {
          try {
            // Phase 3: one drone.telemetry Kafka message per drone per tick,
            // relayed as a single frame rather than a whole-fleet batch.
            const frame: DroneTelemetry = JSON.parse(message.body);
            setDrones((prev) => {
              const next = new Map(prev);
              next.set(frame.droneId, frame);
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
  }, [token]);

  return { drones, status };
}
