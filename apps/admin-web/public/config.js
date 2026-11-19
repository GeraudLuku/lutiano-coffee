// Local development: empty, so the app falls back to VITE_API_BASE or the localhost default.
// In the container this file is replaced at start-up (see docker/40-runtime-config.sh).
window.__LUTIANO__ = { apiBase: "http://localhost:8080/api/v1" };
