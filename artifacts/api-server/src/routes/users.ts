import { Router, type IRouter } from "express";
import bcrypt from "bcryptjs";
import { eq, ne } from "drizzle-orm";
import { db, usersTable } from "@workspace/db";

const router: IRouter = Router();

router.get("/users", async (req, res) => {
  try {
    const users = await db
      .select({ id: usersTable.id, username: usersTable.username, role: usersTable.role, createdAt: usersTable.createdAt })
      .from(usersTable)
      .orderBy(usersTable.createdAt);
    res.json(users);
  } catch (err) {
    req.log.error({ err }, "List users error");
    res.status(500).json({ error: "Error interno" });
  }
});

router.post("/users", async (req, res) => {
  try {
    const { username, password, role } = req.body as { username: string; password: string; role?: string };
    if (!username || !password) {
      res.status(400).json({ error: "Usuario y contraseña requeridos" });
      return;
    }
    const passwordHash = await bcrypt.hash(password, 10);
    const [user] = await db
      .insert(usersTable)
      .values({ username: username.toLowerCase().trim(), passwordHash, role: role === "admin" ? "admin" : "user" })
      .returning({ id: usersTable.id, username: usersTable.username, role: usersTable.role });
    res.status(201).json(user);
  } catch (err: unknown) {
    if (err instanceof Error && err.message.includes("unique")) {
      res.status(409).json({ error: "Ese nombre de usuario ya existe" });
      return;
    }
    req.log.error({ err }, "Create user error");
    res.status(500).json({ error: "Error interno" });
  }
});

router.delete("/users/:id", async (req, res) => {
  try {
    const id = Number(req.params.id);
    const reqUser = (req as unknown as { user: { id: number } }).user;
    if (id === reqUser?.id) {
      res.status(400).json({ error: "No puedes eliminarte a ti mismo" });
      return;
    }
    const [deleted] = await db.delete(usersTable).where(eq(usersTable.id, id)).returning({ id: usersTable.id });
    if (!deleted) {
      res.status(404).json({ error: "Usuario no encontrado" });
      return;
    }
    res.json({ ok: true });
  } catch (err) {
    req.log.error({ err }, "Delete user error");
    res.status(500).json({ error: "Error interno" });
  }
});

router.patch("/users/:id/password", async (req, res) => {
  try {
    const id = Number(req.params.id);
    const { password } = req.body as { password: string };
    if (!password || password.length < 4) {
      res.status(400).json({ error: "Contraseña debe tener al menos 4 caracteres" });
      return;
    }
    const passwordHash = await bcrypt.hash(password, 10);
    const [updated] = await db
      .update(usersTable)
      .set({ passwordHash })
      .where(eq(usersTable.id, id))
      .returning({ id: usersTable.id });
    if (!updated) {
      res.status(404).json({ error: "Usuario no encontrado" });
      return;
    }
    res.json({ ok: true });
  } catch (err) {
    req.log.error({ err }, "Change password error");
    res.status(500).json({ error: "Error interno" });
  }
});

export default router;
