const MODEL = "gemini-3.8-flash";
const LIMIT = 25;
const WINDOW = 60;
const cors = {
 "Access-Control-Allow-Origin":"*",
 "Access-Control-Allow-Methods":"GET,POST,OPTIONS",
 "Access-Control-Allow-Headers":"Content-Type,x-device-id",
 "Access-Control-Max-Age":"86400"
};
const json = (body,status=200)=>Response.json(body,{status,headers:cors});
function buildPrompt(path,payload) {
 const teacher = "Bạn là TrungTalk, gia sư tiếng Trung/Anh cho người Việt. Luôn giải thích bằng tiếng Việt rõ ràng, ưu tiên ví dụ chữ Hán, Pinyin có dấu thanh và bản dịch. Không bịa nguồn.";
 if(path==="/chat") {
  const history=Array.isArray(payload.history)?payload.history.slice(-12).map(x=>(x.role==="user"?"Học viên: ":"Gia sư: ")+String(x.text||"").slice(0,2000)).join("\n"):"";
  return (payload.systemPrompt||teacher)+"\n"+history+"\nHọc viên: "+String(payload.message||"").slice(0,4000);
 }
 if(path==="/grammar") return teacher+" Hãy phân tích câu/từ sau theo: nghĩa, cấu trúc, lỗi thường gặp, 3 ví dụ kèm Pinyin và dịch. Trả JSON với keys explanation, examples, corrections. Nội dung: "+String(payload.prompt||"").slice(0,5000);
 if(path==="/evaluate") return teacher+" Hãy đánh giá câu trả lời học viên. Trả JSON gồm score 0-100, corrected, explanation_vi, better_answer, key_points. Đề bài và câu trả lời: "+String(payload.prompt||"").slice(0,5000);
 if(path==="/flashcard") return teacher+" Tạo 10 flashcard cho chủ đề "+String(payload.topic||"HSK 1")+"; trả JSON array, mỗi phần tử keys hanzi,pinyin,meaning_vi,example,example_pinyin,example_vi,category. Dùng từ phổ biến, chính xác.";
 return "";
}
export default {
 async fetch(request,env) {
  const url=new URL(request.url);
  if(request.method==="OPTIONS") return new Response(null,{status:204,headers:cors});
  if(url.pathname==="/health"&&request.method==="GET") return json({ok:true,service:"trungtalk-ai",model:MODEL,available:!!env.GEMINI_API_KEY});
  const routes=["/chat","/grammar","/evaluate","/flashcard"];
  if(request.method!=="POST"||!routes.includes(url.pathname)) return json({error:"not_found"},404);
  if(!env.GEMINI_API_KEY) return json({error:"server_not_configured"},503);
  let payload;
  try { payload=await request.json(); } catch { return json({error:"invalid_json"},400); }
  const prompt=buildPrompt(url.pathname,payload||{});
  if(!prompt.trim()) return json({error:"empty_prompt"},400);
  const device=(request.headers.get("x-device-id")||"anonymous").slice(0,100);
  const bucket=Math.floor(Date.now()/1000/WINDOW);
  if(env.RATE_LIMIT) {
   const key="rate:"+device+":"+bucket;
   const count=Number(await env.RATE_LIMIT.get(key)||"0");
   if(count>=LIMIT) return json({error:"rate_limit",retry_after:WINDOW},429);
   await env.RATE_LIMIT.put(key,String(count+1),{expirationTtl:120});
  }
  try {
   const wantsJson=url.pathname!=="/chat";
   const upstream=await fetch("https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent?key="+encodeURIComponent(env.GEMINI_API_KEY),{
    method:"POST",headers:{"Content-Type":"application/json"},
    body:JSON.stringify({contents:[{role:"user",parts:[{text:prompt}]}],generationConfig:{temperature:url.pathname==="/chat"?0.65:0.25,maxOutputTokens:1800,...(wantsJson?{responseMimeType:"application/json"}:{})}})
   });
   const data=await upstream.json().catch(()=>({}));
   if(!upstream.ok) return json({error:"upstream_error",status:upstream.status,detail:data.error?.message||"Gemini request failed"},502);
   const text=data.candidates?.[0]?.content?.parts?.map(p=>p.text||"").join("").trim()||"";
   if(!text) return json({error:"empty_model_response"},502);
   return json({text});
  } catch(e) { return json({error:"server_error",message:String(e).slice(0,300)},500); }
 }
};
