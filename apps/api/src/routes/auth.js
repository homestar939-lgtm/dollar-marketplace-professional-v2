import { Router } from 'express';
import { prisma } from '../lib/prisma.js';
import { comparePassword, hashPassword, signUser } from '../lib/auth.js';
import { z } from 'zod';
const r=Router();

r.post('/register',async(req,res)=>{
  const s=z.object({email:z.string().email(),password:z.string().min(8),name:z.string().min(2),role:z.enum(['BUYER','SELLER']).default('BUYER')});
  const x=s.parse(req.body);
  if(await prisma.user.findUnique({where:{email:x.email}})) return res.status(409).json({error:'Email already exists'});
  const user=await prisma.user.create({data:{email:x.email,name:x.name,passwordHash:await hashPassword(x.password),role:x.role}});
  if(x.role==='SELLER') await prisma.store.create({data:{ownerId:user.id,name:`متجر ${user.name}`}});
  res.json({token:signUser(user),user:{id:user.id,email:user.email,name:user.name,role:user.role}});
});
r.post('/login',async(req,res)=>{
  const x=z.object({email:z.string().email(),password:z.string()}).parse(req.body);
  const user=await prisma.user.findUnique({where:{email:x.email}});
  if(!user || !(await comparePassword(x.password,user.passwordHash))) return res.status(401).json({error:'Invalid credentials'});
  res.json({token:signUser(user),user:{id:user.id,email:user.email,name:user.name,role:user.role}});
});
export default r;
