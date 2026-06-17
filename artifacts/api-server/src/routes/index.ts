import { Router, type IRouter } from "express";
import healthRouter from "./health";
import accountsRouter from "./accounts";
import cacheRouter from "./cache";
import statsRouter from "./stats";

const router: IRouter = Router();

router.use(healthRouter);
router.use(statsRouter);
router.use(accountsRouter);
router.use(cacheRouter);

export default router;
