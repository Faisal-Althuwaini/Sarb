import { useState } from "react";
import type { AssistantAnswer, ChatTurn } from "../types/assistant";

// Phase 5: rag-service is REST-only (no WebSocket) - a question/answer round
// trip through Claude + pgvector retrieval takes a few seconds, so each turn
// starts "pending" and flips to "done"/"error" once the response lands.
const ASSISTANT_API_URL = import.meta.env.VITE_ASSISTANT_API_URL ?? "http://localhost:8086";

export function useAssistant() {
  const [turns, setTurns] = useState<ChatTurn[]>([]);

  const ask = async (question: string) => {
    const id = crypto.randomUUID();
    setTurns((prev) => [...prev, { id, question, answer: null, citations: [], status: "pending" }]);

    try {
      const res = await fetch(`${ASSISTANT_API_URL}/api/assistant/ask`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
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
