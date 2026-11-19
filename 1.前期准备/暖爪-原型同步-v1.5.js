/* v1.5 · 2026-11-17 · 四端共用的交互补充；仅使用演示数据，不请求业务或模型服务。 */
(() => {
  'use strict';
  const $ = s => document.querySelector(s);
  const esc = s => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const button = (label, fn, cls = 'btn sm outline') => {
    const b = document.createElement('button'); b.className = cls; b.textContent = label;
    b.setAttribute('onclick', ''); // 兼容旧版画布的全局点击兜底。
    b.addEventListener('click', e => { e.stopPropagation(); fn(); }); return b;
  };
  const note = text => { const p = document.createElement('p'); p.className = 'sync-note'; p.textContent = text; return p; };
  function phone(id, title, back) {
    const p = document.createElement('div'); p.className = 'phone'; p.id = id;
    p.innerHTML = '<div class="status"><span>9:41</span><span>●●●</span></div><div class="nav"><span class="back"></span><span>'+esc(title)+'</span><span></span></div><div class="body"></div>';
    p.querySelector('.back').append(button('‹ 返回', back, 'sync-link')); return p;
  }
  function focus(id) { if ($('#'+id)) window.focusPhone($('#'+id)); }
  const style = document.createElement('style');
  style.textContent = '.sync-note{font-size:12px;line-height:1.7;color:var(--ink-2);margin:10px 0}.sync-stack{display:grid;gap:10px}.sync-link{border:0;background:transparent;color:var(--brand-deep);font:inherit;cursor:pointer}.sync-chat{height:280px;overflow:auto;margin:12px 0;overscroll-behavior:contain}.sync-topics{display:flex;flex-wrap:wrap;gap:6px;margin:12px 0}.sync-topics button{width:auto}.sync-field{display:block;font-size:12px;color:var(--ink-2);margin:12px 0}.sync-field input,.sync-field textarea{margin-top:6px;width:100%}.sync-badge{position:fixed;bottom:12px;left:12px;z-index:70;border:1px solid var(--line);border-radius:14px;background:#fff;padding:8px 12px;box-shadow:var(--shadow-sm);font-size:11px;color:var(--ink-2)}.sync-tip{background:var(--brand-soft);border-radius:12px;padding:12px;font-size:12px;line-height:1.7;margin:12px 0}.sync-chat .msgbubble{white-space:pre-wrap;overflow-wrap:anywhere}.sync-field textarea{min-height:110px;resize:vertical}';
  document.head.append(style);

  function chat(p, role) {
    const body = p.querySelector('.body');
    body.innerHTML = '<div class="ai-banner">✦ 暖爪服务助手</div><p class="sync-note">常见问题即时查看 · 复杂问题可转人工</p><div class="sync-topics"></div><div class="sync-chat" aria-live="polite"></div><p class="sync-note sync-status"></p><div class="sync-stack"><input class="input" maxlength="2000" placeholder="描述你的问题…" aria-label="客服问题"></div>';
    const log = body.querySelector('.sync-chat'), status = body.querySelector('.sync-status');
    let human = false;
    const bubble = (text, mine = false) => { const b = document.createElement('div'); b.className = 'msgbubble '+(mine?'out':'in'); b.textContent = text; log.append(b); log.scrollTop = log.scrollHeight; };
    const greet = () => bubble(role === 'SITTER' ? '你好，我是暖爪服务助手。可以帮你了解接单、档期、服务记录与照护注意事项。' : '你好，我是暖爪服务助手。可以帮你了解预约、地址、服务信息与照护注意事项。');
    const topics = role === 'SITTER' ? ['接单与档期','服务打卡','收益问题','遛狗照护'] : ['订单与派单','服务范围','服务价格','特殊照护'];
    function reply(q) {
      if (/门锁|密码|证件/.test(q)) return '请勿在聊天中发送门锁密码、证件号等敏感信息。授权信息请使用正式交接方式。\n参考：《客服隐私与功能边界》· 凭证保护 · v1.0（示例）';
      if (/呕吐|受伤|呼吸|急救/.test(q)) return '请尽快联系兽医；服务人员应记录观察情况并联系宠主与平台。客服不能作医疗诊断或代办送医。\n参考：《异常观察与安全求助》· 安全求助 · v1.0（示例）';
      if (/退款|改期|取消|投诉/.test(q)) return '我可以解释操作入口，目前无法代办退款、改期、取消或投诉。具体政策与订单资格需由人工核实。\n参考：《订单售后与客服》· 售后边界（示例）';
      if (role === 'SITTER' && /接单|档期|打卡|收益|遛狗/.test(q)) return '接单前核对服务时间、地址、宠物和服务内容；档期由后台维护。按实际完成情况打卡、勾选清单并上传记录。收益疑问请提供订单编号联系人工核对。\n参考：《喂养师任务与记录排错》· 任务与记录 · v1.0（示例）';
      if (role === 'OWNER' && /订单|派单|价格|范围|特殊|地址/.test(q)) return '地址可通过地点搜索或定位回填，保存前补全楼栋门牌。服务价格与可预约范围请以实际页面和运营确认为准；特殊照护请填写备注。当前客服无法查询实时订单、报价或喂养师位置。\n参考：《地址档案与预约排错》· 地址与预约 · v1.0（示例）';
      return '现有资料不足以确认这个问题。你可以补充具体问题，或点击「联系人工客服」进一步核实。';
    }
    const send = q => {
      const input = body.querySelector('input'); const text = (q || input.value).trim(); if (!text) return;
      bubble(text, true); input.value = '';
      if (human) { status.textContent = '等待人工客服接入，留言已保存在本次演示会话中'; return; }
      bubble(reply(text));
    };
    topics.forEach(t => body.querySelector('.sync-topics').append(button(t, () => send(t))));
    body.querySelector('.sync-stack').append(button('发送', () => send(), 'btn sm'));
    body.querySelector('input').addEventListener('keydown', e => { if (e.key === 'Enter') send(); });
    const handoff = button('联系人工客服', () => {
      human = !human;
      if (human) { status.textContent = '正在等待人工客服接入'; handoff.textContent = '结束人工服务，恢复智能客服'; }
      else { log.replaceChildren(); greet(); status.textContent = '已开启新的智能客服会话'; handoff.textContent = '联系人工客服'; }
    });
    body.append(handoff, note('请勿发送门锁密码、证件号等敏感信息。'), note('原型说明：回复及引用为固定交互示例；正式客服按登录角色检索知识库，当前为 11 篇、73 条问答。'));
    greet();
  }

  function addresses() {
    const grid = $('#page-owner .grid'); if (!grid || $('#ow-addresses')) return;
    const p = phone('ow-addresses','服务地址', () => focus('ow-profile')); grid.append(p);
    const body = p.querySelector('.body'); let rows = [], editing = -1;
    try { rows = JSON.parse(localStorage.getItem('warm-paw-v15-addresses') || '[]'); } catch {}
    if (!Array.isArray(rows)) rows = [];
    const persist = () => { try { localStorage.setItem('warm-paw-v15-addresses',JSON.stringify(rows)); } catch {} };
    function render() {
      body.replaceChildren(); body.append(note('管理上门服务地址；修改档案不会改变已有订单的地址快照。'));
      rows.forEach((a,i) => { const c = document.createElement('div'); c.className='card sync-stack'; c.append(note(a.district+' · '+a.address),note(a.name+' '+a.phone),button('编辑地址',()=>edit(i))); body.append(c); });
      if (!rows.length) body.append(note('还没有服务地址，先添加一个吧。'));
      body.append(button('＋ 新增服务地址',()=>edit(-1),'btn'));
    }
    function edit(i) {
      editing = i; const a = rows[i] || {}; let selected = !!(a.lng && a.lat);
      body.innerHTML = '<div class="sync-tip">搜索地点回填所在区和坐标，再补充楼栋门牌。原型使用示例地点。</div><label class="sync-field">地点搜索<input class="input" data-f="search" placeholder="输入小区、街道或建筑名称"></label><div class="sync-stack" data-results></div>';
      const results = body.querySelector('[data-results]');
      const fields = [['name','联系人'],['phone','手机号码'],['district','所在区'],['address','详细地址'],['lng','经度'],['lat','纬度']];
      fields.forEach(([key,label]) => { const l=document.createElement('label'); l.className='sync-field'; l.innerHTML=esc(label)+'<input class="input" data-f="'+key+'" value="'+esc(a[key]||'')+'">'; body.append(l); });
      const set = (k,v) => { body.querySelector('[data-f="'+k+'"]').value=v; };
      const choose = () => { set('district','南山区');set('address','科技园示例小区');set('lng','113.9500');set('lat','22.5400');selected=true;results.replaceChildren(note('已回填示例地点，请补全楼栋门牌。')); };
      body.querySelector('[data-f="search"]').addEventListener('input',e => {
        results.replaceChildren(); if(e.target.value.trim().length<2)return;
        results.append(button('示例搜索结果 · 南山区科技园',choose));
      });
      body.append(button('使用当前位置（演示）',choose),note('定位未授权时可搜索选址；正式版由高德服务回填 GCJ-02 坐标。'));
      const feedback = note(''); feedback.setAttribute('role','status');body.append(feedback);
      body.append(button('保存地址',()=>{
        const next = {}; fields.forEach(([key])=>next[key]=body.querySelector('[data-f="'+key+'"]').value.trim());
        if(!selected){feedback.textContent='请先搜索并选择地点';return;}
        if(!next.name || !/^1\d{10}$/.test(next.phone) || !next.district || !next.address || !Number.isFinite(Number(next.lng)) || !Number.isFinite(Number(next.lat)) || Math.abs(Number(next.lng))>180 || Math.abs(Number(next.lat))>90){feedback.textContent='请补全联系人、11 位手机号、详细地址和有效坐标';return;}
        if(editing<0)rows.push(next);else rows[editing]=next;persist();render();
        document.querySelectorAll('[data-address-summary]').forEach(el=>el.textContent=next.district+' · '+next.address);
      },'btn'),button('返回地址列表',render));
    }
    render();
    ['#ow-profile .body','#ow-order1 .body'].forEach(s=>{const b=$(s);if(b){const entry=button('服务地址 · 管理 / 选址 ›',()=>focus('ow-addresses'));b.prepend(entry);if(s.includes('order1')){const n=note('请先确认上门服务地址');n.dataset.addressSummary='';b.prepend(n);if(rows.length)n.textContent=rows[0].district+' · '+rows[0].address;}}});
  }

  function preferences() {
    let values={};try{values=JSON.parse(localStorage.getItem('warm-paw-v15-preferences')||'{}');}catch{}
    const current = () => ($('#wp-pf-id')?.textContent || $('#wp-pf-m-id')?.textContent || 'S1001').trim();
    const mounts = [['#st-profileedit .body','mobile'],['#wp-addr-card','admin']];
    mounts.forEach(([selector,type])=>{
      const host=$(selector);if(!host)return;
      const box=document.createElement('div');box.className='card sync-preferences';
      box.innerHTML='<label class="sync-field">服务经验与偏好<textarea class="input" maxlength="1000" placeholder="如：有大型犬和怕生宠物照护经验；不承接未训练的攻击性犬只"></textarea></label><p class="sync-note">最多 1000 字。自述供特殊需求匹配参考，资质、档期与服务半径仍需通过平台校验。</p><span class="sync-note" role="status"></span>';
      const field=box.querySelector('textarea');
      function load(){const id=current();if(document.activeElement!==field)field.value=values[id]||'';}
      const save=()=>{values[current()]=field.value;try{localStorage.setItem('warm-paw-v15-preferences',JSON.stringify(values));}catch{}document.querySelectorAll('.sync-preferences textarea').forEach(el=>el.value=field.value);box.querySelector('[role="status"]').textContent='已保存当前喂养师的偏好（原型本地数据）';};
      box.append(button('保存服务偏好',save));host.prepend(box);load();
      const identity=$('#wp-pf-id')||$('#wp-pf-m-id');
      if(identity)new MutationObserver(load).observe(identity,{childList:true,subtree:true,characterData:true});
    });
  }
  function init() {
    const badge=document.createElement('div');badge.className='sync-badge';badge.textContent='v1.5 · 2026-11-17 · 原型演示';document.body.append(badge);
    if($('#ow-service')){
      const room=$('#ow-service').cloneNode(true);room.id='ow-room';room.querySelector('.nav').children[1].textContent='专属服务群';
      room.querySelector('.ai-banner').textContent='订单关联沟通 · 图文消息示例';
      room.querySelector('.grid')?.remove();
      room.querySelector('.nav .back').textContent='‹ 返回';room.querySelector('.nav .back').setAttribute('onclick','');room.querySelector('.nav .back').onclick=()=>focus('ow-servicing');
      $('#ow-service').parentElement.append(room);
      chat($('#ow-service'),'OWNER');$('#ow-service .nav').children[1].textContent='智能客服';
      document.querySelectorAll('#ow-servicing button').forEach(b=>{if(b.textContent.includes('专属服务群'))b.onclick=()=>focus('ow-room');});
    }
    const sitterGrid=$('#page-sitter .grid');
    if(sitterGrid){const p=phone('st-service','智能客服',()=>focus('st-profile'));sitterGrid.append(p);chat(p,'SITTER');
      document.querySelectorAll('#page-sitter .tabbar').forEach(bar=>bar.append(button('💬 客服',()=>focus('st-service'),'tb')));
      $('#st-profile .body')?.prepend(button('智能客服 ›',()=>focus('st-service')));
    }
    addresses();preferences();
    ['owner','sitter'].forEach(k=>{const page=$('#page-'+k);if(page){const count=page.querySelectorAll('.phone').length;const c=page.querySelector('.zone-head .cnt');if(c)c.textContent=count+' 屏';}});
    const dispatch=$('#wp-ai-top');if(dispatch){
      const n=note('派单口径 v1.5：先校验准入及历史低星评价；100 分＝熟悉宠物 40＋距离 30＋等级与平滑评分 20＋当前负载 10。正式大模型仅能选择后端合格候选，结合订单备注与服务偏好给出理由；模型故障保留待派并重试，人工兜底。此处为规则评分模拟。');dispatch.before(n);
      window.WarmPawDispatchFixture={familiarId:'',lowStarId:''};
      const scenario=document.createElement('div');scenario.className='sync-tip';scenario.append(note('历史记录演示：选择人员后重新判定；仅改变原型测试条件。'));
      [['familiarId','曾服务同一宠物'],['lowStarId','该宠主曾给 1–2 星']].forEach(([key,label])=>{
        const l=document.createElement('label');l.className='sync-field';l.textContent=label;
        const select=document.createElement('select');select.className='input';select.dataset.scenario=key;
        select.innerHTML='<option value="">无历史记录</option>';
        document.querySelectorAll('#wp-ai-result-body tr[data-ai-id]').forEach(row=>{const o=document.createElement('option');o.value=row.dataset.aiId;o.textContent=row.dataset.aiId+' · '+row.cells[0].textContent;select.append(o);});
        select.addEventListener('change',()=>{window.WarmPawDispatchFixture[key]=select.value;window.act(select,'adminAIEval');});
        l.append(select);scenario.append(l);
      });dispatch.before(scenario);
    }
    const support=$('#sec-service');if(support)support.prepend(note('当前实现：用户主动联系人工 → 待人工接入 → 客服接管；恢复智能客服会开启新会话。三轮自动升级属于后续目标，后台尚无知识库管理页面。'));
    const home=$('#page-home');if(home)home.prepend(note('产品愿景展示：官网价格为示例；真实支付、自动录像 / 直播、第三方消息与 AI 报告业务回写仍待接入。服务城市、保险、响应与赔偿政策待运营确认。'));
    document.querySelectorAll('.board-badge,.canvas-badge').forEach(b=>b.textContent=b.textContent.replace('19 屏','21 屏').replace('8 屏','9 屏'));
  }
  window.addEventListener('load', init);
})();
