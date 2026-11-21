'use strict';
const test=require('node:test'),assert=require('node:assert/strict');
const vm=require('node:vm'),fs=require('node:fs');
const src=fs.readFileSync('pronunciation-h151.js','utf8');
function harness(){
  const spoken=[];
  function SpeechSynthesisUtterance(text){this.text=text;}
  const native={speak:()=>true,speakSequence:()=>true};
  const context={
    window:{MultiVoiceManager:native,speechSynthesis:{
      getVoices:()=>[
        {name:'English low',lang:'en-US'},
        {name:'Microsoft Jenny Natural',lang:'en-US'},
        {name:'Mandarin Yunxi Neural',lang:'zh-CN'}
      ],
      cancel(){},
      speak(u){spoken.push(u)}
    }},
    document:{addEventListener(){}},
    localStorage:{getItem(){return null},setItem(){}},
    SpeechSynthesisUtterance,
    setInterval(){return 0},clearInterval(){},
    console
  };
  vm.runInNewContext(src,context,{timeout:1000});
  return {api:context.window.PronunciationH151,manager:native,spoken};
}
test('Pinyin uses Hanzi when supplied instead of reading Latin romanization',()=>{
  const {api}=harness();
  assert.equal(api.textForVoice('nǐ hǎo',{lang:'zh-CN',hanzi:'你好'}),'你好');
  assert.equal(api.textForVoice('nǐ hǎo',{lang:'zh-CN'}),'nǐ hǎo');
  assert.equal(api.textForVoice('hello',{lang:'en-US'}),'hello');
});
test('English IPA selects original English spelling when explicitly provided',()=>{
 const {api}=harness();
 assert.equal(api.textForVoice('/həˈloʊ/',{lang:'en-US',word:'hello'}),'hello');
});
test('English chooses natural en-US voice and Mandarin chooses zh-CN',()=>{
 const {manager,spoken}=harness();
 manager.speak('hello',{lang:'en-US'});
 manager.speak('你好',{lang:'zh-CN'});
 assert.equal(spoken[0].voice.name,'Microsoft Jenny Natural');
 assert.equal(spoken[1].voice.lang,'zh-CN');
 assert.equal(spoken[0].lang,'en-US');
 assert.equal(spoken[1].lang,'zh-CN');
});
test('sequential speech correctly routes Chinese and English rather than one language',()=>{
 const {manager,spoken}=harness();
 manager.speakSequence([{text:'hello',lang:'en-US'},{text:'nǐ hǎo',lang:'zh-CN',hanzi:'你好'}]);
 assert.equal(spoken[0].text,'hello');
 spoken[0].onend();
 assert.equal(spoken[1].text,'你好');
 assert.equal(spoken[1].lang,'zh-CN');
});
test('Vietnamese and native Android bridge remain untouched',()=>{
 const {manager,spoken}=harness();
 const ok=manager.speak('Xin chào',{lang:'vi-VN'});
 assert.equal(ok,true);
 assert.equal(spoken.length,0);
});
