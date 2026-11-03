import * as service from './demo-auth.service.js'

// التحقق من الدور يتم داخل الخدمة بـ badRequest (400) لا بـ zod enum (422).
export const login = async (req, res, next) => {
  try {
    res.json(await service.login({ role: req.body?.role, req }))
  } catch (error) {
    next(error)
  }
}
