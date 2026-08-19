import dotenv from "dotenv";

dotenv.config();

const DEFAULT_BASE_URL = "https://integrate.api.nvidia.com/v1";
const DEFAULT_MODEL = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning";
const DEFAULT_PORT = 8000;

export const config = {
  serverPort: Number(process.env.SERVER_PORT || DEFAULT_PORT),
  nvidiaApiKey: process.env.NVIDIA_API_KEY || "",
  nvidiaBaseUrl: process.env.NVIDIA_BASE_URL || DEFAULT_BASE_URL,
  nvidiaModel: process.env.NVIDIA_MODEL || DEFAULT_MODEL,
};

export function validateServerConfig() {
  if (!Number.isInteger(config.serverPort) || config.serverPort <= 0) {
    throw new Error("SERVER_PORT must be a positive integer.");
  }

  if (!config.nvidiaBaseUrl) {
    throw new Error("NVIDIA_BASE_URL must be configured.");
  }

  if (!config.nvidiaModel) {
    throw new Error("NVIDIA_MODEL must be configured.");
  }
}
