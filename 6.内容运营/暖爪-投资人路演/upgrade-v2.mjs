import fs from 'node:fs';
import path from 'node:path';

export function upgrade({slides,notes,note,start,head,foot,end,dir}) {
  const ai=start('ai-service','S13','light','three-forces',5,'核心卖点 / AI 客服')+head('已实现 / 双角色知识问答与人工接管','常见问题 AI 接住，<br>复杂问题人工接手。')+`<div class="three-forces" data-anim="up" style="display:grid;grid-template-columns:4fr 7fr;gap:3vw;flex:1;min-height:0;margin-top:3vh;margin-bottom:3vh"><div class="card-ink" style="padding:2.5vh 2vw;display:flex;flex-direction:column;justify-content:space-between"><div class="t-cat">一套入口，服务两类用户</div><div style="font-size:min(3.1vw,5.5vh);font-weight:200;line-height:1.35">宠主少等一次，<br>喂养师少问一次。</div><div style="display:grid;gap:1.5vh"><p class="body" style="opacity:1;color:var(--paper)">宠主：预约规则、服务范围、照护 FAQ</p><p class="body" style="opacity:1;color:var(--paper)">喂养师：接单档期、打卡、收益规则</p></div></div><div style="display:grid;grid-template-rows:repeat(3,1fr);gap:1.3vh">${[['01','按角色找答案','先识别宠主或喂养师，再检索对应知识，避免把内部规则答给错误对象。'],['02','回答有来源','引用实际知识文档；证据不够时说明无法确认，指向页面或人工。'],['03','人工接得上','用户可申请人工；工作人员接管后 AI 停止回复，处理复杂与争议问题。']].map(([n,t,d])=>`<article class="card-fill" style="padding:1.6vh 1.5vw;display:grid;grid-template-columns:3.5vw 1fr;gap:1vw"><div style="font-size:min(3vw,5.3vh);font-weight:200;color:var(--accent-ink)">${n}</div><div><h3 style="font-size:min(1.9vw,3.38vh);font-weight:300">${t}</h3><p class="body" style="margin-top:.7vh;opacity:1;color:var(--text-secondary)">${d}</p></div></article>`).join('')}</div></div>`+foot('价值验证：重复咨询是否减少、转人工是否顺畅。当前不查询实时订单，不直接执行退款、改约或医疗决策。')+end;
  const points=[{id:'home',name:'① 出门 / ④ 返回',meta:'宠主授权的起终点',x:17,y:66},{id:'walk',name:'② 步行节点',meta:'现场确认可通行',x:42,y:34},{id:'turn',name:'③ 折返点',meta:'按剩余时长返程',x:72,y:57,side:'left'}];
  const relations=[['home','walk'],['walk','turn'],['turn','home']];
  const route=start('walk-route','S08','hero dark','duo-mirror',6,'产品延伸 / 高德 MCP')+head('接入规划 / 待接入与实地验证','遛狗怎么走，<br>出门前就有一条路线。')+`<div class="history-map-grid" data-anim="up"><aside class="history-side"><div class="history-side-head"><div style="font-size:min(2.1vw,3.73vh);font-weight:300;line-height:1.3">地图路线＋逐段指引<br>喂养师确认后照路走</div><p class="body" style="margin-top:1vh;opacity:1;color:var(--accent-on)">把照护要求，转成可执行的出门计划。</p></div>${[['01','先定约束','宠主授权起终点，明确时长、宠物体能与避让要求。'],['02','再规划路线','高德 MCP 检索周边地点与步行路径，组合往返或环线。'],['03','确认后执行','查看距离、用时与分段指引；遇障碍或宠物不适及时调整。']].map(([n,t,d])=>`<div class="relation-card"><div class="nb">${n}</div><div><h3 class="ttl">${t}</h3><p class="desc">${d}</p></div></div>`).join('')}</aside><div class="map-panel"><div class="map-title"><div class="k">WALK PLAN / 概念示意</div><div class="t">从家出发，也把返程算进去</div></div><div class="map-controls" aria-label="路线示意图控制"><button class="map-ctrl" data-map-ctrl="zoom-in" aria-label="放大示意图">+</button><button class="map-ctrl" data-map-ctrl="zoom-out" aria-label="缩小示意图">−</button><button class="map-ctrl drag" data-map-ctrl="drag" aria-label="拖动示意图" aria-pressed="false">DRAG</button></div><div id="walk-map" class="swiss-map fallback-only" data-points='${JSON.stringify(points)}' data-relations='${JSON.stringify(relations)}'><div class="map-static"><svg class="static-relations" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true"><path d="M17 66 L17 48 L42 48 L42 34 L72 34 L72 57 L72 78 L17 78 Z" fill="none" stroke="var(--accent)" stroke-width=".65"/><path d="M17 52 l-1.3 3 h2.6 Z M55 34 l-3 -1.3 v2.6 Z M54 78 l3 -1.3 v2.6 Z" fill="var(--accent)"/></svg>${points.map(p=>`<div class="static-marker accent ${p.side==='left'?'left':''}" style="left:${p.x}%;top:${p.y}%"><span class="pin-dot"></span><span class="pin-line"></span><span class="pin-card"><span class="pin-name">${p.name}</span><span class="pin-meta">${p.meta}</span></span></div>`).join('')}</div></div><div class="t-meta" style="position:absolute;left:1.5vw;bottom:1.5vh;color:var(--ink);letter-spacing:.02em">路线示意 / 非真实导航 · 需现场核验宠物准入与通行条件</div></div></div>`+foot('现有基础：高德地址解析、订单点位排序。待接入：真实步行路线、预计用时与遛狗任务衔接。')+end;
  slides.splice(4,0,ai,route);
  notes.splice(4,0,
    note('ai-service','常见问题 AI 接住，复杂问题人工接手。','核心竞争力','展示已落地的咨询能力与人工责任边界。',['知识客服根据后端登录角色过滤知识，支持宠主和喂养师两个场景；账号间会话隔离。','检索采用向量与关键词融合，答案附实际引用；资料不足时不猜测政策。','用户申请人工后进入待接管状态，工作人员接管后模型停止回复；恢复 AI 会另起会话。','当前回答知识规则，不实时查询订单或执行退款改约；减少重复咨询的收益需要上线后验证。'],'除了回答问题，下一步让喂养师出门后也有执行路线。'),
    note('walk-route','遛狗怎么走，出门前就有一条路线。','核心竞争力','把高德 MCP 的价值落实为喂养师可执行的遛狗计划。',['本页是用户提出的新增产品规划，尚未接入高德 MCP。当前已有地址解析，AI 路线接口只排序订单点位，不查询实际道路。','计划输入为授权起终点、服务时长、宠物体能与避让要求；通过周边检索和步行路径查询组合往返或环线，并把返程纳入时长。','输出应包含地图、预计距离与用时、逐段指引；喂养师确认后执行，遇障碍或宠物异常时人工调整或中止。','地图仅为概念示意，无真实坐标。公共地点不自动等于允许宠物进入，仍需现场确认；不承诺已实现实时轨迹或绝对安全。','接入验收要检查 API 能力与费用、坐标一致性、超时回退、授权与定位数据保护，再做真实道路测试。'],'路线是执行辅助，服务信任还要靠完整的流程约束。'));
  notes.find(n=>n.id==='bounded-ai').transition='同样的边界意识，也体现在 AI 客服里。';
  notes.find(n=>n.id==='closing').talk[0]='投资命题是把信任机制做进服务流程，用已有 AI 派单和知识客服减少协调，并以高德 MCP 路线作为下一步执行辅助。';
  for(let i=0;i<slides.length;i++) slides[i]=slides[i].replace(/INVESTOR BRIEF · \d+ \/ 10/g,`INVESTOR BRIEF · ${String(i+1).padStart(2,'0')} / 12`).replace('10 / 10','12 / 12').replace('WHY WARM PAWS','WHY WARM PAW').replace('有边界的 AI 调度','有边界的 AI 协同').replace('处理偏好与协调复杂度，保留确定性规则和人工责任。','派单与客服已有实现；路线规划扩展执行辅助，保留人工责任。').replace('当前无可披露经营实绩。','高德 MCP 路线待接入；暂无经营实绩。');
  const brand=path.resolve(dir,'../../1.前期准备/暖爪-品牌设计/assets');
  for(const name of ['logo-horizontal.svg','logo-horizontal-reverse.svg']) fs.copyFileSync(path.join(brand,name),path.join(dir,'assets',name));
  const logo=`<span class="brand-logo"><img class="logo-normal" src="./assets/logo-horizontal.svg" alt="暖爪 Logo"><img class="logo-reverse" src="./assets/logo-horizontal-reverse.svg" alt="暖爪 Logo"></span>`;
  for(let i=0;i<slides.length;i++) slides[i]=slides[i].replace('暖爪 / WARM PAWS',logo);
  // Remove audience-facing footer annotations; detailed facts stay in speaker notes.
  for(let i=0;i<slides.length;i++) {
    slides[i]=removeFooterElements(slides[i]);
    slides[i]=slides[i].replace(/<section class="([^"]+)"/,(_,classes)=>'<section class="'+classes.split(/\s+/).filter(c=>!['dark','accent','hero'].includes(c)).concat('light').filter((c,i,a)=>a.indexOf(c)===i).join(' ')+'"')
      .replace(/<canvas class="ascii-bg"[^>]*><\/canvas>/g,'')
      .replaceAll('class="card-ink"','class="card-soft"');
    if(i===0||i===slides.length-1) slides[i]=slides[i].replaceAll('var(--accent-on)','var(--ink)').replaceAll('var(--accent-on-rgb)','var(--ink-rgb)');
  }
}

function removeFooterElements(markup) {
  const tags=/<\/?([a-z][a-z0-9-]*)\b[^>]*>/gi;
  const ranges=[];
  let token;
  while((token=tags.exec(markup))) {
    const opening=token[0];
    if(opening.startsWith('</')) continue;
    const footer=/data-anim="(?:foot|signature)"/.test(opening)
      || (/class="t-meta"/.test(opening) && (
        /data-anim="bottom"/.test(opening)
        || /position:absolute;left:1\.5vw;bottom:1\.5vh/.test(opening)
        || /display:flex;justify-content:space-between;color:var\(--accent-on\)/.test(opening)
      ));
    if(!footer) continue;
    const tag=token[1].toLowerCase();let depth=1,next;
    while((next=tags.exec(markup))) {
      if(next[1].toLowerCase()!==tag) continue;
      if(next[0].startsWith('</')) depth--;
      else if(!next[0].endsWith('/>')) depth++;
      if(!depth) {ranges.push([token.index,tags.lastIndex]);break;}
    }
  }
  const split=/class="slide split/.test(markup);
  for(const [from,to] of ranges.reverse()) markup=markup.slice(0,from)+(split?'<div aria-hidden="true" style="height:5vh;flex:0 0 auto"></div>':'')+markup.slice(to);
  return markup;
}

export const extraCSS=`
#deck .brand-logo{display:inline-block;position:relative;width:6.4vw;height:2.9vw;vertical-align:middle;margin-right:1vw}
#deck .brand-logo img{position:absolute;inset:0;width:100%;height:100%;object-fit:contain}
#deck .brand-logo .logo-reverse{display:none}
#deck .slide.dark .logo-normal,#deck .slide.accent .logo-normal,#deck .b-accent .logo-normal{display:none}
#deck .slide.dark .logo-reverse,#deck .slide.accent .logo-reverse,#deck .b-accent .logo-reverse{display:block}
#deck .slide[data-slide-id="cover"] .brand-logo{width:14vw;height:6.3vw;margin-right:2vw}
#deck .slide[data-slide-id="closing"] .brand-logo{width:12vw;height:5.4vw}
#deck .chrome-min{align-items:center;margin-bottom:3vh;font-size:min(.875vw,1.556vh)}
#deck .chrome-min .l{display:flex;align-items:center;gap:.6vw}
.history-map-grid{display:grid;grid-template-columns:4.3fr 6.7fr;gap:2vw;flex:1;min-height:0;margin:3vh 0 2vh}
.history-side{display:grid;grid-template-rows:1.2fr repeat(3,1fr);gap:1vh;min-height:0}
.history-side-head{background:var(--accent);color:var(--accent-on);padding:1.7vh 1.5vw}
.relation-card{background:rgba(var(--paper-rgb),.08);padding:1.2vh 1.3vw;display:grid;grid-template-columns:auto 1fr;gap:1vw}
.relation-card .nb{font-family:var(--mono);font-size:min(1vw,1.78vh);color:var(--accent-display)}
.relation-card .ttl{font-size:min(1.5vw,2.66vh);font-weight:400;line-height:1.3}
.relation-card .desc{font-size:min(1.13vw,2vh);line-height:1.5;color:var(--text-secondary);margin-top:.5vh}
.map-panel{position:relative;background:var(--grey-1);overflow:hidden;min-height:0;color:var(--ink)}
.map-title{position:absolute;top:1.7vh;left:1.5vw;z-index:3;pointer-events:none}
.map-title .k{font-family:var(--mono);font-size:min(.875vw,1.556vh);color:var(--grey-3);letter-spacing:.1em}
.map-title .t{font-size:min(1.6vw,2.84vh);font-weight:400;margin-top:.6vh}
.map-controls{position:absolute;top:8vh;right:1.5vw;z-index:4;display:flex;gap:.4vw}
.map-ctrl{min-width:2vw;height:3.3vh;border:1px solid var(--ink);background:var(--paper);color:var(--ink);font-size:min(.8vw,1.42vh);cursor:pointer}
.map-ctrl.drag{min-width:3.8vw}.map-ctrl.active{background:var(--accent);color:var(--accent-on);border-color:var(--accent)}
.swiss-map{position:absolute;inset:0;overflow:hidden;touch-action:none}
.map-static{position:absolute;inset:0;background:repeating-linear-gradient(0deg,transparent 0 7vh,rgba(var(--ink-rgb),.07) 7vh 7.15vh),repeating-linear-gradient(90deg,transparent 0 6vw,rgba(var(--ink-rgb),.07) 6vw 6.08vw),var(--grey-1);transform-origin:center;user-select:none}
.static-relations{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}
.static-marker{position:absolute;transform:translate(-50%,-50%);width:0;height:0}
.pin-dot{position:absolute;left:-.35vw;top:-.35vw;width:.7vw;height:.7vw;background:var(--accent);border:.12vw solid var(--paper);border-radius:50%}
.pin-line{position:absolute;left:.4vw;top:0;width:1.1vw;height:1px;background:var(--ink)}
.pin-card{position:absolute;left:1.5vw;top:-2vh;background:var(--paper);padding:.8vh .65vw;white-space:nowrap}
.pin-name{display:block;font-size:min(1.25vw,2.22vh);line-height:1.25;color:var(--ink)}
.pin-meta{display:block;font-size:min(.9vw,1.6vh);line-height:1.3;color:var(--grey-3);margin-top:.4vh}
.static-marker.left .pin-line{left:auto;right:.4vw}.static-marker.left .pin-card{left:auto;right:1.5vw}
/* One consistent light canvas across all twelve pages. */
#deck{--orange-wash:color-mix(in srgb,var(--accent) 9%,var(--paper));--deck-accent-ink:var(--accent-ink);--deck-accent-display:var(--accent-display)}
#deck .slide,#deck .slide .canvas-card{background:var(--paper)!important;color:var(--ink)}
#deck .slide{--text-primary:var(--ink);--text-body:rgba(var(--ink-rgb),.86);--text-secondary:rgba(var(--ink-rgb),.68);--text-helper:rgba(var(--ink-rgb),.55);--line:rgba(var(--ink-rgb),.18);--line-strong:rgba(var(--ink-rgb),.35)}
#deck .brand-logo .logo-normal{display:block!important}
#deck .brand-logo .logo-reverse{display:none!important}
#deck .card-soft{background:var(--grey-1);color:var(--ink)}
#deck .card-soft .body,#deck .card-soft .t-cat,#deck .on-dark{color:var(--text-secondary)!important}
#deck .stack-block:not(.b-accent){background:var(--grey-1)!important;color:var(--ink)!important}
#deck .stack-block.b-accent{background:var(--orange-wash);color:var(--ink)}
#deck .stack-block .layer-desc,#deck .stack-block .layer-tag{color:var(--text-secondary);opacity:1}
#deck .stack-block .layer-nb{color:var(--accent-ink);opacity:1}
#deck .split-half>.half.b-accent,#deck .split-half>.half.b-grey{background:var(--paper);color:var(--ink);--text-primary:var(--ink);--text-body:rgba(var(--ink-rgb),.86);--text-secondary:rgba(var(--ink-rgb),.68);--line:rgba(var(--ink-rgb),.18);--accent-ink:var(--deck-accent-ink);--accent-display:var(--deck-accent-display)}
#deck .split-half>.half.b-accent .chrome-min{color:var(--text-secondary)}
#deck .split-half>.half:first-child{border-right:1px solid var(--line)}
#deck .history-side-head{background:var(--orange-wash);color:var(--ink);--accent-on:var(--ink)}
#deck .relation-card{background:var(--grey-1)}
#deck .slide[data-slide-id="cover"] h1{color:var(--ink)}
#deck .slide[data-slide-id="cover"] h1 span{color:var(--accent-display)}
#deck .slide[data-slide-id="closing"] h2 span{color:var(--accent-display)}
`;

export const mapScript=`<script>
(()=>{const panel=document.querySelector('.map-panel');if(!panel)return;const el=panel.querySelector('.swiss-map'),layer=el.querySelector('.map-static');let scale=1,x=0,y=0,drag=false,last=null;const render=()=>layer.style.transform='translate('+x+'px,'+y+'px) scale('+scale+')';['wheel','pointerdown','pointermove','pointerup','click','dblclick','touchstart','touchmove'].forEach(type=>panel.addEventListener(type,e=>e.stopPropagation(),{passive:true}));panel.querySelectorAll('[data-map-ctrl]').forEach(button=>button.addEventListener('click',()=>{const action=button.dataset.mapCtrl;if(action==='drag'){drag=!drag;button.classList.toggle('active',drag);button.setAttribute('aria-pressed',String(drag));el.style.cursor=drag?'grab':'';}else{scale=Math.max(.8,Math.min(1.6,scale+(action==='zoom-in'?.1:-.1)));render();}}));el.addEventListener('pointerdown',e=>{if(drag){last=[e.clientX,e.clientY];el.setPointerCapture(e.pointerId)}});el.addEventListener('pointermove',e=>{if(!last)return;x=Math.max(-80,Math.min(80,x+e.clientX-last[0]));y=Math.max(-60,Math.min(60,y+e.clientY-last[1]));last=[e.clientX,e.clientY];render()});el.addEventListener('pointerup',()=>last=null);el.addEventListener('pointercancel',()=>last=null);})();
</script>`;
