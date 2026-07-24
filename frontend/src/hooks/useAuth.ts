import { useCallback, useState } from "react";
import { clearAuth, loadAuth, saveAuth } from "../lib/authStorage";
import type { AuthResponse } from "../types/auth";

const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL ?? "http://localhost:8080";

export function useAuth() {
  const [auth, setAuth] = useState<AuthResponse | null>(() => loadAuth());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = useCallback(async (path: "login" | "register", username: string, password: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${GATEWAY_URL}/api/auth/${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      if (!res.ok) {
        throw new Error(res.status === 401 ? "invalid-credentials" : res.status === 409 ? "username-taken" : "unknown");
      }
      const data: AuthResponse = await res.json();
      saveAuth(data);
      setAuth(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "unknown");
    } finally {
      setLoading(false);
    }
  }, []);

  const login = useCallback((username: string, password: string) => submit("login", username, password), [submit]);
  const register = useCallback((username: string, password: string) => submit("register", username, password), [submit]);

  const logout = useCallback(() => {
    clearAuth();
    setAuth(null);
  }, []);

  return { auth, token: auth?.token ?? null, username: auth?.username ?? null, login, register, logout, error, loading };
}
