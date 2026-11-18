const MODEL = "gemini-3.8-flash";
const LIMIT = 25;
const WINDOW = 60;
const cors = {"Access-Control-Allow-Origin":"*","Access-Control-Allow-Methods":"GET,POST,OPTIONS","Access-Control-Allow-Headers":"Content-Type,x-device-id"};
export default {
 async fetch(request, env) {
  const url = new URL(request.url);
  if (request.method === "OPTIONS") return new Response(null,{headers:cors});
  if (url.pathname === "/health") return Response.json({ok:true,service:"trungtalk-proxy"},{headers:cors});
  if (request.method !== "POST" || !["/chat","/grammar","/evaluate"].includes(url.pathname)) return Response.json({error:"not_found"},{status:404,headers:cors});
  if (!env.GEMINI_API_KEY) return Response.json({error:"server_not_configured"},{status:503,headers:cors});
  const device = request.headers.get("x-device-id") || "anonymous";
  const bucket = Math.floor(Date.now()/1000/WINDOW);
  const key = "rate:"+device+":"+bucket;
  if (env.RATE_LIMIT) {
   const n = Number(await env.RATE_LIMIT.get(key) || "0");
   if (n >= LIMIT) return Response.json({error:"rate_limit"},{status:429,headers:cors});
   await env.RATE_LIMIT.put(key,String(n+1),{expirationTtl:120});
  }
  try {
   const payload = await request.json();
   const prompt = url.pathname === "/chat"
    ? (payload.systemPrompt || "Bạn là gia sư tiếng Trung, giải thích bằng tiếng Việt.") + "\n" + (payload.history || []).map(x => (x.role==="user"?"Học viên: ":"Gia sư: ")+x.text).join("\n") + "\nHọc viên: " + (payload.message || "")
    : (payload.prompt || "");
   if (!prompt.trim()) return Response.json({error:"empty_prompt"},{status:400,headers:cors});
   const upstream = await fetch("https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent?key="+env.GEMINI_API_KEY,{
    method:"POST",headers:{"Content-Type":"application/json"},
    body:JSON.stringify({contents:[{role:"user",parts:[{text:prompt}]}],generationConfig:{temperature:url.pathname==="/chat"?0.7:0.25,responseMimeType:url.pathname==="/chat"?"text/plain":"application/json"}})
   });
   const data = await upstream.json();
   if (!upstream.ok) return Response.json({error:"gemini_error",status:upstream.status},{status:502,headers:cors});
   const text = data.candidates?.[0]?.content?.parts?.map(p=>p.text||"").join("") || "";
   return Response.json({text},{headers:cors});
  } catch(e) { return Response.json({error:"server_error",message:String(e)},{status:500,headers:cors}); }
 }
};
