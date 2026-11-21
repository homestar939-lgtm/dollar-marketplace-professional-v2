import test from 'node:test'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'

const cwd = new URL('..', import.meta.url).pathname
const baseline = {
  ...process.env,
  NODE_ENV: 'production',
  DATABASE_URL: 'postgresql://release:secret@localhost:5432/dollar',
  JWT_SECRET: 'a'.repeat(48),
  CORS_ORIGINS: 'http://192.168.1.50:5173'
}

function loadConfig(overrides = {}) {
  return spawnSync(process.execPath, ['--input-type=module', '-e', "import('./src/config/env.js')"], {
    cwd,
    env: { ...baseline, ...overrides },
    encoding: 'utf8'
  })
}

test('accepts explicit production LAN HTTP origins and strong secrets', () => {
  const result = loadConfig()
  assert.equal(result.status, 0, result.stderr)
})

test('rejects wildcard, loopback, malformed origins, and weak JWT secrets', () => {
  const invalidConfigurations = [
    [{ CORS_ORIGINS: '*' }, 'CORS_ORIGINS must list explicit local-network origins'],
    [{ CORS_ORIGINS: 'http://localhost:5173' }, 'CORS_ORIGINS entries must be exact LAN HTTP(S) origins'],
    [{ CORS_ORIGINS: 'http://127.0.0.1:5173' }, 'CORS_ORIGINS entries must be exact LAN HTTP(S) origins'],
    [{ CORS_ORIGINS: 'http://192.168.1.50:5173/app' }, 'CORS_ORIGINS entries must be exact LAN HTTP(S) origins'],
    [{ JWT_SECRET: 'too-short-secret' }, 'JWT_SECRET must be at least 32 characters']
  ]

  for (const [overrides, message] of invalidConfigurations) {
    const result = loadConfig(overrides)
    assert.notEqual(result.status, 0, `configuration should fail: ${JSON.stringify(overrides)}`)
    assert.match(result.stderr, new RegExp(message.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
