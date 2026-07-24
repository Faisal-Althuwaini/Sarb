import { useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { AlertRecord } from "../types/alert";

// Phase 3: telemetry-service relays drone.alerts over the same WebSocket as
// telemetry; alert-service (which owns the alerts table) serves the REST seed.
// Phase 6: the REST call now goes through the gateway (which owns JWT
// enforcement for every REST route); the WebSocket still connects directly
// to telemetry-service (see useFleetTelemetry) with the JWT on the STOMP
// CONNECT frame instead, since gateway can't proxy WebSocket upgrades.
const WS_URL = import.meta.env.VITE_WS_URL ?? "http://localhost:8083/ws";
const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL ?? "http://localhost:8080";

export function useAlerts(token: string | null) {
  const [alerts, setAlerts] = useState<Map<number, AlertRecord>>(new Map());
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!token) return;

    fetch(`${GATEWAY_URL}/api/alerts/active`, { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => res.json())
      .then((initial: AlertRecord[]) => {
        setAlerts((prev) => {
          const next = new Map(prev);
          for (const alert of initial) {
            next.set(alert.alertId, alert);
          }
          return next;
        });
      })
      .catch((err) => console.error("Failed to load active alerts", err));

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      onConnect: () => {
        client.subscribe("/topic/alerts", (message: IMessage) => {
          try {
            const alert: AlertRecord = JSON.parse(message.body);
            setAlerts((prev) => {
              const next = new Map(prev);
              next.set(alert.alertId, alert);
              return next;
            });
          } catch (err) {
            console.error("Failed to parse alert event", err);
          }
        });
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
    };
  }, [token]);

  return { alerts };
}
