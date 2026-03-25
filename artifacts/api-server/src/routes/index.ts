import { Router, type IRouter } from "express";
import healthRouter from "./health";
import vehiclesRouter from "./vehicles";
import routesRouter from "./routes";

const router: IRouter = Router();

router.use(healthRouter);
router.use(vehiclesRouter);
router.use(routesRouter);

export default router;
