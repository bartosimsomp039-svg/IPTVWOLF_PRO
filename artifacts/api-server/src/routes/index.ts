import { Router, type IRouter } from "express";
import healthRouter from "./health";
import authRouter from "./auth";
import accountsRouter from "./accounts";
import cacheRouter from "./cache";
import statsRouter from "./stats";
import usersRouter from "./users";
import { requireAuth, requireAdmin } from "../middlewares/requireAuth";

const router: IRouter = Router();

router.use(healthRouter);
router.use(authRouter);

router.use(requireAuth);

router.use(statsRouter);
router.use(accountsRouter);
router.use(cacheRouter);

router.use(requireAdmin);
router.use(usersRouter);

export default router;
