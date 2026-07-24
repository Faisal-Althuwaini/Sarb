import { useState } from "react";
import type { AssistantAnswer, ChatTurn } from "../types/assistant";

// Phase 5: rag-service is REST-only (no WebSocket) - a question/answer round
// trip through Claude + pgvector retrieval takes a few seconds, so each turn
// starts "pending" and flips to "done"/"error" once the response lands.
// Phase 6: the request now goes through the gateway, which enforces the JWT.
const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL ?? "http://localhost:8080";

export function useAssistant(token: string | null) {
  const [turns, setTurns] = useState<ChatTurn[]>([]);

  const ask = async (question: string) => {
    const id = crypto.randomUUID();
    setTurns((prev) => [...prev, { id, question, answer: null, citations: [], status: "pending" }]);

    try {
      const res = await fetch(`${GATEWAY_URL}/api/assistant/ask`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({ question }),
      });
      if (!res.ok) throw new Error(`rag-service responded ${res.status}`);
      const data: AssistantAnswer = await res.json();
      setTurns((prev) =>
        prev.map((t) => (t.id === id ? { ...t, answer: data.answer, citations: data.citations, status: "done" } : t)),
      );
    } catch (err) {
      console.error("Failed to get assistant answer", err);
      setTurns((prev) => (prev.map((t) => (t.id === id ? { ...t, status: "error" } : t))));
    }
  };

  return { turns, ask };
}
