import { env } from '../../config/env.js'
import { unauthenticated, unprocessable } from '../../lib/errors.js'

/**
 * Verifies a Google ID token with Google itself and checks it was minted for
 * our client id. The email alone is never treated as proof of identity.
 */
export async function verifyGoogleIdToken(idToken) {
  const response = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`, { method: 'GET' })
  if (!response.ok) throw unauthenticated('Google rejected the ID token')
  const payload = await response.json()

  if (!env.GOOGLE_CLIENT_ID) throw unprocessable('Google Sign-In is not configured (GOOGLE_CLIENT_ID missing)')
  if (payload.aud !== env.GOOGLE_CLIENT_ID) throw unauthenticated('Google token audience mismatch')
  if (!payload.sub) throw unauthenticated('Google token has no subject')
  if (payload.email_verified !== 'true' && payload.email_verified !== true) throw unauthenticated('Google email is not verified')
  if (payload.exp && Number(payload.exp) * 1000 < Date.now()) throw unauthenticated('Google token has expired')

  return { subject: payload.sub, email: String(payload.email).toLowerCase(), name: payload.name || payload.email, picture: payload.picture || null }
}
