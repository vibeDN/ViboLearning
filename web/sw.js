/* Minimal offline cache. Bump VERSION to force a refresh. */
const VERSION = "vibolearning-v1";
const ASSETS = ["./", "index.html", "courses/cpp-basics.json", "manifest.webmanifest", "icon.svg"];

self.addEventListener("install", e => {
  e.waitUntil(caches.open(VERSION).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting()));
});
self.addEventListener("activate", e => {
  e.waitUntil(
    caches.keys().then(keys => Promise.all(keys.filter(k => k !== VERSION).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});
self.addEventListener("fetch", e => {
  const { request } = e;
  if (request.method !== "GET") return;
  // Never cache the compiler API.
  if (request.url.includes("wandbox.org")) return;
  e.respondWith(
    caches.match(request).then(hit => hit || fetch(request).then(res => {
      if (res.ok && new URL(request.url).origin === location.origin) {
        const copy = res.clone();
        caches.open(VERSION).then(c => c.put(request, copy));
      }
      return res;
    }).catch(() => caches.match("index.html")))
  );
});
