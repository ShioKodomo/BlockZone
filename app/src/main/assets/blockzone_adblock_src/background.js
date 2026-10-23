import { WebExtensionBlocker } from '@ghostery/adblocker-webextension';
const hardBlocks=['doubleclick.net','googleadservices.com','googlesyndication.com','pagead2.googlesyndication.com','adservice.google.com','googleads.g.doubleclick.net','youtube.com/api/stats/ads','youtube.com/pagead/','youtube.com/ptracking','youtube.com/get_midroll_info','youtube.com/api/stats/qoe?adformat='];
function notify(count=1){try{browser.runtime.sendNativeMessage('blockzone',{type:'blocked',count});}catch(e){}}
async function start(){
  try{const blocker=await WebExtensionBlocker.fromPrebuiltAdsAndTracking(); blocker.enableBlockingInBrowser(browser);}catch(e){console.error('BlockZone engine failed',e);}
  browser.webRequest.onBeforeRequest.addListener((details)=>{const u=(details.url||'').toLowerCase(); if(hardBlocks.some(x=>u.includes(x))){notify(); return {cancel:true};} return {};},{urls:['<all_urls>']},['blocking']);
  browser.runtime.onMessage.addListener(msg=>{if(msg&&msg.type==='cosmeticBlock')notify(msg.count||1);});
}
start();
