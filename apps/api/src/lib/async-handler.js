/** Express 5 forwards async rejections, but explicit wrapping keeps intent clear. */
export const asyncHandler = (fn) => (req, res, next) => {
  Promise.resolve(fn(req, res, next)).catch(next)
}
