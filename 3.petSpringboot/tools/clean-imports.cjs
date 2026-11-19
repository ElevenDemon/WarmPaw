const fs=require('fs'),path=require('path');const root='src/main/java/com/nuanzhua/pet';
const files=fs.readdirSync(root,{recursive:true}).filter(f=>f.endsWith('.java'));
const classes={};for(const f of files){const s=fs.readFileSync(path.join(root,f),'utf8');classes[path.basename(f,'.java')]=s.match(/package ([^;]+);/)[1];}
for(const f of files){const p=path.join(root,f);let s=fs.readFileSync(p,'utf8');const pkg=s.match(/package ([^;]+);/)[1];s=s.replace(/^import com\.nuanzhua\.pet\.[^;]+;\r?\n/gm,'').replace(/import static com\.nuanzhua\.pet\.[\w.]+\.([A-Z]\w*)\.(\w+);/g,(_,cls,fn)=>`import static ${classes[cls]}.${cls}.${fn};`);
const body=s.slice(s.indexOf('public '));const imports=Object.entries(classes).filter(([n,p])=>p!==pkg&&new RegExp('\\b'+n+'\\b').test(body)).map(([n,p])=>`import ${p}.${n};`).join('\n');
s=s.replace(/^(package [^;]+;)/m,'$1\n'+imports);fs.writeFileSync(p,s);}
