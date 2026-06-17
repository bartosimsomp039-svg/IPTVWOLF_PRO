import { Router, type IRouter } from "express";
import { eq } from "drizzle-orm";
import { db, accountsTable } from "@workspace/db";
import { GetAccountCacheParams, ClearAccountCacheParams } from "@workspace/api-zod";

const TTL_MS = 2 * 24 * 60 * 60 * 1000; // 48 hours (matches Android CacheManager)

function buildCacheStatus(account: typeof accountsTable.$inferSelect) {
  const lastUpdated = account.cacheUpdatedAt ? account.cacheUpdatedAt.toISOString() : null;
  const isExpired = !account.cacheUpdatedAt
    ? true
    : Date.now() - account.cacheUpdatedAt.getTime() > TTL_MS;
  const remaining = account.cacheUpdatedAt
    ? Math.max(0, TTL_MS - (Date.now() - account.cacheUpdatedAt.getTime()))
    : 0;
  const hoursRemaining = remaining > 0 ? Math.floor(remaining / (1000 * 60 * 60)) : 0;

  // hasLive/Movies/Series inferred from whether cache was ever written
  const hasAny = !!account.cacheUpdatedAt;
  return {
    accountId: account.id,
    hasLive: hasAny,
    hasMovies: hasAny,
    hasSeries: hasAny,
    lastUpdated,
    isExpired,
    hoursRemaining: hoursRemaining > 0 ? hoursRemaining : null,
  };
}

const router: IRouter = Router();

router.get("/accounts/:id/cache", async (req, res) => {
  try {
    const { id } = GetAccountCacheParams.parse({ id: Number(req.params.id) });
    const [account] = await db.select().from(accountsTable).where(eq(accountsTable.id, id));
    if (!account) {
      res.status(404).json({ error: "Account not found" });
      return;
    }
    res.json(buildCacheStatus(account));
  } catch (err) {
    req.log.error({ err }, "getAccountCache error");
    res.status(500).json({ error: "Internal server error" });
  }
});

router.delete("/accounts/:id/cache", async (req, res) => {
  try {
    const { id } = ClearAccountCacheParams.parse({ id: Number(req.params.id) });
    // Mark cache as expired by clearing the cacheUpdatedAt timestamp
    const [account] = await db
      .update(accountsTable)
      .set({ cacheUpdatedAt: null })
      .where(eq(accountsTable.id, id))
      .returning();
    if (!account) {
      res.status(404).json({ error: "Account not found" });
      return;
    }
    res.json(buildCacheStatus(account));
  } catch (err) {
    req.log.error({ err }, "clearAccountCache error");
    res.status(500).json({ error: "Internal server error" });
  }
});

export default router;
