import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const html=fs.readFileSync(path.join(root,'index.html'),'utf8');
const sections=[...html.matchAll(/<section class="([^"]+)"[^>]*data-slide-id="([^"]+)"[^>]*>/g)];
assert.equal(sections.length,12);
for(const s of sections){assert(s[1].split(/\s/).includes('light'));assert(!s[1].split(/\s/).some(x=>['dark','accent','hero'].includes(x)));}
const visible=html.slice(html.indexOf('<section class='),html.lastIndexOf('</section>')+10);
assert(!/data-anim="(?:foot|signature)"/.test(visible));
assert.equal((visible.match(/class="brand-logo"/g)||[]).length,12);
assert(visible.includes('常见问题 AI 接住'));
assert(visible.includes('接入规划 / 待接入与实地验证'));
for(const file of ['暖爪-投资人路演-12页-清爽版.html','暖爪-投资人路演-12页-统一浅色动态版.html'])assert.equal(fs.readFileSync(path.join(root,file),'utf8'),html);
const report={slides:12,uniformLight:true,logos:12,footerAnnotations:0,aliasesSynced:true,visualBrowserRerun:false};
fs.writeFileSync(path.join(root,'验收','uniform-style-check.json'),JSON.stringify(report,null,2));
console.log(JSON.stringify(report));
