import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { createServer } from "node:net";
import { symlinkSync, unlinkSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const pageDir = fileURLToPath(new URL("./", import.meta.url));

async function freePort() {
  const server = createServer();
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const port = server.address().port;
  await new Promise((resolve) => server.close(resolve));
  return port;
}

test("landing server confines files and sets security headers", async (t) => {
  const port = await freePort();
  const child = spawn(process.execPath, [join(pageDir, "serve.mjs"), String(port)], {
    stdio: ["ignore", "pipe", "pipe"],
  });
  t.after(() => child.kill());
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("server did not start")), 5_000);
    child.stdout.once("data", () => { clearTimeout(timer); resolve(); });
    child.once("exit", (code) => { clearTimeout(timer); reject(new Error(`server exited: ${code}`)); });
  });

  const base = `http://127.0.0.1:${port}`;
  const page = await fetch(base);
  assert.equal(page.status, 200);
  assert.match(page.headers.get("content-type"), /^text\/html/);
  assert.match(page.headers.get("content-security-policy"), /object-src 'none'/);
  assert.equal(page.headers.get("x-content-type-options"), "nosniff");
  assert.equal(page.headers.get("x-frame-options"), "DENY");
  assert.equal((await fetch(base, { method: "HEAD" })).status, 200);
  assert.equal((await fetch(base, { method: "POST" })).status, 405);
  assert.equal((await fetch(`${base}/%2e%2e%2fSECURITY.md`)).status, 403);

  const link = join(pageDir, "dist", ".security-test-link");
  try {
    symlinkSync(join(pageDir, ".."), link, process.platform === "win32" ? "junction" : "dir");
  } catch (error) {
    if (error.code === "EPERM") {
      t.diagnostic("symlink check skipped: Windows did not allow creating a test symlink");
      return;
    }
    throw error;
  }
  try {
    assert.equal((await fetch(`${base}/.security-test-link/SECURITY.md`)).status, 403);
  } finally {
    unlinkSync(link);
  }
});
