const fs=require('fs'),path=require('path');const root='src/main/java/com/nuanzhua/pet';
const clean=s=>s.replace(/@(RequestAttribute|RequestBody|PathVariable|RequestHeader|RequestParam)(\((?:"[^"]*"|[^)])*\))?\s*/g,'').replace(/@Valid\s*/g,'');
for(const file of fs.readdirSync(root+'/controller').filter(f=>f.endsWith('Controller.java'))){
 const p=root+'/controller/'+file;const original=fs.readFileSync(p,'utf8');const name=file.slice(0,-5),service=name.replace('Controller','ApplicationService');
 let impl=original.replace('package com.nuanzhua.pet.controller;','package com.nuanzhua.pet.service.impl;\nimport com.nuanzhua.pet.service.'+service+';\nimport org.springframework.stereotype.Service;');
 impl=impl.replace(/@RestController\s*/g,'').replace(/@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping)\((?:"[^"]*"|[^)])*\)\s*/g,'');
 impl=clean(impl).replace(`public class ${name} {`,`@Service\npublic class ${service}Impl implements ${service} {`).replace(`public ${name}(`,`public ${service}Impl(`);
 const methods=[];const re=/public Api<\?> (\w+)\(([\s\S]*?)\)\s*\{/g;let m;let controller=original;
 const changes=[];
 while((m=re.exec(original))){const sig=m[0].slice(0,-1).trim();const params=clean(m[2]);const argNames=params.split(/,(?![^<]*>)/).map(s=>s.trim().split(/\s+/).pop()).filter(Boolean).join(',');
 let i=re.lastIndex,depth=1,quote=null,escape=false;
 while(depth&&i<original.length){const c=original[i++];if(quote){if(escape)escape=false;else if(c==='\\')escape=true;else if(c===quote)quote=null;}else if(c==='"'||c==="'")quote=c;else if(c==='{')depth++;else if(c==='}')depth--;}
 changes.push([re.lastIndex,i-1,` return application.${m[1]}(${argNames}); `]);methods.push(`    Api<?> ${m[1]}(${params});`);re.lastIndex=i;
 }
 // Keep endpoint annotations and signatures, replace bodies, then drop fields/constructors/private helpers by using original endpoint slices.
 const endpoints=[];const endpointRe=/@(GetMapping|PostMapping|PutMapping|DeleteMapping)\(/g;let match;
 while((match=endpointRe.exec(original))){const start=match.index;const bodyStart=changes.find(c=>c[0]>start);if(!bodyStart)break;let s=original.slice(start,bodyStart[0])+` return application.${methods[endpoints.length].match(/Api<\?> (\w+)/)[1]}(`;
 const sigPart=original.slice(start,bodyStart[0]);const pm=/public Api<\?> \w+\(([\s\S]*?)\)\s*\{/.exec(sigPart);const params=clean(pm[1]);const args=params.split(/,(?![^<]*>)/).map(x=>x.trim().split(/\s+/).pop()).filter(Boolean).join(',');
 endpoints.push(s+args+'); }');endpointRe.lastIndex=bodyStart[1]+1;
 }
 const importBlock=original.slice(0,original.indexOf('@RestController')).replace('package com.nuanzhua.pet.controller;','package com.nuanzhua.pet.controller;\nimport com.nuanzhua.pet.service.'+service+';');
 const classAnnotations=original.slice(original.indexOf('@RestController'),original.indexOf('public class'));
 controller=importBlock+classAnnotations+`public class ${name} {\n    private final ${service} application;\n    public ${name}(${service} application) { this.application=application; }\n    `+endpoints.join('\n    ')+'\n}\n';
 // Transaction annotations belong to the service implementation.
 controller=controller.replace(/@Transactional\s*/g,'');
 fs.writeFileSync(p,controller);fs.writeFileSync(root+'/service/impl/'+service+'Impl.java',impl);
 const imports=original.match(/^import [^;]+;/gm).join('\n');fs.writeFileSync(root+'/service/'+service+'.java',`package com.nuanzhua.pet.service;\n${imports}\n\npublic interface ${service} {\n${methods.join('\n')}\n}\n`);
}
console.log('Controllers now delegate to application services.');
