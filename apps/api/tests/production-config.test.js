import test from 'node:test'
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'

const cwd = new URL('..', import.meta.url).pathname
const baseline = {
  ...process.env,
  NODE_ENV: 'production',
  DATABASE_URL: 'postgresql://release:secret@localhost:5432/dollar',
  JWT_SECRET: 'a'.repeat(48),
  DEMO_MODE: 'false',
  EXPOSE_TOKENS: 'false',
  CORS_ORIGINS: 'https://market.example.com'
}

function loadConfig(overrides = {}) {
  return spawnSync(process.execPath, ['--input-type=module', '-e', "import('./src/config/env.js')"], {
    cwd,
    env: { ...baseline, ...overrides },
    encoding: 'utf8'
  })
}

test('accepts an explicitly safe production configuration', () => {
  const result = loadConfig()
  assert.equal(result.status, 0, result.stderr)
})

test('refuses production demo mode, token exposure, wildcard or insecure CORS, and weak JWT secrets', () => {
  const invalidConfigurations = [
    [{ DEMO_MODE: 'true' }, 'DEMO_MODE must be false'],
    [{ EXPOSE_TOKENS: 'true' }, 'EXPOSE_TOKENS must be false'],
    [{ CORS_ORIGINS: '*' }, 'CORS_ORIGINS must list explicit HTTPS origins'],
    [{ CORS_ORIGINS: 'http://market.example.com' }, 'CORS_ORIGINS entries must be exact HTTPS origins'],
    [{ JWT_SECRET: 'too-short-secret' }, 'JWT_SECRET must be at least 32 characters']
  ]

  for (const [overrides, message] of invalidConfigurations) {
    const result = loadConfig(overrides)
    assert.notEqual(result.status, 0, `configuration should fail: ${JSON.stringify(overrides)}`)
    assert.match(result.stderr, new RegExp(message.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
})
