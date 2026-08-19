import multer from "multer";
import { AppError, createErrorResponse } from "./errors.js";
import { logError, logInfo } from "./logger.js";

const MAX_IMAGE_SIZE_BYTES = 8 * 1024 * 1024;
const ALLOWED_IMAGE_MIME_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/heic",
  "image/heif",
  "image/webp",
]);

export const recipeUpload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: MAX_IMAGE_SIZE_BYTES,
    files: 1,
  },
  fileFilter: (_request, file, callback) => {
    if (!ALLOWED_IMAGE_MIME_TYPES.has(file.mimetype)) {
      callback(new AppError(400, "INVALID_IMAGE_MIME_TYPE", "image must be a supported image file."));
      return;
    }

    callback(null, true);
  },
});

export function requestLogger(request, _response, next) {
  logInfo("Incoming request", {
    method: request.method,
    path: request.path,
    contentType: request.headers["content-type"],
  });
  next();
}

export function notFoundHandler(_request, _response, next) {
  next(new AppError(404, "NOT_FOUND", "The requested endpoint was not found."));
}

export function errorHandler(error, _request, response, _next) {
  if (error instanceof multer.MulterError) {
    if (error.code === "LIMIT_FILE_SIZE") {
      const sizeError = new AppError(400, "IMAGE_TOO_LARGE", "image must be 8 MB or smaller.");
      response.status(sizeError.status).json(createErrorResponse(sizeError));
      return;
    }

    const uploadError = new AppError(400, "MULTIPART_UPLOAD_ERROR", "The multipart upload could not be processed.");
    response.status(uploadError.status).json(createErrorResponse(uploadError));
    return;
  }

  if (error instanceof AppError) {
    response.status(error.status).json(createErrorResponse(error));
    return;
  }

  logError("Unhandled server error", {
    name: error?.name,
    message: error?.message,
    stack: error?.stack,
  });

  const serverError = new AppError(500, "INTERNAL_SERVER_ERROR", "The server could not complete the request.");
  response.status(serverError.status).json(createErrorResponse(serverError));
}
