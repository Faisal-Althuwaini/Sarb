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
    <div className="flex h-svh w-full items-center justify-center bg-background text-foreground">
      <Card className="w-80">
        <CardHeader className="text-center">
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
              className="h-9 rounded-lg border border-border bg-background px-3 text-sm"
            />
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={t("auth.password")}
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              className="h-9 rounded-lg border border-border bg-background px-3 text-sm"
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
