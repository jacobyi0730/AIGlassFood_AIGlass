function timestamp() {
  return new Date().toISOString();
}

export function logInfo(message, context = undefined) {
  if (context) {
    console.log(`[${timestamp()}] INFO ${message}`, context);
    return;
  }

  console.log(`[${timestamp()}] INFO ${message}`);
}

export function logError(message, context = undefined) {
  if (context) {
    console.error(`[${timestamp()}] ERROR ${message}`, context);
    return;
  }

  console.error(`[${timestamp()}] ERROR ${message}`);
}
