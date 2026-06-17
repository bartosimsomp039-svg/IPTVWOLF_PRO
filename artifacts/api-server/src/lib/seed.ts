import bcrypt from "bcryptjs";
import { eq } from "drizzle-orm";
import { db, usersTable } from "@workspace/db";
import { logger } from "./logger";

export async function seedAdminUser() {
  try {
    const [existing] = await db
      .select({ id: usersTable.id })
      .from(usersTable)
      .where(eq(usersTable.username, "admin"))
      .limit(1);

    if (existing) return;

    const passwordHash = await bcrypt.hash("admin", 10);
    await db.insert(usersTable).values({
      username: "admin",
      passwordHash,
      role: "admin",
    });
    logger.info("Admin user created (username: admin, password: admin)");
  } catch (err) {
    logger.error({ err }, "Failed to seed admin user");
  }
}
