#!/usr/bin/env node
/**
 * Starts ng serve on the first available port starting from BASE_PORT.
 * If 4200 is taken, tries 4201, 4202 … up to MAX_PORT.
 */
const net  = require('net');
const { execSync } = require('child_process');

const BASE_PORT = 4200;
const MAX_PORT  = 4210;

function isPortAvailable(port) {
  return new Promise((resolve) => {
    const server = net.createServer();
    server.once('error', () => resolve(false));
    server.once('listening', () => {
      server.close();
      resolve(true);
    });
    server.listen(port, '127.0.0.1');
  });
}

async function findPort() {
  for (let port = BASE_PORT; port <= MAX_PORT; port++) {
    if (await isPortAvailable(port)) return port;
  }
  throw new Error(`No available port in range ${BASE_PORT}-${MAX_PORT}`);
}

findPort().then((port) => {
  if (port !== BASE_PORT) {
    console.log(`Port ${BASE_PORT} is in use — starting on port ${port} instead`);
  }
  execSync(`npx ng serve --port ${port}`, { stdio: 'inherit' });
}).catch((err) => {
  console.error(err.message);
  process.exit(1);
});
