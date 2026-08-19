import { AppError } from "./errors.js";
import { logError, logInfo } from "./logger.js";

const DEFAULT_TIMEOUT_MS = 45000;
const MAX_RETRY_ATTEMPTS = 2;

function toDataUrl(buffer, mimeType) {
  return `data:${mimeType};base64,${buffer.toString("base64")}`;
}

function buildEndpoint(baseUrl) {
  return new URL("chat/completions", `${baseUrl.replace(/\/+$/, "")}/`).toString();
}

function shouldRetryStatus(status) {
  return status === 408 || status === 409 || status === 429 || status >= 500;
}

function shouldRetryError(error) {
  return error?.name === "AbortError" || error instanceof TypeError;
}

async function parseJsonSafely(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function extractRecipeText(payload) {
  const content = payload?.choices?.[0]?.message?.content;

  if (typeof content === "string" && content.trim()) {
    return content.trim();
  }

  if (Array.isArray(content)) {
    const merged = content
        .map((part) => {
          if (typeof part === "string") return part;
          if (part?.type === "text" && typeof part.text === "string") return part.text;
          return "";
        })
        .join("\n")
        .trim();

    if (merged) {
      return merged;
    }
  }

  return "";
}

function mapUpstreamHttpError(response, payload) {
  if (response.status === 401 || response.status === 403) {
    return new AppError(502, "NVIDIA_AUTH_FAILED", "The recipe service authentication failed.");
  }

  if (response.status === 429) {
    return new AppError(503, "NVIDIA_RATE_LIMITED", "The recipe service is temporarily busy. Please try again.");
  }

  if (response.status === 408 || response.status === 504) {
    return new AppError(504, "NVIDIA_TIMEOUT", "The recipe service took too long to respond.");
  }

  if (response.status >= 500) {
    return new AppError(502, "NVIDIA_UPSTREAM_ERROR", "The recipe service returned an upstream error.");
  }

  if (response.status >= 400) {
    return new AppError(502, "NVIDIA_REQUEST_FAILED", "The recipe service rejected the request.");
  }

  return new AppError(502, "NVIDIA_UPSTREAM_ERROR", payload?.message || "The recipe service returned an unexpected response.");
}

function mapNetworkError(error) {
  if (error?.name === "AbortError") {
    return new AppError(504, "NVIDIA_TIMEOUT", "The recipe service took too long to respond.");
  }

  return new AppError(502, "NVIDIA_NETWORK_ERROR", "The server could not reach the recipe service.");
}

export function createNvidiaClient({ config, fetchImpl = fetch }) {
  return {
    async createRecipeCompletion({ requestId, imageBuffer, imageMimeType, prompt, language, systemPrompt, userPrompt }) {
      if (!config.nvidiaApiKey) {
        throw new AppError(500, "NVIDIA_CONFIG_MISSING", "NVIDIA_API_KEY is not configured on the server.");
      }

      const requestBody = {
        model: config.nvidiaModel,
        messages: [
          {
            role: "system",
            content: systemPrompt,
          },
          {
            role: "user",
            content: [
              {
                type: "text",
                text: userPrompt,
              },
              {
                type: "image_url",
                image_url: {
                  url: toDataUrl(imageBuffer, imageMimeType),
                },
              },
            ],
          },
        ],
        max_tokens: 1024,
        temperature: 0.2,
        top_k: 1,
        stream: false,
        chat_template_kwargs: {
          enable_thinking: false,
        },
      };

      let lastError = null;
      const endpoint = buildEndpoint(config.nvidiaBaseUrl);

      for (let attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt += 1) {
        try {
          const startedAt = Date.now();
          logInfo("Calling NVIDIA recipe API", {
            requestId,
            attempt,
            maxAttempts: MAX_RETRY_ATTEMPTS,
            endpoint,
            model: config.nvidiaModel,
            promptLength: prompt.length,
            language,
            imageMimeType,
            imageBytes: imageBuffer.length,
          });

          const response = await fetchImpl(endpoint, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${config.nvidiaApiKey}`,
              Accept: "application/json",
            },
            body: JSON.stringify(requestBody),
            signal: AbortSignal.timeout(DEFAULT_TIMEOUT_MS),
          });

          const payload = await parseJsonSafely(response);
          const elapsedMs = Date.now() - startedAt;

          logInfo("Received NVIDIA response", {
            requestId,
            attempt,
            status: response.status,
            ok: response.ok,
            elapsedMs,
            payloadKeys: payload ? Object.keys(payload).slice(0, 10) : [],
          });

          if (!response.ok) {
            const mappedError = mapUpstreamHttpError(response, payload);
            if (attempt < MAX_RETRY_ATTEMPTS && shouldRetryStatus(response.status)) {
              logInfo("Retrying NVIDIA request after upstream status", {
                requestId,
                attempt,
                status: response.status,
                mappedCode: mappedError.code,
              });
              lastError = mappedError;
              continue;
            }
            throw mappedError;
          }

          const recipeText = extractRecipeText(payload);
          if (!recipeText) {
            throw new AppError(502, "NVIDIA_EMPTY_RESPONSE", "The recipe service returned an empty response.");
          }

          return {
            recipeText,
            model: payload?.model || config.nvidiaModel,
            language,
            prompt,
          };
        } catch (error) {
          logError("NVIDIA request attempt failed", {
            requestId,
            attempt,
            name: error?.name,
            message: error?.message,
            code: error?.code,
            status: error?.status,
          });

          if (error instanceof AppError) {
            throw error;
          }

          const mappedError = mapNetworkError(error);
          if (attempt < MAX_RETRY_ATTEMPTS && shouldRetryError(error)) {
            logInfo("Retrying NVIDIA request after network error", {
              requestId,
              attempt,
              mappedCode: mappedError.code,
            });
            lastError = mappedError;
            continue;
          }

          throw mappedError;
        }
      }

      throw lastError || new AppError(502, "NVIDIA_UPSTREAM_ERROR", "The recipe service request failed.");
    },
  };
}
