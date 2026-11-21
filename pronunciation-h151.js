/* H151. English / Mandarin learning pronunciation.
 * Preserves Vietnamese Korva voice and native Android Sherpa-ONNX bridge.
 * All replacements are explicit: never read Latin pinyin with an English voice.
 */
(() => {
  'use strict';
  if (window.PronunciationH151) return;
  const VOICE_PREF_KEY='aihuedan_h151_voice_preferences';
  const prefs=(()=>{
    try{return {...{en:'en-US',zh:'zh-CN',rate:.92},...JSON.parse(localStorage.getItem(VOICE_PREF_KEY)||'{}')}}
    catch{return {en:'en-US',zh:'zh-CN',rate:.92}}
  })();
  const isHanzi = text => /[\u3400-\u9fff]/u.test(String(text||''));
  const isIPA = text => /^\/?[ˈˌəɪæɑɔʊθðʃʒŋɜɚɒʌːa-z\s.]+\/?$/i.test(String(text||'').trim())
    && /[ˈˌəɪæɑɔʊθðʃʒŋɜɚɒʌː]/.test(String(text||''));
  const isPinyin = text => {
    const s=String(text||'').trim();
    return !isHanzi(s)&&/(?:[āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ]|[a-zü]+[1-5](?:\s|$))/i.test(s);
  };
  const language = lang => /^en(?:-|$)/i.test(lang||'')?'en':/^zh(?:-|$)/i.test(lang||'')?'zh':null;
  function textForVoice(text,opts={}){
    const raw=String(text||'').trim();
    const family=language(opts.lang);
    if(!family)return raw;
    if(family==='zh' && isPinyin(raw)){
      const hanzi=opts.hanzi||opts.chinese||opts.characters||opts.wordHanzi;
      // Do not invent lexical meaning from pinyin (many homophones exist).
      if(hanzi && isHanzi(hanzi))return String(hanzi).trim();
      return raw;
    }
    if(family==='en'&&isIPA(raw)){
      const word=opts.english||opts.word||opts.spelling;
      // IPA is notation, not orthographic text to be fed to a TTS model.
      if(word && !isIPA(word))return String(word).trim();
    }
    return raw;
  }
  function voiceScore(v,locale){
    if(!v||!v.lang)return -100000;
    const want=locale.toLowerCase(),got=v.lang.toLowerCase();
    const base=want.split('-')[0],baseGot=got.split('-')[0];
    if(baseGot!==base)return -100000;
    let score=got===want?100:got.startsWith(base+'-')?50:10;
    const name=(v.name||'').toLowerCase();
    if(/natural|neural|premium|enhanced|xiaoxiao|yunxi|jenny|aria|samantha|google/.test(name))score+=35;
    if(v.localService)score+=3;
    if(/robot|compact|espeak/.test(name))score-=20;
    return score;
  }
  function chooseVoice(locale,voices){
    return [...(voices||[])].sort((a,b)=>voiceScore(b,locale)-voiceScore(a,locale))[0]||null;
  }
  function chooseLanguage(opts={}){
    const family=language(opts.lang);
    if(!family)return opts.lang;
    if(family==='en')return opts.accent==='en-GB'||opts.accent==='en-US'?opts.accent:prefs.en;
    return 'zh-CN';
  }
  function setPreferences(next={}){
    if(next.en==='en-GB'||next.en==='en-US')prefs.en=next.en;
    if(Number.isFinite(Number(next.rate)))prefs.rate=Math.max(.6,Math.min(1.2,Number(next.rate)));
    try{localStorage.setItem(VOICE_PREF_KEY,JSON.stringify(prefs))}catch{}
    return {...prefs};
  }
  let originalSpeak=null,originalSequence=null,installed=false;
  function install(){
    const manager=window.MultiVoiceManager;
    if(!manager || typeof manager.speak!=='function')return false;
    if(manager.__h151Installed)return true;
    originalSpeak=manager.speak.bind(manager);
    originalSequence=typeof manager.speakSequence==='function'?manager.speakSequence.bind(manager):null;
    manager.speak=function(text,opts={}){
      const family=language(opts.lang);
      if(!family)return originalSpeak(text,opts);
      const next={...opts,lang:chooseLanguage(opts),rate:opts.rate??prefs.rate};
      const spoken=textForVoice(text,next);
      // AndroidVoice is still routed by the existing native WebView bridge.
      if(!window.AndroidVoice && 'speechSynthesis' in window){
        const voices=window.speechSynthesis.getVoices?.()||[];
        const voice=chooseVoice(next.lang,voices);
        if(voice)next.voice=voice;
      }
      return originalSpeak(spoken,next);
    };
    if(originalSequence){
      manager.speakSequence=function(parts,opts={}){
        const enhanced=(parts||[]).map(part=>{
          if(!part||!language(part.lang))return part;
          return {...part,text:textForVoice(part.text,part),lang:chooseLanguage(part),rate:part.rate??prefs.rate};
        });
        return originalSequence(enhanced,opts);
      };
    }
    manager.__h151Installed=true;installed=true;
    return true;
  }
  let attempts=0;
  if(!install()){
    const check=setInterval(()=>{if(install()||++attempts>100)clearInterval(check)},100);
    document.addEventListener('DOMContentLoaded',install,{once:true});
  }
  const api={install,isHanzi,isPinyin,isIPA,textForVoice,chooseVoice,chooseLanguage,
    setPreferences,get preferences(){return {...prefs}},get installed(){return installed}};
  window.PronunciationH151=Object.freeze(api);
})();
