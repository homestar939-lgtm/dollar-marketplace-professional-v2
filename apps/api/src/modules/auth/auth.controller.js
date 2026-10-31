import * as service from './auth.service.js'
import { verifyGoogleIdToken } from './google.js'
import { asyncHandler } from '../../lib/async-handler.js'

export const register = asyncHandler(async (req, res) => {
  const result = await service.register({ ...req.body, req })
  res.status(201).json(result)
})

export const verifyEmail = asyncHandler(async (req, res) => {
  res.json({ user: await service.verifyEmail(req.body) })
})

export const login = asyncHandler(async (req, res) => {
  res.json(await service.login({ ...req.body, req }))
})

export const refresh = asyncHandler(async (req, res) => {
  res.json(await service.refresh({ ...req.body, req }))
})

export const logout = asyncHandler(async (req, res) => {
  res.json(await service.logout({ sessionId: req.session.id }))
})

export const me = asyncHandler(async (req, res) => {
  res.json({ user: service.publicUser(req.user) })
})

export const sessions = asyncHandler(async (req, res) => {
  res.json({ data: await service.listSessions(req.user.id) })
})

export const forgotPassword = asyncHandler(async (req, res) => {
  res.json(await service.forgotPassword(req.body))
})

export const resetPassword = asyncHandler(async (req, res) => {
  res.json(await service.resetPassword(req.body))
})

export const changePassword = asyncHandler(async (req, res) => {
  res.json(await service.changePassword({ userId: req.user.id, ...req.body }))
})

export const google = asyncHandler(async (req, res) => {
  const verifier = req.app.locals.googleVerifier || verifyGoogleIdToken
  const profile = await verifier(req.body.idToken)
  res.json(await service.loginWithGoogle({ profile, req }))
})
