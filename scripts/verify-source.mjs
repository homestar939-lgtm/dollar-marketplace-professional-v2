import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const required = [
  'apps/api/prisma/schema.prisma',
  'apps/api/src/server.js',
  'apps/api/src/routes/auth.js',
  'apps/api/src/routes/catalog.js',
  'apps/api/src/routes/commerce.js',
  'apps/api/src/routes/messaging.js',
  'apps/api/src/routes/seller.js',
  'apps/api/src/routes/admin.js',
  'apps/web/src/App.tsx',
  'apps/android/settings.gradle.kts',
  'apps/android/build.gradle.kts',
  'apps/android/app/build.gradle.kts',
  'apps/android/app/src/main/AndroidManifest.xml',
  'apps/android/app/src/main/java/com/dollar/marketplace/MainActivity.kt'
];
for (const file of required) if (!fs.existsSync(path.join(root, file))) throw new Error(`Missing required file: ${file}`);

const forbidden = /apps\/api\/src\/routes\/ai\.js|services\/ai\.service|\/ai\//i;
function walk(dir) {
  const out = [];
  for (const entry of fs.readdirSync(dir, {withFileTypes:true})) {
    if (['node_modules','.git','dist','build'].includes(entry.name)) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...walk(full)); else out.push(full);
  }
  return out;
}
for (const file of walk(root)) {
  if (!/\.(js|ts|tsx|kt|kts|md|json|yml|yaml|prisma)$/.test(file)) continue;
  const text = fs.readFileSync(file, 'utf8');
  if (forbidden.test(text)) throw new Error(`Forbidden AI route reference in ${path.relative(root,file)}`);
}

const rootPkg = JSON.parse(fs.readFileSync(path.join(root,'package.json')));
if (rootPkg.workspaces?.join('|') !== 'apps/api|apps/web') throw new Error('Unexpected workspace layout');
const apiPkg = JSON.parse(fs.readFileSync(path.join(root,'apps/api/package.json')));
const webPkg = JSON.parse(fs.readFileSync(path.join(root,'apps/web/package.json')));
if (apiPkg.dependencies['@prisma/client'] !== apiPkg.devDependencies.prisma) throw new Error('Prisma client/CLI versions differ');
if (webPkg.dependencies.react !== webPkg.dependencies['react-dom']) throw new Error('React/react-dom versions differ');

const androidBuild = fs.readFileSync(path.join(root,'apps/android/build.gradle.kts'),'utf8');
const androidApp = fs.readFileSync(path.join(root,'apps/android/app/build.gradle.kts'),'utf8');
if (!/com\.android\.application.*version "9\.4\.0"/.test(androidBuild)) throw new Error('AGP is not pinned to 9.4.0');
if (!/kotlin\.android.*version "2\.3\.21"/.test(androidBuild)) throw new Error('Kotlin Android plugin is not pinned to 2.3.21');
if (!/compose-bom:2026\.08\.00/.test(androidApp)) throw new Error('Compose BOM is not pinned to 2026.08.00');
if (!/ktor-client-android:3\.5\.2/.test(androidApp)) throw new Error('Ktor version mismatch');
if (!/coil-compose:3\.6\.2/.test(androidApp)) throw new Error('Coil version mismatch');

console.log('SOURCE VERIFY OK');
