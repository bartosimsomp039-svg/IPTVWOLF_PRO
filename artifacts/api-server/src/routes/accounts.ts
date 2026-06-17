import { Router, type IRouter } from "express";
import { eq } from "drizzle-orm";
import { db, accountsTable } from "@workspace/db";
import {
  CreateAccountBody,
  UpdateAccountBody,
  GetAccountParams,
  UpdateAccountParams,
  DeleteAccountParams,
} from "@workspace/api-zod";

const router: IRouter = Router();

router.get("/accounts", async (req, res) => {
  try {
    const accounts = await db.select().from(accountsTable).orderBy(accountsTable.createdAt);
    res.json(
      accounts.map((a) => ({
        ...a,
        cacheUpdatedAt: a.cacheUpdatedAt ? a.cacheUpdatedAt.toISOString() : null,
        createdAt: a.createdAt.toISOString(),
      }))
    );
  } catch (err) {
    req.log.error({ err }, "listAccounts error");
    res.status(500).json({ error: "Internal server error" });
  }
});

router.post("/accounts", async (req, res) => {
  try {
    const parsed = CreateAccountBody.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ error: "Invalid input" });
      return;
    }
    const [account] = await db.insert(accountsTable).values(parsed.data).returning();
    res.status(201).json({
      ...account,
      cacheUpdatedAt: account.cacheUpdatedAt ? account.cacheUpdatedAt.toISOString() : null,
      createdAt: account.createdAt.toISOString(),
    });
  } catch (err) {
    req.log.error({ err }, "createAccount error");
    res.status(500).json({ error: "Internal server error" });
  }
});

router.get("/accounts/:id", async (req, res) => {
  try {
    const { id } = GetAccountParams.parse({ id: Number(req.params.id) });
    const [account] = await db.select().from(accountsTable).where(eq(accountsTable.id, id));
    if (!account) {
      res.status(404).json({ error: "Account not found" });
      return;
    }
    res.json({
      ...account,
      cacheUpdatedAt: account.cacheUpdatedAt ? account.cacheUpdatedAt.toISOString() : null,
      createdAt: account.createdAt.toISOString(),
    });
  } catch (err) {
    req.log.error({ err }, "getAccount error");
    res.status(500).json({ error: "Internal server error" });
  }
});

router.patch("/accounts/:id", async (req, res) => {
  try {
    const { id } = UpdateAccountParams.parse({ id: Number(req.params.id) });
    const parsed = UpdateAccountBody.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ error: "Invalid input" });
      return;
    }
    const [account] = await db
      .update(accountsTable)
      .set(parsed.data)
      .where(eq(accountsTable.id, id))
      .returning();
    if (!account) {
      res.status(404).json({ error: "Account not found" });
      return;
    }
    res.json({
      ...account,
      cacheUpdatedAt: account.cacheUpdatedAt ? account.cacheUpdatedAt.toISOString() : null,
      createdAt: account.createdAt.toISOString(),
    });
  } catch (err) {
    req.log.error({ err }, "updateAccount error");
    res.status(500).json({ error: "Internal server error" });
  }
});

router.delete("/accounts/:id", async (req, res) => {
  try {
    const { id } = DeleteAccountParams.parse({ id: Number(req.params.id) });
    const result = await db.delete(accountsTable).where(eq(accountsTable.id, id)).returning();
    if (result.length === 0) {
      res.status(404).json({ error: "Account not found" });
      return;
    }
    res.status(204).send();
  } catch (err) {
    req.log.error({ err }, "deleteAccount error");
    res.status(500).json({ error: "Internal server error" });
  }
});

export default router;
