import test from "node:test";
import assert from "node:assert/strict";
import { createNvidiaClient } from "../src/nvidia-client.js";
import { AppError } from "../src/errors.js";

const config = {
  nvidiaApiKey: "test-key",
  nvidiaBaseUrl: "https://integrate.api.nvidia.com/v1",
  nvidiaModel: "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",
};

test("nvidia client returns recipe text from a successful response", async () => {
  const client = createNvidiaClient({
    config,
    fetchImpl: async () =>
      new Response(
          JSON.stringify({
            model: config.nvidiaModel,
            choices: [
              {
                message: {
                  content: "1. Recipe Name\n2. Ingredients Seen In Image",
                },
              },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
      ),
  });

  const result = await client.createRecipeCompletion({
    imageBuffer: Buffer.from("image"),
    imageMimeType: "image/png",
    prompt: "Healthy dinner",
    language: "ko-KR",
    systemPrompt: "system",
    userPrompt: "user",
  });

  assert.match(result.recipeText, /Recipe Name/i);
});

test("nvidia client maps auth errors", async () => {
  const client = createNvidiaClient({
    config,
    fetchImpl: async () =>
      new Response(JSON.stringify({ error: "unauthorized" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      }),
  });

  await assert.rejects(
      () =>
        client.createRecipeCompletion({
          imageBuffer: Buffer.from("image"),
          imageMimeType: "image/png",
          prompt: "Healthy dinner",
          language: "ko-KR",
          systemPrompt: "system",
          userPrompt: "user",
        }),
      (error) => error instanceof AppError && error.code === "NVIDIA_AUTH_FAILED",
  );
});

test("nvidia client maps empty text responses", async () => {
  const client = createNvidiaClient({
    config,
    fetchImpl: async () =>
      new Response(
          JSON.stringify({
            choices: [{ message: { content: "" } }],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
      ),
  });

  await assert.rejects(
      () =>
        client.createRecipeCompletion({
          imageBuffer: Buffer.from("image"),
          imageMimeType: "image/png",
          prompt: "Healthy dinner",
          language: "ko-KR",
          systemPrompt: "system",
          userPrompt: "user",
        }),
      (error) => error instanceof AppError && error.code === "NVIDIA_EMPTY_RESPONSE",
  );
});

test("nvidia client retries transient upstream failures", async () => {
  let attempts = 0;
  const client = createNvidiaClient({
    config,
    fetchImpl: async () => {
      attempts += 1;
      if (attempts === 1) {
        return new Response(JSON.stringify({ error: "temporary" }), {
          status: 503,
          headers: { "Content-Type": "application/json" },
        });
      }

      return new Response(
          JSON.stringify({
            choices: [{ message: { content: "1. Recipe Name" } }],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
      );
    },
  });

  const result = await client.createRecipeCompletion({
    imageBuffer: Buffer.from("image"),
    imageMimeType: "image/png",
    prompt: "Healthy dinner",
    language: "ko-KR",
    systemPrompt: "system",
    userPrompt: "user",
  });

  assert.equal(attempts, 2);
  assert.match(result.recipeText, /Recipe Name/i);
});
