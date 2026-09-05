const BASE=import.meta.env.VITE_API_URL||'http://localhost:4000/api';
export async function api<T=any>(path:string,options:RequestInit={}){const token=localStorage.getItem('dollar_token');const res=await fetch(BASE+path,{...options,headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{}) ,...(options.headers||{})}});const data=await res.json().catch(()=>({}));if(!res.ok)throw new Error(data.error||'Request failed');return data as T;}
export const getCategories=()=>api('/catalog/categories');
export const getProducts=(params:string='')=>api(`/catalog/products${params}`);
export const getProduct=(id:string)=>api(`/catalog/products/${id}`);
