export interface Citation {
  source: string | null;
  sourceType: "regulation" | "sop" | null;
  page: number | null;
  score: number | null;
  excerpt: string | null;
}

export interface AssistantAnswer {
  answer: string;
  citations: Citation[];
}

export interface ChatTurn {
  id: string;
  question: string;
  answer: string | null;
  citations: Citation[];
  status: "pending" | "done" | "error";
}
