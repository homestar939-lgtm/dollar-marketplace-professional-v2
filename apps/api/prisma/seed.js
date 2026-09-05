import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';
const prisma = new PrismaClient();

async function main() {
  const adminHash = await bcrypt.hash('Admin123!', 12);
  const sellerHash = await bcrypt.hash('Seller123!', 12);
  const buyerHash = await bcrypt.hash('Buyer123!', 12);

  const admin = await prisma.user.upsert({
    where: { email: 'admin@dollar.local' },
    update: {},
    create: { email: 'admin@dollar.local', name: 'مدير دولار', passwordHash: adminHash, role: 'ADMIN', verified: true }
  });
  const seller = await prisma.user.upsert({
    where: { email: 'seller@dollar.local' },
    update: {},
    create: { email: 'seller@dollar.local', name: 'مورد دولار', passwordHash: sellerHash, role: 'SELLER', verified: true }
  });
  const buyer = await prisma.user.upsert({
    where: { email: 'buyer@dollar.local' },
    update: {},
    create: { email: 'buyer@dollar.local', name: 'مشتري دولار', passwordHash: buyerHash, role: 'BUYER', verified: true }
  });

  const permissionKeys = ['catalog.read','catalog.write','orders.read','orders.manage','rfq.manage','offers.manage','disputes.manage','users.manage','analytics.read'];
  for (const key of permissionKeys) await prisma.permission.upsert({where:{key},update:{},create:{key,description:key}});
  for (const name of ['BUYER','SELLER','ADMIN']) await prisma.roleDefinition.upsert({where:{name},update:{},create:{name,description:name}});

  const store = await prisma.store.upsert({
    where: { ownerId: seller.id },
    update: {},
    create: { ownerId: seller.id, name: 'متجر المورد العالمي', description: 'متجر تجريبي للجملة والتجزئة', location: 'صنعاء', verified: true, rating: 4.8 }
  });

  const categoryData = [
    ['سيارات','Cars','cars'],
    ['إلكترونيات','Electronics','electronics'],
    ['عقارات','Real Estate','real-estate'],
    ['ملابس','Fashion','fashion'],
    ['معدات صناعية','Industrial Equipment','industrial-equipment'],
    ['زراعة','Agriculture','agriculture'],
    ['مواد بناء','Building Materials','building-materials'],
    ['خدمات','Services','services']
  ];
  const cats = {};
  for (const [nameAr,nameEn,slug] of categoryData) {
    cats[slug] = await prisma.category.upsert({ where:{slug}, update:{}, create:{nameAr,nameEn,slug} });
  }

  const attrs = [
    ['brand','الماركة','Brand','text',null,null],['model','الموديل','Model','text',null,null],['year','سنة الصنع','Year','number',null,null],
    ['condition','الحالة','Condition','select',null,['جديد','مستعمل','مجدّد']],['color','اللون','Color','text',null,null],
    ['engine','المحرك','Engine','text',null,null],['mileage','الكيلومترات','Mileage','number','km',null],['drive','الدفع','Drive','select',null,['أمامي','خلفي','دفع رباعي']],['seats','عدد المقاعد','Seats','number',null,null],['vin','رقم الهيكل','VIN','text',null,null],
    ['fuel','الوقود','Fuel','select',null,['بنزين','ديزل','هجين','كهرباء']],['transmission','ناقل الحركة','Transmission','select',null,['أوتوماتيك','يدوي']],
    ['ram','RAM','RAM','number','GB',null],['storage','سعة التخزين','Storage','number','GB',null],['screen','الشاشة','Screen','text',null,null],['camera','الكاميرا','Camera','text',null,null],['battery','البطارية','Battery','text',null,null],['warranty','الضمان','Warranty','text',null,null],
    ['propertyType','نوع العقار','Property Type','select',null,['شقة','فيلا','أرض','محل','مكتب']],['listingType','نوع العرض','Listing Type','select',null,['للبيع','للإيجار']],['area','المساحة','Area','number','m²',null],['rooms','عدد الغرف','Rooms','number',null,null],['bathrooms','عدد الحمامات','Bathrooms','number',null,null],['floor','الطابق','Floor','number',null,null],['age','عمر العقار','Age','number','سنة',null],['parking','المواقف','Parking','number',null,null],['finishing','التشطيب','Finishing','select',null,['فاخر','جيد','متوسط','عظم']],
    ['power','القدرة','Power','number','kW',null],['voltage','الجهد','Voltage','number','V',null],['capacity','الإنتاجية','Capacity','number',null,null],['moq','الحد الأدنى للطلب','MOQ','number',null,null],['origin','بلد المنشأ','Origin','text',null,null]
  ];
  const attrMap = {};
  for (const [key,nameAr,nameEn,type,unit,options] of attrs) {
    attrMap[key] = await prisma.attribute.upsert({
      where:{key}, update:{}, create:{key,nameAr,nameEn,type,unit,options}
    });
  }

  const links = [
    ['cars',['brand','model','year','condition','engine','mileage','drive','fuel','transmission','color','seats','vin']],
    ['electronics',['brand','model','ram','storage','screen','camera','battery','color','condition','warranty']],
    ['real-estate',['propertyType','listingType','area','rooms','bathrooms','floor','age','parking','finishing']],
    ['industrial-equipment',['brand','model','year','condition','power','voltage','capacity','moq','origin']],
    ['fashion',['brand','model','color','condition']],['agriculture',['brand','model','condition','capacity','origin']],['building-materials',['brand','model','origin','condition']]
  ];
  for (const [slug, keys] of links) for (let i=0;i<keys.length;i++) {
    await prisma.categoryAttribute.upsert({
      where:{categoryId_attributeId:{categoryId:cats[slug].id,attributeId:attrMap[keys[i]].id}},
      update:{sortOrder:i},
      create:{categoryId:cats[slug].id,attributeId:attrMap[keys[i]].id,required:i<2,sortOrder:i}
    });
  }

  const product = await prisma.product.findFirst({where:{sellerId:seller.id,titleEn:'Toyota Camry Hybrid 2022'}});
  if (!product) {
    const p = await prisma.product.create({
      data:{
        sellerId:seller.id, storeId:store.id, categoryId:cats.cars.id,
        titleAr:'تويوتا كامري هايبرد 2022', titleEn:'Toyota Camry Hybrid 2022',
        descriptionAr:'سيارة اقتصادية بحالة ممتازة، مناسبة للاستخدام اليومي.',
        descriptionEn:'Efficient vehicle in excellent condition.',
        price:18500,currency:'USD',stock:3,minOrderQty:1,status:'ACTIVE',
        location:'صنعاء',featured:true,shippingText:'الشحن حسب الموقع'
      }
    });
    const vals = [
      ['brand','Toyota'],['model','Camry'],['year','2022'],['condition','مستعمل'],
      ['fuel','هجين'],['transmission','أوتوماتيك'],['color','أبيض']
    ];
    for (const [key,value] of vals) await prisma.productAttribute.create({data:{productId:p.id,attributeId:attrMap[key].id,value}});
    await prisma.productImage.create({data:{productId:p.id,url:'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?auto=format&fit=crop&w=1200&q=80'}});
  }

  await prisma.cart.upsert({where:{userId:buyer.id},update:{},create:{userId:buyer.id}});
  await prisma.notification.create({data:{userId:buyer.id,title:'مرحباً بك في دولار',body:'ابدأ التسوق أو اطلب عرض سعر من الموردين.'}});

  console.log('Seed complete');
}
main().finally(()=>prisma.$disconnect());
