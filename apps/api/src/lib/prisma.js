import { PrismaClient } from '@prisma/client'
import { env } from '../config/env.js'

const globalForPrisma = globalThis

export const prisma =
  globalForPrisma.__dollarPrisma ??
  new PrismaClient({
    log: env.isTest ? [] : ['warn', 'error']
  })

if (!env.isProd) globalForPrisma.__dollarPrisma = prisma
