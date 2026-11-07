import { createReadStream, realpathSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, isAbsolute, join, normalize, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

const root = realpathSync(normalize(fileURLToPath(new URL("./dist/", import.meta.url))));
const types = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
};
const port = Number(process.argv[2]) || 4173;
const securityHeaders = {
  "Content-Security-Policy": "default-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src https://fonts.gstatic.com; img-src 'self' data:; script-src 'self'; connect-src 'none'; object-src 'none'; base-uri 'self'; form-action 'none'; frame-ancestors 'none'",
  "Referrer-Policy": "strict-origin-when-cross-origin",
  "X-Content-Type-Options": "nosniff",
  "X-Frame-Options": "DENY",
};
const escapesRoot = (pathFromRoot) => pathFromRoot === ".." || pathFromRoot.startsWith(`..${sep}`) || isAbsolute(pathFromRoot);

createServer((request, response) => {
  if (request.method !== "GET" && request.method !== "HEAD") {
    response.writeHead(405, { ...securityHeaders, Allow: "GET, HEAD" }).end("Method not allowed");
    return;
  }

  let pathname;
  try {
    pathname = decodeURIComponent(new URL(request.url, "http://localhost").pathname);
  } catch {
    response.writeHead(400, securityHeaders).end("Bad request");
    return;
  }

  const requested = pathname === "/" ? "index.html" : pathname.slice(1);
  const file = normalize(join(root, requested));
  const pathFromRoot = relative(root, file);

  if (escapesRoot(pathFromRoot)) {
    response.writeHead(403, securityHeaders).end("Forbidden");
    return;
  }

  try {
    const target = realpathSync(statSync(file).isDirectory() ? join(file, "index.html") : file);
    const resolvedPathFromRoot = relative(root, target);
    if (escapesRoot(resolvedPathFromRoot)) {
      response.writeHead(403, securityHeaders).end("Forbidden");
      return;
    }
    if (!statSync(target).isFile()) {
      response.writeHead(404, securityHeaders).end("Not found");
      return;
    }
    response.writeHead(200, {
      ...securityHeaders,
      "Content-Type": types[extname(target)] ?? "application/octet-stream",
    });
    if (request.method === "HEAD") {
      response.end();
      return;
    }
    createReadStream(target).pipe(response);
  } catch {
    response.writeHead(404, securityHeaders).end("Not found");
  }
}).listen(port, "127.0.0.1", () => {
  console.log(`InstBlock landing page: http://127.0.0.1:${port}`);
});
