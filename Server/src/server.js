import { createServer } from "node:http";
import { config } from "./config.js";
import { logError, logInfo } from "./logger.js";
import { createApp } from "./app.js";

const app = createApp(config);
const server = createServer(app);

const shutdownSignals = ["SIGINT", "SIGTERM"];
let shuttingDown = false;

function shutdown(signal) {
  if (shuttingDown) {
    return;
  }

  shuttingDown = true;
  logInfo(`Received ${signal}. Shutting down server.`);
  server.close((error) => {
    if (error) {
      logError("Failed to shut down cleanly", { message: error.message });
      process.exit(1);
      return;
    }

    logInfo("Server shut down cleanly.");
    process.exit(0);
  });
}

server.listen(config.serverPort, "0.0.0.0", () => {
  logInfo("FoodServer is listening.", {
    host: "0.0.0.0",
    port: config.serverPort,
    model: config.nvidiaModel,
  });
});

for (const signal of shutdownSignals) {
  process.on(signal, () => shutdown(signal));
}

process.on("uncaughtException", (error) => {
  logError("Uncaught exception", { message: error.message, stack: error.stack });
  shutdown("uncaughtException");
});

process.on("unhandledRejection", (reason) => {
  logError("Unhandled rejection", { reason });
  shutdown("unhandledRejection");
});
