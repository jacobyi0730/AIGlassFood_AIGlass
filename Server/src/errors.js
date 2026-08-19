export class AppError extends Error {
  constructor(status, code, message) {
    super(message);
    this.name = "AppError";
    this.status = status;
    this.code = code;
  }
}

export function createErrorResponse(error) {
  return {
    message: error.message || "An unexpected server error occurred.",
    code: error.code || "INTERNAL_SERVER_ERROR",
  };
}
