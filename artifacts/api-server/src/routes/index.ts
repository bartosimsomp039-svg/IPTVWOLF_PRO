import { Router, type IRouter } from "express";
import healthRouter from "./health";
import accountsRouter from "./accounts";
import cacheRouter from "./cache";
import statsRouter from "./stats";
import { requireAuth } from "../middlewares/requireAuth";

const router: IRouter = Router();

router.use(healthRouter);

router.use(requireAuth);

router.use(statsRouter);
router.use(accountsRouter);
router.use(cacheRouter);

export default router;
