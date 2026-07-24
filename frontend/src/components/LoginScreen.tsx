import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface LoginScreenProps {
  onLogin: (username: string, password: string) => void;
  onRegister: (username: string, password: string) => void;
  error: string | null;
  loading: boolean;
}

export function LoginScreen({ onLogin, onRegister, error, loading }: LoginScreenProps) {
  const { t } = useTranslation();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (username.trim() === "" || password.trim() === "") return;
    if (mode === "login") onLogin(username, password);
    else onRegister(username, password);
  };

  return (
    <div className="relative flex h-svh w-full items-center justify-center overflow-hidden bg-background text-foreground">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_center,oklch(1_0_0/6%),transparent_60%)]" />
      <Card className="glow-primary relative w-80">
        <CardHeader className="text-center">
          <p className="text-xs text-muted-foreground">{t("auth.consoleLabel")}</p>
          <CardTitle className="font-heading text-2xl">{t("auth.title")}</CardTitle>
          <p className="text-sm text-muted-foreground">{t("auth.subtitle")}</p>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-3" onSubmit={submit}>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder={t("auth.username")}
              autoComplete="username"
              className="h-9 rounded-sm border border-border bg-background px-3 font-mono text-sm outline-none focus:border-primary/60 focus:ring-3 focus:ring-primary/20"
            />
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={t("auth.password")}
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              className="h-9 rounded-sm border border-border bg-background px-3 font-mono text-sm outline-none focus:border-primary/60 focus:ring-3 focus:ring-primary/20"
            />
            {error && (
              <p className="text-sm text-destructive">
                {t(`auth.errors.${error}`, { defaultValue: t("auth.errors.unknown") })}
              </p>
            )}
            <Button type="submit" disabled={loading}>
              {loading ? t("auth.submitting") : mode === "login" ? t("auth.login") : t("auth.register")}
            </Button>
            <button
              type="button"
              onClick={() => setMode((m) => (m === "login" ? "register" : "login"))}
              className="text-sm text-muted-foreground underline-offset-4 hover:underline"
            >
              {mode === "login" ? t("auth.toggleToRegister") : t("auth.toggleToLogin")}
            </button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
