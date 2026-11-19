const fs=require('fs'),path=require('path');
const root=path.resolve('src/main/java/com/nuanzhua/pet');
const files=fs.readdirSync(root,{recursive:true}).filter(f=>f.endsWith('.java') && f.includes(path.sep));
const target={};
for(const file of files){const n=path.basename(file,'.java'); target[n]=n.endsWith('Controller')?'controller':n.endsWith('Requests')?'dto':n==='Actor'||n==='Api'?'vo':n==='BusinessException'||n==='Errors'?'exception':n.endsWith('Mapper')||n==='BusinessSqlProvider'?'mapper':n==='AppUser'?'entity':n==='WebConfig'?'filter':n.endsWith('Service')?'service/impl':file.split(path.sep)[0];}
const imports=['common','config','controller','entity','mapper','service','dto','vo','enums','filter','exception'].map(x=>`import com.nuanzhua.pet.${x}.*;`).join('\n');
for(const file of files){let text=fs.readFileSync(path.join(root,file),'utf8'); const name=path.basename(file,'.java'); const dest=target[name];
text=text.replace(/^package [^;]+;/m,`package com.nuanzhua.pet.${dest.replaceAll('/','.')};`).replace(/^import com\.nuanzhua\.pet\.[^;]+;\r?\n/gm,'');
text=text.replace(/^(package [^;]+;)/m,'$1\n\n'+imports);
if(dest==='service/impl'){
 const methods=[];
 const re=/public\s+(static\s+)?([\w<>,?\[\] .]+?)\s+(\w+)\(([^)]*)\)\s*\{/g;let m;
 while((m=re.exec(text))){if(m[3]===name)continue;if(m[1]){let depth=1,end=re.lastIndex;while(depth&&end<text.length){if(text[end]==='{')depth++;if(text[end]==='}')depth--;end++;}methods.push(text.slice(m.index,end));}else methods.push(`${m[2]} ${m[3]}(${m[4]});`);}
 const javaImports=(text.match(/^import (?!com\.nuanzhua\.pet\.)[^;]+;/gm)||[]).join('\n');
 const iface=`package com.nuanzhua.pet.service;\n\n${imports}\n${javaImports}\n\npublic interface ${name} {\n${methods.map(x=>'    '+x).join('\n')}\n}\n`;
 fs.mkdirSync(path.join(root,'service'),{recursive:true});fs.writeFileSync(path.join(root,'service',name+'.java'),iface);
 text=text.replace(`public class ${name} {`,`public class ${name}Impl implements ${name} {`).replace(new RegExp(`public ${name}\\(`,'g'),`public ${name}Impl(`);
}
const out=path.join(root,dest,name+(dest==='service/impl'?'Impl':'')+'.java');fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,text);if(path.resolve(out)!==path.resolve(root,file))fs.unlinkSync(path.join(root,file));
}
for(const d of ['enums','aspect','entity','mapper'])fs.mkdirSync(path.join(root,d),{recursive:true});
for(const d of ['static','templates','mapper'])fs.mkdirSync(path.resolve('src/main/resources',d),{recursive:true});
console.log('Refactored',files.length,'classes.');
