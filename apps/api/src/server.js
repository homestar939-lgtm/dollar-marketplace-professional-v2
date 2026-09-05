import express from 'express';
import cors from 'cors';
import authRoutes from './routes/auth.js';
import catalogRoutes from './routes/catalog.js';
import sellerRoutes from './routes/seller.js';
import commerceRoutes from './routes/commerce.js';
import messagingRoutes from './routes/messaging.js';
import adminRoutes from './routes/admin.js';

const app=express();
app.use(cors());
app.use(express.json({limit:'2mb'}));
app.get('/health',(req,res)=>res.json({ok:true,app:'Dollar Marketplace',version:'1.0.0'}));
app.use('/api/auth',authRoutes);
app.use('/api/catalog',catalogRoutes);
app.use('/api/seller',sellerRoutes);
app.use('/api/commerce',commerceRoutes);
app.use('/api/messaging',messagingRoutes);
app.use('/api/admin',adminRoutes);
app.use((err,req,res,next)=>{
  console.error(err);
  if(err?.name==='ZodError')return res.status(400).json({error:'Validation failed',details:err.issues});
  res.status(500).json({error:'Internal server error'});
});
const port=Number(process.env.PORT||4000);
app.listen(port,()=>console.log(`Dollar API running on http://localhost:${port}`));
