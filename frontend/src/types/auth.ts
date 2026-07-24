export type Role = "ADMIN" | "OPERATOR";

export interface AuthResponse {
  token: string;
  username: string;
  role: Role;
}
