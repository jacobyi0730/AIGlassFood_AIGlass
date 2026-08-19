import { buildUserPrompt, SYSTEM_PROMPT } from "./prompt-builder.js";

export function createRecipeService({ config, nvidiaClient }) {
  return {
    async createRecipe({ requestId, imageBuffer, imageMimeType, prompt, language }) {
      const userPrompt = buildUserPrompt({ prompt, language });

      return nvidiaClient.createRecipeCompletion({
        requestId,
        imageBuffer,
        imageMimeType,
        prompt,
        language,
        systemPrompt: SYSTEM_PROMPT,
        userPrompt,
        model: config.nvidiaModel,
      });
    },
  };
}
