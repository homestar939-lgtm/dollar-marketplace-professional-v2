import { createApp } from './app.js'
import { env } from './config/env.js'
import { prisma } from './lib/prisma.js'

const app = createApp()
const server = app.listen(env.PORT, () => {
  console.log(`[dollar-api] listening on :${env.PORT} (${env.NODE_ENV})`)
})

async function shutdown(signal) {
  console.log(`[dollar-api] ${signal} received, shutting down`)
  server.close(async () => {
    await prisma.$disconnect()
    process.exit(0)
  })
  setTimeout(() => process.exit(1), 10000).unref()
}

process.on('SIGTERM', () => shutdown('SIGTERM'))
process.on('SIGINT', () => shutdown('SIGINT'))
