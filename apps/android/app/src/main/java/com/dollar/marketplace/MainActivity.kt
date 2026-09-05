package com.dollar.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*

private val RoyalBlack = Color(0xFF09090B); private val Gold = Color(0xFFD4AF37); private val Surface = Color(0xFF141416); private val Text = Color(0xFFF7F3E8); private val Muted = Color(0xFFA9A39A)

@Serializable data class Product(val id:String,val titleAr:String,val titleEn:String="",val price:Double,val currency:String="USD",val stock:Int=0,val minOrderQty:Int=1,val location:String?=null,val images:List<ProductImage> = emptyList(),val store:Store?=null,val category:Category?=null)
@Serializable data class ProductImage(val id:String,val url:String)
@Serializable data class Category(val id:String,val nameAr:String,val nameEn:String,val slug:String)
@Serializable data class Store(val id:String,val name:String,val rating:Double=0.0,val verified:Boolean=false)

class Api { private val client=HttpClient { install(ContentNegotiation){json(Json{ignoreUnknownKeys=true})} }; suspend fun products(q:String="",cat:String=""):List<Product>{ val url=buildString{append("${com.dollar.marketplace.BuildConfig.API_BASE_URL}/catalog/products?");if(q.isNotBlank())append("q=" + java.net.URLEncoder.encode(q,"UTF-8") + "&");if(cat.isNotBlank())append("category=$cat")}; return client.get(url).body() }; suspend fun categories():List<Category> = client.get("${com.dollar.marketplace.BuildConfig.API_BASE_URL}/catalog/categories").body() }

class MainActivity:ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{DollarApp()}} }

@Composable fun DollarApp(){ MaterialTheme(colorScheme=darkColorScheme(primary=Gold,onPrimary=RoyalBlack,background=RoyalBlack,surface=Surface,onBackground=Text,onSurface=Text,secondary=Gold)){ var tab by remember{mutableStateOf(0)}; var search by remember{mutableStateOf("")}; var cat by remember{mutableStateOf("")}; var products by remember{mutableStateOf<List<Product>>(emptyList())}; var categories by remember{mutableStateOf<List<Category>>(emptyList())}; val api=remember{Api()}; LaunchedEffect(Unit){runCatching{categories=api.categories();products=api.products()}}; LaunchedEffect(search,cat){if(search.length>1||cat.isNotBlank())runCatching{products=api.products(search,cat)}}; Scaffold(containerColor=RoyalBlack,bottomBar={NavigationBar(containerColor=Surface){listOf(Icons.Default.Home to "الرئيسية",Icons.Default.Search to "بحث",Icons.Default.Category to "الأقسام",Icons.Default.ShoppingCart to "السلة",Icons.Default.Person to "حسابي").forEachIndexed{i,p->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(p.first,null)},label={Text(p.second)})}}}){pad->Column(Modifier.fillMaxSize().padding(pad)){TopBar(search,{search=it});when(tab){0->Home(products,categories,{cat=it});1->SearchPage(search,products,{search=it});2->Categories(categories){cat=it;tab=0};3->Cart();4->Account()}}}}

@Composable fun TopBar(search:String,onSearch:(String)->Unit){Column(Modifier.padding(18.dp,14.dp,18.dp,8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("DOLLAR",color=Gold,fontSize=25.sp,fontWeight=FontWeight.Black);Text("MARKETPLACE",color=Muted,fontSize=10.sp,letterSpacing=2.sp)}IconButton(onClick={}){Icon(Icons.Default.Notifications,null,tint=Gold)}}OutlinedTextField(value=search,onValueChange=onSearch,modifier=Modifier.fillMaxWidth(),placeholder={Text("ابحث عن منتج، مورد أو شركة")},leadingIcon={Icon(Icons.Default.Search,null,tint=Gold)},singleLine=true,colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Gold,unfocusedBorderColor=Color(0xFF39393D)))}}

@Composable fun Home(products:List<Product>,categories:List<Category>,onCat:(String)->Unit){LazyColumn(contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){item{Hero()};item{Text("الأقسام الرئيسية",fontSize=20.sp,fontWeight=FontWeight.Bold);LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(categories.take(10)){c->AssistChip(onClick={onCat(c.slug)},label={Text(c.nameAr)},colors=AssistChipDefaults.assistChipColors(containerColor=Surface,labelColor=Text),border=AssistChipDefaults.assistChipBorder(true,Gold))}}};item{SectionTitle("منتجات مميزة")};items(products.take(12)){ProductCard(it)}}}
@Composable fun Hero(){Card(colors=CardDefaults.cardColors(containerColor=Surface),shape=MaterialTheme.shapes.extraLarge){Column(Modifier.padding(24.dp)){Text("سوقك العالمي يبدأ هنا",color=Gold,fontSize=28.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(8.dp));Text("تجارة متعددة البائعين • جملة وتجزئة • RFQ • تفاوض • شراء آمن",color=Text,fontSize=15.sp);Spacer(Modifier.height(18.dp));Button(onClick={},colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=RoyalBlack)){Text("استكشف السوق",fontWeight=FontWeight.Bold)}}}}
@Composable fun SectionTitle(s:String){Text(s,fontSize=20.sp,fontWeight=FontWeight.Bold)}
@Composable fun ProductCard(p:Product){Card(Modifier.fillMaxWidth().padding(bottom=4.dp),colors=CardDefaults.cardColors(containerColor=Surface),shape=MaterialTheme.shapes.large){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){if(p.images.isNotEmpty())AsyncImage(model=p.images.first().url,contentDescription=null,modifier=Modifier.size(92.dp).clip(MaterialTheme.shapes.medium));Column(Modifier.padding(start=14.dp).weight(1f)){Text(p.titleAr, fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);Text(p.store?.name?:"متجر موثوق",color=Muted,fontSize=12.sp);Spacer(Modifier.height(7.dp));Text("${p.price.toInt()} ${p.currency}",color=Gold,fontSize=20.sp,fontWeight=FontWeight.Black);Text("الحد الأدنى ${p.minOrderQty} • ${p.location?:"متاح للشحن"}",color=Muted,fontSize=11.sp)}};Icon(Icons.Default.ChevronLeft,null,tint=Gold)}}
@Composable fun SearchPage(search:String,products:List<Product>,onSearch:(String)->Unit){Column(Modifier.padding(18.dp)){Text("بحث متقدم",fontSize=25.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(12.dp));Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(true,onClick={},label={Text("الأحدث")});FilterChip(false,onClick={},label={Text("الأرخص")});FilterChip(false,onClick={},label={Text("الأقرب")});FilterChip(false,onClick={},label={Text("موثوق")})};Spacer(Modifier.height(14.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(products){ProductCard(it)}}}}
@Composable fun Categories(categories:List<Category>,onCat:(String)->Unit){LazyColumn(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("كل الأقسام",fontSize=26.sp,fontWeight=FontWeight.Black)};items(categories){c->ListItem(headlineContent={Text(c.nameAr,fontWeight=FontWeight.Bold)},supportingContent={Text(c.nameEn,color=Muted)},leadingContent={Icon(Icons.Default.Category,null,tint=Gold)},modifier=Modifier.clip(MaterialTheme.shapes.medium).clickable{onCat(c.slug)},colors=ListItemDefaults.colors(containerColor=Surface))}}}
@Composable fun Cart(){Column(Modifier.fillMaxSize().padding(22.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.ShoppingCart,null,tint=Gold,modifier=Modifier.size(54.dp));Text("السلة جاهزة للشراء",fontSize=22.sp,fontWeight=FontWeight.Bold);Text("أضف منتجات من عدة متاجر وسيتم تقسيمها تلقائياً إلى طلبات البائعين عند الدفع.",color=Muted)}}
@Composable fun Account(){LazyColumn(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("حسابي",fontSize=28.sp,fontWeight=FontWeight.Black)};item{ProfileCard()};item{Text("إدارة التجارة",fontSize=20.sp,fontWeight=FontWeight.Bold)};listOf("طلباتي","المفضلة والمقارنة","المحادثات والتفاوض","طلبات عروض الأسعار RFQ","النزاعات والاسترجاع","العناوين وطرق الدفع").forEach{s->item{ListItem(headlineContent={Text(s)},trailingContent={Icon(Icons.Default.ChevronLeft,null,tint=Gold)},colors=ListItemDefaults.colors(containerColor=Surface))}}}}
@Composable fun ProfileCard(){Card(colors=CardDefaults.cardColors(containerColor=Surface)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=MaterialTheme.shapes.extraLarge,color=Gold,modifier=Modifier.size(54.dp)){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.Person,null,tint=RoyalBlack)}};Column(Modifier.padding(start=14.dp)){Text("حساب المشتري",fontWeight=FontWeight.Bold,fontSize=18.sp);Text("ابدأ الشراء أو افتح متجرك",color=Muted)}}}}
