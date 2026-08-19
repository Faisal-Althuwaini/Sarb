import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { ChatTurn } from "../types/assistant";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";

interface AssistantPanelProps {
  turns: ChatTurn[];
  onAsk: (question: string) => void;
  onClose: () => void;
}

export function AssistantPanel({ turns, onAsk, onClose }: AssistantPanelProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState("");

  const submit = () => {
    const question = draft.trim();
    if (question === "") return;
    onAsk(question);
    setDraft("");
  };

  return (
    <div className="absolute inset-y-0 inset-e-0 z-1100 w-96 max-w-full p-3">
      <Card className="glow-primary flex h-full min-h-0 flex-col shadow-xl">
        <CardHeader className="border-b pb-3">
          <CardTitle className="flex items-center justify-between">
            <span>{t("assistant.title")}</span>
            <Button size="icon-sm" variant="ghost" onClick={onClose} aria-label={t("assistant.close")}>
              ✕
            </Button>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex min-h-0 flex-1 flex-col gap-2 px-(--card-spacing) pt-3">
          <ScrollArea className="min-h-0 flex-1">
            {turns.length === 0 ? (
              <p className="py-6 text-center text-sm text-muted-foreground">{t("assistant.empty")}</p>
            ) : (
              <ul className="flex flex-col gap-3 py-2">
                {turns.map((turn) => (
                  <li key={turn.id} className="flex flex-col gap-1.5">
                    <div className="self-end rounded-sm bg-primary/10 px-3 py-2 text-sm text-foreground">
                      {turn.question}
                    </div>
                    {turn.status === "pending" && (
                      <div className="self-start rounded-sm border bg-card px-3 py-2 text-sm text-muted-foreground">
                        {t("assistant.thinking")}
                      </div>
                    )}
                    {turn.status === "error" && (
                      <div className="self-start rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                        {t("assistant.error")}
                      </div>
                    )}
                    {turn.status === "done" && (
                      <div className="self-start rounded-sm border bg-card px-3 py-2 text-sm text-foreground">
                        <p className="whitespace-pre-wrap">{turn.answer}</p>
                        {turn.citations.length > 0 && (
                          <div className="mt-2 flex flex-col gap-1 border-t pt-2">
                            <p className="text-xs font-medium text-muted-foreground">{t("assistant.citations")}</p>
                            {turn.citations.map((c, i) => (
                              <div key={i} className="flex items-center gap-1.5 text-xs text-muted-foreground">
                                <Badge variant="outline" className="shrink-0">
                                  {c.sourceType ? t(`assistant.sourceType.${c.sourceType}`) : "?"}
                                </Badge>
                                <span className="truncate">
                                  {c.source}
                                  {c.page != null ? ` — ص${c.page}` : ""}
                                </span>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </ScrollArea>
          <div className="flex items-end gap-2 border-t pt-2">
            <textarea
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  submit();
                }
              }}
              placeholder={t("assistant.placeholder")}
              rows={2}
              className="h-16 flex-1 resize-none rounded-sm border border-border bg-background px-2 py-1.5 text-sm"
            />
            <Button size="sm" disabled={draft.trim() === ""} onClick={submit}>
              {t("assistant.send")}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
