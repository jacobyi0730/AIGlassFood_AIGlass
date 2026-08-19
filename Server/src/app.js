import express from "express";
import { validateServerConfig } from "./config.js";
import { requestLogger, notFoundHandler, errorHandler } from "./middleware.js";
import { createRouter } from "./routes.js";
import { createRecipeService } from "./recipe-service.js";
import { createNvidiaClient } from "./nvidia-client.js";

export function createApp(config, dependencies = {}) {
  validateServerConfig();

  const app = express();
  const nvidiaClient =
      dependencies.nvidiaClient || createNvidiaClient({ config, fetchImpl: dependencies.fetchImpl });
  const recipeService =
      dependencies.recipeService || createRecipeService({ config, nvidiaClient });

  app.disable("x-powered-by");
  app.use(requestLogger);
  app.use(createRouter({ config, recipeService }));
  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}
