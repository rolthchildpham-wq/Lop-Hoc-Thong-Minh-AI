/* V5.2 SMART LEARNING PLATFORM
   Grade 5 + CEFR + HSK + Smart Practice/SRS + Test Center/Adaptive
   Original learning activities generated from syllabus metadata; no textbook pages copied.
*/
(function(){
'use strict';
const VERSION='5.2';
const STORE_KEY='ai_smart_learning_v52';
const HSK_DB='aihuedan_hsk_cache_v52';
const now=()=>Date.now();
const safeJSON=(s,d)=>{try{return JSON.parse(s)||d}catch(e){return d}};
const hash=s=>{let h=2166136261;for(const ch of String(s)){h^=ch.charCodeAt(0);h=Math.imul(h,16777619)}return h>>>0};
function seededShuffle(arr,seed){arr=arr.slice();let x=(seed>>>0)||1;for(let i=arr.length-1;i>0;i--){x=(Math.imul(x,1664525)+1013904223)>>>0;const j=x%(i+1);[arr[i],arr[j]]=[arr[j],arr[i]]}return arr}
function norm(s){return String(s||'').toLowerCase().replace(/\s+/g,' ').trim()}
function loadState(){return safeJSON(localStorage.getItem(STORE_KEY),{items:{},tests:[],daily:{date:'',done:0},settings:{dailyGoal:30}})}
let STATE=loadState();
function save(){try{localStorage.setItem(STORE_KEY,JSON.stringify(STATE))}catch(e){}}
function itemId(item){return item.smartId||item.bankId||item.lessonId||('q-'+hash([item.topic,item.question,item.word,item.hanzi].join('|')))}
function record(item,ok){if(!item)return;const id=itemId(item),s=STATE.items[id]||{seen:0,correct:0,wrong:0,mastery:0,lastSeen:0,due:0};s.seen++;s.lastSeen=now();if(ok){s.correct++;s.mastery=Math.min(5,s.mastery+1);const gaps=[0,6,24,72,168,336];s.due=now()+(gaps[s.mastery]||336)*3600000;}else{s.wrong++;s.mastery=Math.max(0,s.mastery-1);s.due=now()+10*60000;}STATE.items[id]=s;save();}
function weakest(limit=30){return Object.entries(STATE.items).sort((a,b)=>((a[1].mastery-a[1].wrong*.5)-(b[1].mastery-b[1].wrong*.5))||a[1].due-b[1].due).slice(0,limit)}
window.SmartSRS52={record,state:()=>STATE,weakest};

// ---------- GRADE 5: expand English to 600 activities ----------
function clone(x){return JSON.parse(JSON.stringify(x))}
function rotateOptions(base,shift){const opts=(base.options||[]).slice();const answer=opts[base.correct];if(!opts.length)return {opts,correct:0};const r=opts.map((_,i)=>opts[(i+shift)%opts.length]);return {opts:r,correct:r.indexOf(answer)} }
function makeG5Variant(base,unit,n){
 const b=clone(base),v=n%5,rot=rotateOptions(b,n%Math.max(1,(b.options||[]).length));
 const stem=b.question||b.summary||b.topic; const tag=['Core','Listening','Context','Review','Speed'][v];
 b.options=rot.opts;b.correct=rot.correct;b.smartId=`g5e-${unit}-${n+1}`;b.lessonId=`E${unit}`;b.activity=`${tag} Practice`;b.topic=`Unit ${unit} • ${b.topic||'English 5'}`;
 if(v===1){b.question=`🎧 Listen & choose: ${stem}`;b.summary=`Nghe cô giáo đọc câu hỏi và chọn đáp án phù hợp. ${b.summary||''}`;}
 if(v===2){b.question=`Trong tình huống giao tiếp của Unit ${unit}, ${stem}`;b.summary=`Áp dụng mẫu câu vào ngữ cảnh thực tế.`;}
 if(v===3){b.question=`Ôn tập Unit ${unit}: ${stem}`;b.summary=`Ôn lại kiến thức đã học trước khi chuyển bài.`;}
 if(v===4){b.question=`⚡ Quick check: ${stem}`;b.summary=`Kiểm tra phản xạ nhanh, không thay đổi mục tiêu kiến thức.`;}
 return b;
}
function expandGrade5English(){
 if(!window.GRADE5_BANK?.grade5_eng)return;
 const out=window.GRADE5_BANK.grade5_eng;
 for(let u=1;u<=20;u++){
   const k=`E${u}`,src=(out[k]||[]).filter(x=>!x.isTermTest);if(!src.length)continue;
   const bank=[];for(let i=0;i<30;i++)bank.push(makeG5Variant(src[i%src.length],u,i));out[k]=bank;
   if(window.GRADE5_COURSE?.grade5_eng){const node=GRADE5_COURSE.grade5_eng.find(x=>x.key===k);if(node)node.count=bank.length;}
 }
 // 40-question balanced semester tests, no duplicate smartId inside a paper.
 for(const term of [1,2]){
   const units=term===1?[1,2,3,4,5,6,7,8,9,10]:[11,12,13,14,15,16,17,18,19,20];
   const paper=[];let round=0;while(paper.length<40){for(const u of units){const a=out[`E${u}`]||[];const q=a[round%a.length];if(q&&!paper.some(x=>x.smartId===q.smartId))paper.push(Object.assign(clone(q),{activity:`Kiểm tra học kì ${term}`,isTermTest:true,smartId:`g5e-t${term}-${paper.length+1}`}));if(paper.length>=40)break}round++;if(round>50)break}
   out[`ET${term}`]=paper;if(window.GRADE5_META?.grade5_eng)GRADE5_META.grade5_eng[`ET${term}`]=`📝 KIỂM TRA HỌC KÌ ${term} · 40 câu`;
 }
 if(window.DATA_BANK)DATA_BANK.grade5_eng=out;
}
expandGrade5English();

// ---------- CEFR A1-C1: 3,900 activities ----------
const CEFR_COUNTS={A1:600,A2:700,B1:800,B2:900,C1:900};
const CEFR_UNITS={
 A1:['Meeting people','Family & home','Daily routines','School life','Food & drink','Time & dates','Shopping','Weather','Clothes','Hobbies','Town & places','Transport','Health basics','Abilities','Plans','Celebrations','Describing people','At a café','Travel basics','My world review'],
 A2:['Life experiences','Past events','Future plans','Health & advice','Work & study','Travel problems','Shopping choices','Technology','Home & neighbourhood','Food & cooking','Relationships','Entertainment','Weather events','Rules & obligations','Comparisons','Invitations','Services','Stories','Opinions','A2 project'],
 B1:['Personal goals','Education','Workplace','Travel experiences','Health & lifestyle','Media','Culture','Environment','Relationships','Problem solving','Technology','News','Consumer choices','Public services','Presentations','Storytelling','Opinions & reasons','Plans & decisions','Community','B1 project'],
 B2:['Debate & argument','Professional communication','Academic discussion','Media literacy','Global issues','Science & technology','Environment & policy','Culture & identity','Negotiation','Meetings','Reports','Complex narratives','Critical reading','Public speaking','Ethics','Economics','Innovation','Social change','Research discussion','B2 project'],
 C1:['Academic & analytical language','Advanced presentations','Negotiation strategy','Global challenges','Critical synthesis','Research communication','Professional leadership','Policy discussion','Media discourse','Culture & nuance','Complex problem solving','Formal writing','Evidence & evaluation','Persuasion','Mediation','Intercultural communication','Strategic meetings','Abstract concepts','Rhetorical emphasis','C1 capstone']
};
const GRAMMAR={A1:['be','present simple','have/has','there is/are','can','imperatives'],A2:['past simple','present perfect basics','going to/will','comparatives','must/have to','first conditional'],B1:['present perfect','past continuous','modals','relative clauses','conditionals','reported speech'],B2:['passives','mixed conditionals','advanced modals','participle clauses','discourse markers','hedging'],C1:['inversion','cleft sentences','nominalisation','advanced conditionals','rhetorical devices','stance & hedging']};
const CAN={A1:'Can handle simple everyday exchanges',A2:'Can communicate in routine situations and describe experiences',B1:'Can deal with familiar situations and explain opinions',B2:'Can interact fluently and discuss complex topics',C1:'Can express ideas fluently, precisely and flexibly'};
const ACT_TYPES=['Vocabulary','Listening','Speaking','Dialogue','Grammar','Reading','Task','Interaction','Mediation','Review'];
function cefrActivity(level,unitIndex,n){
 const topic=CEFR_UNITS[level][unitIndex],grammar=GRAMMAR[level][(unitIndex+n)%GRAMMAR[level].length],type=ACT_TYPES[n%ACT_TYPES.length],serial=n+1;
 const scenario=['at school','with a friend','while travelling','in an online chat','in a public place','at work','during a project','in a meeting'][(unitIndex+n)%8];
 const canDo=`${CAN[level]} about ${topic.toLowerCase()}`;
 const templates={
  Vocabulary:[`Which expression best matches the topic “${topic}”?`,[`A useful ${topic.toLowerCase()} expression`,`An unrelated technical code`,`A random number only`,`No communication at all`],0],
  Listening:[`🎧 Which response best fits a short conversation about ${topic}?`,[`A clear relevant response about ${topic}`,`A response about an unrelated topic`,`Silence only`,`A list of letters`],0],
  Speaking:[`🎤 What is the best speaking task for ${topic}?`,[`Give a short relevant response ${scenario}`,`Copy a dictionary page`,`Avoid speaking completely`,`Spell random words only`],0],
  Dialogue:[`Complete the dialogue about ${topic}: choose the most natural next turn.`,[`A relevant follow-up question or answer`,`Change to an unrelated subject abruptly`,`Repeat one word ten times`,`End without responding`],0],
  Grammar:[`Which choice best practises ${grammar} in the context of ${topic}?`,[`Use ${grammar} to express a meaningful idea`,`Ignore grammar and copy symbols`,`Use only isolated letters`,`Choose an unrelated formula`],0],
  Reading:[`After reading a short text about ${topic}, what should you identify first?`,[`Main idea and supporting details`,`Only punctuation marks`,`The font name`,`A random number`],0],
  Task:[`Task ${serial}: what should the learner do to demonstrate the CEFR goal?`,[canDo,`Memorise spelling without context`,`Translate every word but never communicate`,`Skip the task`],0],
  Interaction:[`Which action shows effective interaction about ${topic}?`,[`Respond, ask a relevant follow-up, and clarify when needed`,`Give no response`,`Use an unrelated memorised sentence`,`Change subject immediately`],0],
  Mediation:[`Which mediation task fits ${topic}?`,[`Explain the key information clearly to another person`,`Copy text without understanding`,`Hide the main point`,`Replace the message with random words`],0],
  Review:[`Review ${topic}: which statement best describes the lesson goal?`,[canDo,`Learn unrelated symbols`,`Avoid all interaction`,`Memorise one isolated letter`],0]
 };
 const t=templates[type],sh=seededShuffle(t[1].map((x,i)=>({x,i})),hash(`${level}-${unitIndex}-${n}`));
 return {kind:'mcq',smartId:`cefr-${level}-u${unitIndex+1}-${n+1}`,level,unit:unitIndex+1,unitTitle:topic,type:`${level} • ${topic} • ${type}`,activity:type,situation:scenario,canDo,grammar,prompt:t[0],speak:t[0],options:sh.map(z=>z.x),correct:sh.findIndex(z=>z.i===t[2]),explain:`${level} goal: ${canDo}. Focus: ${grammar}.`};
}
function buildCefr(){
 const bank={};for(const level of Object.keys(CEFR_COUNTS)){const total=CEFR_COUNTS[level],per=Math.floor(total/20),extra=total%20,a=[];for(let u=0;u<20;u++){const count=per+(u<extra?1:0);for(let n=0;n<count;n++)a.push(cefrActivity(level,u,n));}bank[level]=a;}return bank;
}
window.CEFR_SMART_BANK=buildCefr();window.CEFR_SMART_COUNTS=CEFR_COUNTS;if(window.DATA_BANK)DATA_BANK.english=window.CEFR_SMART_BANK;
// Sequential CEFR engine; no automatic reshuffle/repeat.
try{getEnglishRoundItem=function(sub){const a=DATA_BANK.english[sub]||[];return a[questionIndex]||null;};}catch(e){}

// ---------- HSK 1-6 FULL VIRTUAL BANK (HSK 2.0 regular 2026) ----------
const HSK_URL=l=>`https://raw.githubusercontent.com/drkameleon/complete-hsk-vocabulary/main/wordlists/inclusive/old/${l}.json`;
const HSK_COUNTS={1:150,2:300,3:600,4:1200,5:2500,6:5000};
const HSK_TYPES=['meaning','pinyin','hanzi','tone','listening','speaking','context','review'];
function openDB(){return new Promise((resolve,reject)=>{const r=indexedDB.open(HSK_DB,1);r.onupgradeneeded=()=>r.result.createObjectStore('levels');r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error)})}
async function cacheGet(k){try{const db=await openDB();return await new Promise((res,rej)=>{const r=db.transaction('levels').objectStore('levels').get(k);r.onsuccess=()=>res(r.result);r.onerror=()=>rej(r.error)})}catch(e){return null}}
async function cacheSet(k,v){try{const db=await openDB();await new Promise((res,rej)=>{const tx=db.transaction('levels','readwrite');tx.objectStore('levels').put(v,k);tx.oncomplete=res;tx.onerror=()=>rej(tx.error)})}catch(e){}}
function hskWord(raw){const f=(raw.forms||[])[0]||{},tr=f.transcriptions||{};return {hanzi:raw.simplified||'',pinyin:tr.pinyin||'',numeric:tr.numeric||'',meaning:(f.meanings||[])[0]||'meaning unavailable',pos:(raw.pos||[]).join('/'),radical:raw.radical||''}}
function tonesOf(numeric){return String(numeric||'').split(/\s+/).map(x=>{const m=x.match(/([1-5])$/);return m?Number(m[1]):0}).filter(Boolean)}
function hskActivity(vocab,level,index){const wi=Math.floor(index/HSK_TYPES.length)%vocab.length,ti=index%HSK_TYPES.length,w=vocab[wi],type=HSK_TYPES[ti];const pick=k=>vocab[(wi+1+k*37)%vocab.length];const distract=[pick(0),pick(1),pick(2)];let question,options,answer,summary;
 summary=`${w.hanzi} • ${w.pinyin} • ${w.meaning}${w.pos?' • '+w.pos:''}`;
 if(type==='meaning'){question=`“${w.hanzi}” có nghĩa tham khảo nào phù hợp?`;options=[w.meaning,...distract.map(x=>x.meaning)];answer=w.meaning;}
 if(type==='pinyin'){question=`Pinyin đúng của “${w.hanzi}” là gì?`;options=[w.pinyin,...distract.map(x=>x.pinyin)];answer=w.pinyin;}
 if(type==='hanzi'){question=`Chọn Hán tự đúng cho “${w.pinyin}”.`;options=[w.hanzi,...distract.map(x=>x.hanzi)];answer=w.hanzi;}
 if(type==='tone'){const tones=tonesOf(w.numeric).join('-')||'0';question=`Mẫu thanh điệu của “${w.pinyin}” là gì?`;options=[tones,'1-1','2-3','4-4'].filter((x,i,a)=>a.indexOf(x)===i).slice(0,4);while(options.length<4)options.push(String(options.length+1));answer=tones;}
 if(type==='listening'){question=`🎧 Nghe “${w.hanzi}” rồi chọn Pinyin.`;options=[w.pinyin,...distract.map(x=>x.pinyin)];answer=w.pinyin;}
 if(type==='speaking'){question=`🎤 Luyện nói: chọn mục tiêu đúng trước khi ghi âm.`;options=[`${w.hanzi} — ${w.pinyin}`,`${distract[0].hanzi} — ${w.pinyin}`,`${w.hanzi} — ${distract[1].pinyin}`,`${distract[2].hanzi} — ${distract[2].pinyin}`];answer=options[0];}
 if(type==='context'){question=`Trong bài HSK ${level}, từ nào đang được luyện trong thẻ này?`;options=[w.hanzi,...distract.map(x=>x.hanzi)];answer=w.hanzi;}
 if(type==='review'){question=`Cặp Hán tự–Pinyin nào đúng?`;options=[`${w.hanzi} — ${w.pinyin}`,`${w.hanzi} — ${distract[0].pinyin}`,`${distract[1].hanzi} — ${w.pinyin}`,`${distract[2].hanzi} — ${distract[1].pinyin}`];answer=options[0];}
 const sh=seededShuffle(options.map((x,i)=>({x,i})),hash(`hsk-${level}-${index}`));return {smartId:`hsk${level}-${wi+1}-${type}`,bankId:`hsk${level}-${wi+1}-${type}`,topic:`HSK ${level} • ${type}`,hanzi:w.hanzi,pinyin:w.pinyin,vn:w.meaning,tones:tonesOf(w.numeric),summary,question,options:sh.map(z=>z.x),correct:sh.findIndex(z=>z.x===answer),answerText:answer,explain:`${w.hanzi} (${w.pinyin}) — ${w.meaning}.`,activity:type,wordIndex:wi+1,virtualTotal:vocab.length*HSK_TYPES.length};}
function virtualHsk(vocab,level){return new Proxy([],{get(t,p){if(p==='length')return vocab.length*HSK_TYPES.length;if(p==='__vocab')return vocab;if(/^\d+$/.test(String(p)))return hskActivity(vocab,level,Number(p));return Reflect.get(t,p)}})}
async function loadHskLevel(level){level=Number(level);let raw=await cacheGet('old-'+level);if(!raw){const bubble=document.getElementById('teacherBubble');if(bubble)bubble.innerHTML=`⏳ Đang nạp ngân hàng HSK ${level} đầy đủ…`;const r=await fetch(HSK_URL(level),{cache:'force-cache'});if(!r.ok)throw new Error('HSK dataset HTTP '+r.status);raw=await r.json();cacheSet('old-'+level,raw)}const vocab=raw.map(hskWord).filter(x=>x.hanzi&&x.pinyin);const proxy=virtualHsk(vocab,level);DATA_BANK.chinese_hsk[level]=proxy;window.HSK_FULL_VOCAB=window.HSK_FULL_VOCAB||{};window.HSK_FULL_VOCAB[level]=vocab;return proxy;}
window.HSKFull52={loadLevel,counts:HSK_COUNTS,activityTypes:HSK_TYPES,source:'complete-hsk-vocabulary (MIT/CC-CEDICT definitions)'};

// ---------- Global progression guard: advance, never loop ----------
const oldLoadQuestion=window.loadQuestion;
window.loadQuestion=function(){
 try{
  if(appMode==='english'){
    const list=DATA_BANK.english[currentSubCategory]||[];
    if(questionIndex>=list.length){const lv=['A1','A2','B1','B2','C1'],i=lv.indexOf(currentSubCategory);if(i>=0&&i<lv.length-1){currentSubCategory=lv[i+1];questionIndex=0;toggleLangModelUI();teacherSpeak(`Hoàn thành ${lv[i]}. Chuyển sang ${currentSubCategory}.`);}else if(i===lv.length-1){questionIndex=list.length-1;teacherSpeak('Chúc mừng! Em đã hoàn thành lộ trình CEFR A1 đến C1.');}}
  }
  if(appMode==='chinese_hsk'){
    const list=DATA_BANK.chinese_hsk[currentSubCategory]||[];
    if(list.length&&questionIndex>=list.length){const l=Number(currentSubCategory);if(l<6){currentSubCategory=l+1;questionIndex=0;loadHskLevel(currentSubCategory).then(()=>{toggleLangModelUI();oldLoadQuestion();teacherSpeak(`Hoàn thành HSK ${l}. Chuyển sang HSK ${l+1}.`)});return;}questionIndex=Math.max(0,list.length-1);teacherSpeak('Chúc mừng! Em đã hoàn thành lộ trình HSK 1 đến HSK 6.');}
  }
 }catch(e){console.warn('V5.2 progression guard',e)}
 return oldLoadQuestion();
};
// On selecting HSK, load full dataset asynchronously and resume same level.
const oldSetAppMode=window.setAppMode;
window.setAppMode=function(mode,sub,source='system'){
 const r=oldSetAppMode(mode,sub,source);
 if(mode==='chinese_hsk'){const lv=Number(currentSubCategory)||1;loadHskLevel(lv).then(()=>{questionIndex=0;window.loadQuestion();}).catch(e=>{console.warn('HSK full bank fallback',e);teacherSpeak('Chưa tải được ngân hàng HSK đầy đủ, cô dùng ngân hàng lõi hiện có.');});}
 return r;
};

// ---------- Smart answer telemetry ----------
try{const old=window.checkGrade5Answer;window.checkGrade5Answer=function(selected,correct){try{record(currentItem,selected===correct)}catch(e){}return old(selected,correct)}}catch(e){}
try{const old=window.chooseEnglishCefrOption;window.chooseEnglishCefrOption=function(selected,btn){try{record(currentItem,selected===currentItem.correct)}catch(e){}return old(selected,btn)}}catch(e){}


function injectPracticeHub(){
 const box=document.getElementById('hubPracticeBody');if(!box)return;
 if(box.querySelector('.smart-practice-v52'))return;
 const s=stats();const wrap=document.createElement('div');wrap.className='smart-practice-v52';wrap.innerHTML=`<div style="margin:14px 0;padding:14px;border:1px solid #5eead4;border-radius:16px;background:#ecfeff;color:#134e4a"><b>🧠 Smart Review V5.2</b><p style="margin:6px 0">SRS toàn hệ thống • ${s.due} mục đến hạn • ${s.accuracy}% chính xác • ${s.mastered} mục đã thành thạo</p><button style="border:0;background:#0f766e;color:white;border-radius:10px;padding:10px 14px;font-weight:800" onclick="LearningHub.close();SmartPlatform52.openCenter();setTimeout(()=>SmartPlatform52.startSmartReview(),100)">Ôn thông minh 20 câu</button></div>`;box.prepend(wrap);
}
function smartReviewPool(){
 const all=adaptivePool();const weakIds=new Set(weakest(100).map(x=>x[0]));const weak=all.filter(x=>weakIds.has(itemId(x)));const rest=all.filter(x=>!weakIds.has(itemId(x)));return seededShuffle([...weak,...rest],hash('smart-review'+new Date().toDateString())).slice(0,20);
}
function startSmartReview(){startTest('Practice Hub • Smart Review',smartReviewPool())}
const hubObserver=new MutationObserver(()=>{if(document.getElementById('learningHub')?.classList.contains('open'))setTimeout(injectPracticeHub,30)});
document.addEventListener('DOMContentLoaded',()=>{const hub=document.getElementById('learningHub');if(hub)hubObserver.observe(hub,{attributes:true,subtree:true,childList:true});});

// ---------- Test Center ----------
function collectGrade5(mode,term,count){const key=(mode==='grade5_math'?'MT':mode==='grade5_viet'?'VT':'ET')+term;const a=window.GRADE5_BANK?.[mode]?.[key]||[];return seededShuffle(a,hash(mode+term+now().toString().slice(0,8))).slice(0,count||a.length)}
function cefrTest(level,kind){const a=window.CEFR_SMART_BANK[level]||[],n=kind==='checkpoint'?20:kind==='progress'?30:50;return seededShuffle(a,hash(level+kind+new Date().toDateString())).slice(0,n)}
function hskTest(level,kind){const a=DATA_BANK.chinese_hsk[level]||[],n=kind==='mini'?10:kind==='unit'?25:({1:40,2:60,3:80,4:100,5:100,6:101}[level]||40),out=[];for(let i=0;i<n&&i<a.length;i++)out.push(a[(i*17)%a.length]);return out}
let TEST={items:[],i:0,score:0,title:''};
function startTest(title,items){if(!items||!items.length){alert('Ngân hàng bài kiểm tra chưa sẵn sàng.');return;}TEST={items,i:0,score:0,title};renderTest();}
function renderTest(){const body=document.getElementById('smartTestBody');if(!body)return;if(TEST.i>=TEST.items.length){const pct=Math.round(TEST.score/TEST.items.length*100);STATE.tests.push({title:TEST.title,score:TEST.score,total:TEST.items.length,pct,at:now()});save();body.innerHTML=`<div class="st-result"><h2>🏆 Hoàn thành</h2><b>${TEST.score}/${TEST.items.length} • ${pct}%</b><p>${pct>=80?'Nắm tốt kiến thức.':pct>=60?'Đạt mức cơ bản, nên ôn lỗi sai.':'Cần Practice Hub ôn lại các điểm yếu.'}</p><button onclick="SmartPlatform52.renderCenter()">Về Test Center</button></div>`;return;}const q=TEST.items[TEST.i],opts=q.options||[];body.innerHTML=`<div class="st-test-head"><b>${TEST.title}</b><span>${TEST.i+1}/${TEST.items.length}</span></div><div class="st-q"><small>${q.topic||q.type||''}</small><h3>${q.question||q.prompt||''}</h3>${opts.map((o,i)=>`<button onclick="SmartPlatform52.answerTest(${i})">${String.fromCharCode(65+i)}. ${o}</button>`).join('')}</div>`;}
function answerTest(i){const q=TEST.items[TEST.i],ok=i===Number(q.correct);record(q,ok);if(ok)TEST.score++;TEST.i++;renderTest();}
function adaptivePool(){const pools=[];['A1','A2','B1','B2','C1'].forEach(l=>pools.push(...(CEFR_SMART_BANK[l]||[]).slice(0,60)));['grade5_math','grade5_viet','grade5_eng'].forEach(m=>Object.values(GRADE5_BANK?.[m]||{}).forEach(a=>pools.push(...(a||[]).slice(0,3))));return seededShuffle(pools,hash('adaptive'+new Date().toDateString())).slice(0,30)}
function stats(){const learned=Object.values(STATE.items),seen=learned.reduce((n,x)=>n+x.seen,0),correct=learned.reduce((n,x)=>n+x.correct,0),mastered=learned.filter(x=>x.mastery>=4).length,due=learned.filter(x=>x.due&&x.due<=now()).length;return {seen,accuracy:seen?Math.round(correct/seen*100):0,mastered,due};}
function renderCenter(){const s=stats(),body=document.getElementById('smartTestBody');if(!body)return;body.innerHTML=`
 <div class="st-stats"><div><b>${s.seen}</b><span>Lượt làm</span></div><div><b>${s.accuracy}%</b><span>Chính xác</span></div><div><b>${s.mastered}</b><span>Đã thành thạo</span></div><div><b>${s.due}</b><span>Đến hạn SRS</span></div></div>
 <h3>🎒 Lớp 5</h3><div class="st-grid">${['grade5_math','grade5_viet','grade5_eng'].map((m,mi)=>`<div class="st-card"><b>${['Toán 5','Tiếng Việt 5','Tiếng Anh 5'][mi]}</b><button onclick="SmartPlatform52.startGrade5('${m}',1)">HK1</button><button onclick="SmartPlatform52.startGrade5('${m}',2)">HK2</button></div>`).join('')}</div>
 <h3>🇬🇧 CEFR</h3><div class="st-grid">${['A1','A2','B1','B2','C1'].map(l=>`<div class="st-card"><b>${l}</b><small>${CEFR_COUNTS[l]} hoạt động</small><button onclick="SmartPlatform52.startCefr('${l}','checkpoint')">Checkpoint</button><button onclick="SmartPlatform52.startCefr('${l}','final')">Final</button></div>`).join('')}</div>
 <h3>🇨🇳 HSK</h3><div class="st-grid">${[1,2,3,4,5,6].map(l=>`<div class="st-card"><b>HSK ${l}</b><small>${HSK_COUNTS[l]} từ • ${HSK_COUNTS[l]*8} hoạt động ảo</small><button onclick="SmartPlatform52.startHsk(${l},'mini')">Mini</button><button onclick="SmartPlatform52.startHsk(${l},'mock')">Mock</button></div>`).join('')}</div>
 <h3>🧠 Adaptive</h3><div class="st-card wide"><p>30 câu lấy từ các phần yếu/đã học, dùng SRS mastery để ưu tiên nội dung cần ôn.</p><button onclick="SmartPlatform52.startAdaptive()">Bắt đầu Adaptive Test</button></div>`;}
function injectUI(){
 const style=document.createElement('style');style.textContent=`#smartTestCenter{position:fixed;inset:0;background:#08111eea;z-index:99999;display:none;align-items:center;justify-content:center;padding:18px}.st-panel{background:#f7fbff;color:#14213d;width:min(1100px,96vw);height:min(90vh,850px);overflow:auto;border-radius:22px;padding:20px}.st-top{display:flex;justify-content:space-between;align-items:center}.st-top button{font-size:22px;border:0;background:#e8eef8;border-radius:12px;padding:8px 12px}.st-stats,.st-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.st-stats div,.st-card{background:white;border:1px solid #dbe5f2;border-radius:16px;padding:14px}.st-stats b{font-size:24px;display:block}.st-stats span,.st-card small{color:#64748b}.st-card button,.st-result button{border:0;background:#0ea5a4;color:white;border-radius:10px;padding:9px 12px;margin:8px 6px 0 0;font-weight:700}.st-card.wide{grid-column:1/-1}.st-test-head{display:flex;justify-content:space-between;margin:15px 0}.st-q{background:white;padding:18px;border-radius:16px}.st-q button{display:block;width:100%;text-align:left;margin:9px 0;padding:13px;border-radius:11px;border:1px solid #cbd5e1;background:#f8fafc}.smart-test-btn{position:fixed;right:14px;bottom:92px;z-index:8000;border:0;border-radius:999px;background:#7c3aed;color:#fff;padding:12px 16px;font-weight:800;box-shadow:0 8px 28px #0005}@media(max-width:700px){.st-stats,.st-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.smart-test-btn{bottom:calc(84px + env(safe-area-inset-bottom,0px))}}`;
 document.head.appendChild(style);const modal=document.createElement('div');modal.id='smartTestCenter';modal.innerHTML=`<div class="st-panel"><div class="st-top"><div><h2>🧪 Test Center V5.2</h2><small>Grade 5 • CEFR • HSK • Adaptive</small></div><button onclick="SmartPlatform52.closeCenter()">✕</button></div><div id="smartTestBody"></div></div>`;document.body.appendChild(modal);const btn=document.createElement('button');btn.className='smart-test-btn';btn.innerHTML='🧪 Test Center';btn.onclick=openCenter;document.body.appendChild(btn);
}
function openCenter(){document.getElementById('smartTestCenter').style.display='flex';renderCenter()}
function closeCenter(){document.getElementById('smartTestCenter').style.display='none'}
async function startHsk(level,kind){try{await loadHskLevel(level)}catch(e){}startTest(`HSK ${level} • ${kind.toUpperCase()}`,hskTest(level,kind))}
function startGrade5(mode,term){startTest(`${{grade5_math:'Toán 5',grade5_viet:'Tiếng Việt 5',grade5_eng:'Tiếng Anh 5'}[mode]} • HK${term}`,collectGrade5(mode,term))}
function startCefr(level,kind){startTest(`CEFR ${level} • ${kind.toUpperCase()}`,cefrTest(level,kind))}
function startAdaptive(){startTest('Adaptive Test • Smart Review',adaptivePool())}

// Update visible program labels/counts.
function updateLabels(){try{const h=document.getElementById('appHeaderTitle');if(h&&appMode==='grade5_eng')h.innerText='🌟 TIẾNG ANH LỚP 5 • 20 UNITS • 600 HOẠT ĐỘNG';}catch(e){}}
setInterval(updateLabels,2000);
document.addEventListener('DOMContentLoaded',()=>{injectUI();updateLabels();});
window.SmartPlatform52={version:VERSION,CEFR_COUNTS,HSK_COUNTS,openCenter,closeCenter,renderCenter,answerTest,startGrade5,startCefr,startHsk,startAdaptive,startSmartReview,stats,loadHskLevel,state:()=>STATE};
})();