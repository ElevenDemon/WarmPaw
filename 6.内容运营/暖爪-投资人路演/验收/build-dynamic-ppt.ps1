$ErrorActionPreference = 'Stop'
$deckRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = Split-Path -Parent $deckRoot
$buildDir = Join-Path $PSScriptRoot 'dynamic-powerpoint'
$previewDir = Join-Path $buildDir 'preview'
New-Item -ItemType Directory -Path $buildDir,$previewDir -Force | Out-Null
$notes = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'presentation-notes.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$logo = Join-Path $outputRoot '..\1.前期准备\暖爪-品牌设计\assets\logo-horizontal.png'
$logo = [IO.Path]::GetFullPath($logo)
function RGB([string]$hex) {
    return [Convert]::ToInt32($hex.Substring(0,2),16) + 256*[Convert]::ToInt32($hex.Substring(2,2),16) + 65536*[Convert]::ToInt32($hex.Substring(4,2),16)
}
$paper=RGB 'FAFAF8'; $ink=RGB '0A0A0A'; $orange=RGB 'FF6B35'; $burnt=RGB 'C2410C'; $grey=RGB '666663'; $rule=RGB 'D8D8D4'; $soft=RGB 'F0F0ED'; $wash=RGB 'FFF0E8'
function Text([string]$value,[double]$x,[double]$y,[double]$w,[double]$h,[double]$size=18,[int]$color=$ink,[bool]$bold=$false) {
    $s=$script:slide.Shapes.AddTextbox(1,$x,$y,$w,$h)
    $s.Name='Text-'+[Guid]::NewGuid().ToString('N')
    $s.TextFrame.MarginLeft=0; $s.TextFrame.MarginRight=0; $s.TextFrame.MarginTop=0; $s.TextFrame.MarginBottom=0
    $s.TextFrame.WordWrap=-1
    $s.TextFrame.AutoSize=0
    $s.TextFrame.TextRange.Text=$value
    $s.TextFrame.TextRange.Font.Name='Microsoft YaHei'
    $s.TextFrame.TextRange.Font.NameFarEast='Microsoft YaHei'
    $s.TextFrame.TextRange.Font.Size=$size
    $s.TextFrame.TextRange.Font.Color.RGB=$color
    $s.TextFrame.TextRange.Font.Bold= $(if($bold){-1}else{0})
    $s.TextFrame.TextRange.ParagraphFormat.SpaceAfter=0
    $s.TextFrame.TextRange.ParagraphFormat.SpaceBefore=0
    $s.TextFrame2.AutoSize=0
    $s.Width=$w; $s.Height=$h
}
function Rect([double]$x,[double]$y,[double]$w,[double]$h,[int]$color=$soft) {
    $s=$script:slide.Shapes.AddShape(1,$x,$y,$w,$h)
    $s.Fill.ForeColor.RGB=$color; $s.Line.Visible=0
}
function Line([double]$x1,[double]$y1,[double]$x2,[double]$y2,[int]$color=$rule,[double]$weight=1) {
    $s=$script:slide.Shapes.AddLine($x1,$y1,$x2,$y2)
    $s.Line.ForeColor.RGB=$color; $s.Line.Weight=$weight
}
function BeginBlock { $script:blockStart=$script:slide.Shapes.Count+1 }
function Reveal([int]$trigger=3,[int]$effect=10,[double]$delay=0.08) {
    $last=$script:slide.Shapes.Count
    if($last -lt $script:blockStart){return}
    if($last -eq $script:blockStart){$shape=$script:slide.Shapes.Item($last)}
    else {
        $names=@(); for($n=$script:blockStart;$n -le $last;$n++){ $names+=$script:slide.Shapes.Item($n).Name }
        $shape=$script:slide.Shapes.Range([object[]]$names).Group()
    }
    $anim=$script:slide.TimeLine.MainSequence.AddEffect($shape,$effect,0,$trigger)
    $anim.Timing.Duration=0.38; $anim.Timing.TriggerDelayTime=$delay
}
function NewSlide([int]$n,[string]$section,[string]$title='',[string]$kicker='') {
    $script:slide=$script:pres.Slides.Add($n,12)
    $script:slide.Name=('{0:00}-{1}' -f $n,$notes[$n-1].id)
    $script:slide.FollowMasterBackground=0
    $script:slide.Background.Fill.ForeColor.RGB=$paper
    $pic=$script:slide.Shapes.AddPicture($logo,0,-1,46,25,76,34.25)
    $pic.Name='Warm Paw official logo'
    Text $section 142 38 500 22 11 $grey
    Text ('INVESTOR BRIEF    {0:00} / 12' -f $n) 738 38 180 22 10 $grey
    Line 48 76 912 76 $rule 0.65
    if($kicker){Text $kicker 48 92 864 20 11 $burnt}
    if($title){BeginBlock; Text $title 48 120 864 115 38 $ink; Reveal 2 10 0}
    $script:slide.SlideShowTransition.EntryEffect=3849
    $script:slide.SlideShowTransition.Duration=0.45
    $script:slide.SlideShowTransition.AdvanceOnClick=-1
    $script:slide.SlideShowTransition.AdvanceOnTime=0
    $note=$notes[$n-1]
    $script:slide.NotesPage.Shapes.Placeholders.Item(2).TextFrame.TextRange.Text=$note.title+"`r`n`r`n"+($note.talk -join "`r`n")+"`r`n`r`n转场："+$note.transition
}
function NumberedRow([string]$n,[string]$heading,[string]$desc,[double]$x,[double]$y,[double]$w,[double]$h=82) {
    BeginBlock
    Line $x $y ($x+$w) $y
    Text $n $x ($y+13) 40 36 24 $burnt
    Text $heading ($x+52) ($y+11) ($w-52) 34 23
    Text $desc ($x+52) ($y+48) ($w-52) ($h-45) 14.5 $grey
    Reveal
}
$app=$null; $script:pres=$null
try {
    $app=New-Object -ComObject PowerPoint.Application
    $app.DisplayAlerts=1
    $script:pres=$app.Presentations.Add(0)
    $script:pres.PageSetup.SlideWidth=960; $script:pres.PageSetup.SlideHeight=540

    NewSlide 1 '暖爪 / 宠物上门照护'
    BeginBlock
    Text '让离家的宠主，更放心一点。' 48 130 800 38 23 $grey
    Reveal 2 10 0
    BeginBlock
    Text '人不在家，' 44 198 860 94 68
    Reveal
    BeginBlock
    Text '照护不缺席。' 44 290 860 100 68 $orange
    Reveal
    BeginBlock
    Line 48 438 128 438 $orange 3
    Text '可信履约  /  AI 派单与客服  /  从深圳单区开始' 48 465 860 32 20 $grey
    Reveal

    NewSlide 2 '用户与机会' '离家之后，谁能让人放心？' '核心用户 / 出差、旅行中的城市养宠人'
    Line 480 220 480 496
    BeginBlock
    Text '现有选择' 48 212 390 25 12 $grey
    Text "找得到人，`n还要自己做判断。" 48 245 410 82 30
    Text "亲友帮忙、临时找人或寄养，`n宠主仍需反复确认可靠性。" 48 345 395 58 16 $grey
    Text "陌生人入户，筛选与交接成本高`n过程分散在聊天和照片里`n迟到、失联或异常，靠临时协调" 48 417 405 83 16 $grey
    Reveal
    BeginBlock
    Text '暖爪切入' 520 212 390 25 12 $burnt
    Text "把一次托付，`n变成可核对的服务。" 520 245 400 82 30 $burnt
    Text "从深圳单区的居家喂养开始，`n统一交接、派单和履约记录。" 520 345 390 58 16 $grey
    Text "审核喂养师，按资质和档期准入`n订单串起任务、照片和沟通记录`n异常进入运营接管与工单处理" 520 417 400 83 16 $grey
    Reveal

    NewSlide 3 '产品闭环' '一次服务，为下次托付积累信任。' '核心体验 / 让下一次托付更省心'
    $steps=@(@('01','预约交接','把宠物习惯、时间与特殊需求讲清楚。'),@('02','匹配熟人','优先同宠服务历史，再看距离与负载。'),@('03','履约留痕','按清单执行，保留照片与订单沟通。'),@('04','评价回流','争取复购，历史低星限制再次匹配。'))
    for($j=0;$j -lt 4;$j++){NumberedRow $steps[$j][0] $steps[$j][1] $steps[$j][2] 48 (204+$j*73) 432 70}
    BeginBlock
    $circle=$script:slide.Shapes.AddShape(9,609,235,224,224)
    $circle.Fill.Visible=0; $circle.Line.ForeColor.RGB=$orange; $circle.Line.Weight=1.6
    Text "熟悉，`n可以积累。" 642 301 180 92 31
    Text '交接' 700 204 60 28 16 $burnt
    Text '匹配' 846 334 60 28 16 $burnt
    Text '履约' 700 471 60 28 16 $burnt
    Text '反馈' 544 334 60 28 16 $burnt
    Reveal

    NewSlide 4 '核心卖点 / AI 派单' "合格，是底线。`n合适，才交给 AI。" '有边界的智能 / 已实现的调度机制'
    $cols=@(@('01 / RULES','先过准入','账号、资质、时段、半径、容量与时间冲突逐项过滤。','不能做的单，不进入候选。'),@('02 / AI MATCH','再选合适的人',"在合格名单内，结合订单备注、`n服务偏好和评分选择，`n并给出理由。",'经验与偏好，进入匹配过程。'),@('03 / OPERATIONS','异常有人接管','模型失败重试，拒单或超时重新派发，无合格人选由运营处理。','AI 失效，仍有负责人。'))
    for($j=0;$j -lt 3;$j++){
        $x=48+$j*296; BeginBlock
        Rect $x 249 272 250 $(if($j -eq 1){$wash}else{$soft})
        Text $cols[$j][0] ($x+20) 267 232 24 11 $burnt
        Text $cols[$j][1] ($x+20) 310 232 40 26
        Text $cols[$j][2] ($x+20) 365 232 91 16 $grey
        Text $cols[$j][3] ($x+20) 470 232 22 11.5 $burnt
        Reveal
    }

    NewSlide 5 '核心卖点 / AI 客服' "常见问题 AI 接住，`n复杂问题人工接手。" '知识客服 / 已有实现'
    BeginBlock
    Rect 48 249 298 250 $wash
    Text "宠主少等一次，`n喂养师少问一次。" 70 285 258 95 29 $burnt
    Text "宠主：预约规则与服务说明`n喂养师：接单规则与操作指引" 70 425 253 52 14.5 $grey
    Reveal
    NumberedRow '01' '按角色找答案' '识别宠主或喂养师，检索对应知识，避免内部规则答给错误对象。' 380 249 532 83
    NumberedRow '02' '回答有来源' '引用实际知识文档，证据不足时说明无法确认，指向页面或人工。' 380 335 532 83
    NumberedRow '03' '人工接得上' '用户可申请人工，工作人员接管后 AI 停止回复，处理复杂与争议问题。' 380 421 532 83

    NewSlide 6 '核心卖点 / 高德 MCP 遛狗路线' "遛狗怎么走，`n出门前就有一条路线。" '接入规划 / 待接入与实地验证'
    $routeSteps=@(@('01','先定约束',"授权起终点，明确服务时长、`n宠物体能与避让要求。"),@('02','再规划路线',"查询周边地点与步行路径，`n组合往返或环线。"),@('03','确认后执行',"距离、用时与分段指引可查，`n遇异常及时调整。"))
    for($j=0;$j -lt 3;$j++){NumberedRow $routeSteps[$j][0] $routeSteps[$j][1] $routeSteps[$j][2] 48 (248+$j*85) 318 83}
    Rect 408 249 504 250 $soft
    Text '地图路线 + 逐段指引' 430 266 452 28 20
    Text '喂养师确认后照路走' 430 301 452 24 13 $grey
    foreach($gx in @(455,530,605,680,755,830,890)){Line $gx 342 $gx 478 $rule 0.5}
    foreach($gy in @(345,385,425,465)){Line 431 $gy 890 $gy $rule 0.5}
    BeginBlock
    Line 472 443 472 365 $orange 4
    Line 472 365 651 365 $orange 4
    Text '① 出门 / ④ 返回' 430 468 185 24 13 $burnt
    Reveal 3 22
    BeginBlock
    Line 651 365 828 365 $orange 4
    Text '② 步行节点' 596 334 160 24 13 $burnt
    Reveal 3 22
    BeginBlock
    Line 828 365 828 443 $orange 4
    Line 828 443 472 443 $orange 4
    Text '③ 折返点' 776 468 130 24 13 $burnt
    Reveal 3 22

    NewSlide 7 '履约信任' '让“放心”有据可查。' '可核对的服务 / 把信任落实到每个环节'
    $trust=@(@('资格','谁能进门',"按服务类目核验资质，`n结合时段、服务半径`n与容量筛选。"),@('执行','事情有没有做完','清单承接服务要求，照片与媒体记录关联订单。'),@('评价','下次要不要再选','同宠完单历史参与评分，历史低星排除再次匹配。'),@('接管','出了问题谁处理','人工客服与应急工单承接，运营核实事实并协调。'))
    for($j=0;$j -lt 4;$j++){
        $x=48+$j*222; BeginBlock
        Line $x 244 ($x+198) 244 $orange 2
        Text $trust[$j][0] $x 272 198 65 42 $burnt
        Text $trust[$j][1] $x 360 198 34 18
        Text $trust[$j][2] $x 411 196 90 16 $grey
        Reveal
    }

    NewSlide 8 '交付与商业' '服务的三端，已有共同底座。' '产品能力 / 宠主、喂养师与运营协同'
    $mods=@(@('宠主端','预约、地址与订单流程'),@('喂养师端','档期、偏好与履约操作'),@('运营后台','审核、派单与客服接管'),@('派单引擎','准入、评分与大模型选择'),@('知识客服','按角色检索，回答带来源'),@('履约记录','清单、照片与订单消息'))
    for($j=0;$j -lt 6;$j++){
        $x=48+($j%3)*296; $y=230+[Math]::Floor($j/3)*144; BeginBlock
        Line $x $y ($x+272) $y
        Text ('0'+($j+1)) $x ($y+14) 272 23 12 $burnt
        Text $mods[$j][0] $x ($y+47) 272 40 27
        Text $mods[$j][1] $x ($y+95) 272 34 16 $grey
        Reveal
    }

    NewSlide 9 '商业逻辑' '先算清每一单，再放大每一区。' '经营主张 / 用实收与真实成本判断模式'
    BeginBlock
    Text "订单密度`n×`n稳定复购" 48 232 298 190 44 $burnt
    Text '把熟悉的服务，留在熟悉的片区。' 48 464 304 28 15 $grey
    Reveal
    NumberedRow '01' '以按次上门服务为主' '基础服务与合适的增值项目，会员需在复购成立后验证。' 380 230 532 86
    NumberedRow '02' '实收减去全部变动成本' '扣除喂养师结算、渠道佣金、补贴、支付及变动售后成本。' 380 324 532 86
    NumberedRow '03' '同区复购，减少重复投入' '观察路程、空档和每单人工协调时间，用实际数据检验效率。' 380 418 532 86

    NewSlide 10 '试点与增长' "从一个片区，`n做出可复制的经营单元。" '进入市场 / 先备供给，再开获客'
    $stops=@(@('限定服务区','深圳单区'),@('备齐可用供给','审核与档期'),@('触达真实需求','社区与门店'),@('完成首单','交接与回访'),@('验证后复制','复购与成本'))
    Line 66 303 882 303 $rule 1.5
    for($j=0;$j -lt 5;$j++){
        $x=48+$j*177; BeginBlock
        Rect ($x+14) 297 12 12 $orange
        Text ('0'+($j+1)) $x 260 160 28 15 $burnt
        Text $stops[$j][0] $x 327 170 30 18
        Text $stops[$j][1] $x 367 170 27 15 $grey
        Reveal
    }
    $growth=@(@('首单入口','社区、门店与出行前内容'),@('复购入口','完单回访与下次出行预约'),@('投入约束','按渠道核算实付与贡献回收'))
    for($j=0;$j -lt 3;$j++){
        $x=48+$j*296; BeginBlock; Line $x 429 ($x+272) 429
        Text $growth[$j][0] $x 443 272 27 16 $burnt
        Text $growth[$j][1] $x 475 272 25 14.5 $grey
        Reveal
    }

    NewSlide 11 '试点与投资判断'
    Line 480 115 480 499
    BeginBlock
    Text "先证明`n能放心托付，`n再证明`n值得重复购买。" 48 153 410 297 43
    Reveal 2 10 0
    Text '建议试点门槛 / 以下均为目标，非已达成' 520 107 396 25 11.5 $burnt
    $metrics=@(@('100 单','首月有效完单','核验需求、履约能力与投诉原因。'),@('95%','派单成功率目标','同时观察人工介入、迟到与投诉。'),@('30%','90 天内复购目标','按首批完单用户队列，完整窗口后判断。'))
    for($j=0;$j -lt 3;$j++){
        $y=153+$j*115; BeginBlock; Line 520 ($y-8) 912 ($y-8)
        Text $metrics[$j][0] 520 $y 174 64 47 $orange
        Text $metrics[$j][1] 705 ($y+15) 214 35 18
        Text $metrics[$j][2] 520 ($y+72) 392 30 13.5 $grey
        Reveal
    }

    NewSlide 12 '投资判断 / 下一步'
    BeginBlock
    Text "把放心，`n做成一门" 48 150 440 135 49
    Reveal 2 10 0
    BeginBlock
    Text "可重复的`n生意。" 48 285 440 142 49 $orange
    Text '从深圳一个片区开始。' 48 473 430 30 20 $grey
    Reveal
    NumberedRow '01' '信任机制已进入产品' "资格准入、履约留痕与评价反馈，`n共同约束一次服务。" 520 166 392 103
    NumberedRow '02' 'AI 的价值有清楚边界' '派单与客服已有实现，路线规划扩展执行辅助，保留人工责任。' 520 280 392 103
    NumberedRow '03' '先验证单区经营模型' '先投入供给、履约与试点数据，再决定扩区和融资节奏。' 520 394 392 103

    $pptx=Join-Path $outputRoot '暖爪-投资人路演-12页-动态版.pptx'
    $ppt=Join-Path $outputRoot '暖爪-投资人路演-12页-动态版.ppt'
    $script:pres.SaveAs($pptx,24)
    $script:pres.SaveAs($ppt,1)
    $script:pres.Close(); $script:pres=$null
    $script:pres=$app.Presentations.Open($ppt,-1,0,0)
    $pages=@(); $overflow=New-Object System.Collections.Generic.List[object]
    function CheckShape($shape,[int]$page) {
        if($shape.Type -eq 6){for($g=1;$g -le $shape.GroupItems.Count;$g++){CheckShape $shape.GroupItems.Item($g) $page};return}
        if($shape.HasTextFrame -eq -1 -and $shape.TextFrame.HasText -eq -1){
            $tr=$shape.TextFrame.TextRange
            if($tr.BoundHeight -gt $shape.Height+3 -or $tr.BoundWidth -gt $shape.Width+3){
                $overflow.Add([ordered]@{slide=$page;text=$tr.Text;height=$shape.Height;boundHeight=$tr.BoundHeight;width=$shape.Width;boundWidth=$tr.BoundWidth})
            }
        }
    }
    for($i=1;$i -le 12;$i++){
        $s=$script:pres.Slides.Item($i)
        $s.Export((Join-Path $previewDir ('{0:00}.png' -f $i)),'PNG',1600,900)
        for($k=1;$k -le $s.Shapes.Count;$k++){CheckShape $s.Shapes.Item($k) $i}
        $effects=@(); for($k=1;$k -le $s.TimeLine.MainSequence.Count;$k++){
            $e=$s.TimeLine.MainSequence.Item($k)
            $effects+=@{type=$e.EffectType;trigger=$e.Timing.TriggerType;duration=$e.Timing.Duration;shape=$e.Shape.Name}
        }
        if($effects.Count -eq 0){throw "Slide $i has no saved animations"}
        $pages+=@{slide=$i;shapes=$s.Shapes.Count;animations=$effects.Count;effects=$effects;transition=$s.SlideShowTransition.EntryEffect;background=$s.Background.Fill.ForeColor.RGB}
    }
    $signature=[BitConverter]::ToString([IO.File]::ReadAllBytes($ppt)[0..7])
    if($signature -ne 'D0-CF-11-E0-A1-B1-1A-E1'){throw 'Not a genuine binary PPT'}
    $report=[ordered]@{ppt=$ppt;pptx=$pptx;slides=$script:pres.Slides.Count;signature=$signature;pages=$pages;overflow=$overflow;verifiedIn=('Microsoft PowerPoint '+$app.Version)}
    $report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $buildDir 'validation.json') -Encoding UTF8
    Copy-Item -LiteralPath $ppt -Destination (Join-Path $outputRoot '暖爪-投资人路演-12页.ppt') -Force
    [ordered]@{ppt=$ppt;slides=12;animations=($pages | Measure-Object -Property animations -Sum).Sum;overflow=$overflow;preview=$previewDir} | ConvertTo-Json -Depth 5
} finally {
    if($script:pres){$script:pres.Close();[Runtime.InteropServices.Marshal]::ReleaseComObject($script:pres)|Out-Null}
    if($app){$app.Quit();[Runtime.InteropServices.Marshal]::ReleaseComObject($app)|Out-Null}
}
