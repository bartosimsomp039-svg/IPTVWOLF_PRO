import { Router, type IRouter } from "express";
import { db, accountsTable } from "@workspace/db";

const TTL_MS = 2 * 24 * 60 * 60 * 1000;

const router: IRouter = Router();

router.get("/stats", async (req, res) => {
  try {
    const accounts = await db.select().from(accountsTable);
    const total = accounts.length;
    const active = accounts.filter((a) => a.active).length;
    const xtream = accounts.filter((a) => a.type === "xtream").length;
    const m3u = accounts.filter((a) => a.type === "m3u").length;
    const cacheActive = accounts.filter(
      (a) => a.cacheUpdatedAt && Date.now() - a.cacheUpdatedAt.getTime() <= TTL_MS
    ).length;
    const cacheExpired = total - cacheActive;

    res.json({
      totalAccounts: total,
      activeAccounts: active,
      xtreamCount: xtream,
      m3uCount: m3u,
      cacheActiveCount: cacheActive,
      cacheExpiredCount: cacheExpired,
    });
  } catch (err) {
    req.log.error({ err }, "getStats error");
    res.status(500).json({ error: "Internal server error" });
  }
});

export default router;
