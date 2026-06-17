import jwt from "jsonwebtoken";
import type { Request, Response, NextFunction } from "express";

export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  if (!header?.startsWith("Bearer ")) {
    res.status(401).json({ error: "Unauthorized" });
    return;
  }
  const token = header.slice(7);
  try {
    const secret = process.env.SESSION_SECRET;
    if (!secret) throw new Error("SESSION_SECRET not set");
    const payload = jwt.verify(token, secret) as { id: number; username: string; role: string };
    (req as unknown as { user: typeof payload }).user = payload;
    next();
  } catch {
    res.status(401).json({ error: "Token inválido o expirado" });
  }
}

export function requireAdmin(req: Request, res: Response, next: NextFunction) {
  const user = (req as unknown as { user?: { role: string } }).user;
  if (user?.role !== "admin") {
    res.status(403).json({ error: "Solo el administrador puede hacer esto" });
    return;
  }
  next();
}
