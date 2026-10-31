import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
const secret = process.env.JWT_SECRET || 'development-secret';
export async function hashPassword(password){ return bcrypt.hash(password,12); }
export async function comparePassword(password,hash){ return bcrypt.compare(password,hash); }
export function signUser(user){ return jwt.sign({id:user.id,email:user.email,role:user.role},secret,{expiresIn:'7d'}); }
export function verifyToken(token){ return jwt.verify(token,secret); }
