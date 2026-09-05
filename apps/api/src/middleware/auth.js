import { verifyToken } from '../lib/auth.js';
export function auth(required=false){
  return (req,res,next)=>{
    const header=req.headers.authorization||'';
    const token=header.startsWith('Bearer ')?header.slice(7):null;
    if(!token){ if(required) return res.status(401).json({error:'Authentication required'}); return next(); }
    try{ req.user=verifyToken(token); next(); } catch { if(required) return res.status(401).json({error:'Invalid token'}); next(); }
  };
}
export function roles(...allowed){
  return (req,res,next)=>allowed.includes(req.user?.role)?next():res.status(403).json({error:'Forbidden'});
}
