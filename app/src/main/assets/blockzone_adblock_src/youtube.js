(()=>{
  const hide=`#player-ads,ytd-ad-slot-renderer,ytd-display-ad-renderer,ytd-promoted-video-renderer,ytd-promoted-sparkles-web-renderer,ytd-in-feed-ad-layout-renderer,ytd-banner-promo-renderer,ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer,.ytp-ad-overlay-container,.ytp-ad-player-overlay,.video-ads,tp-yt-paper-dialog ytd-mealbar-promo-renderer{display:none!important}`;
  const style=document.createElement('style');
  style.textContent=hide;
  (document.documentElement||document).appendChild(style);

  // Keep YouTube from pausing only because the page became hidden/backgrounded.
  // This follows the same principle as Mozilla's open-source Video Background Play Fix.
  try {
    Object.defineProperty(document,'hidden',{configurable:true,get:()=>false});
    Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'visible'});
  } catch (_) {}
  document.addEventListener('visibilitychange',e=>e.stopImmediatePropagation(),true);
  window.addEventListener('pagehide',e=>e.stopImmediatePropagation(),true);
  setInterval(()=>{
    try {
      document.dispatchEvent(new Event('mousemove',{bubbles:true}));
      document.dispatchEvent(new Event('touchstart',{bubbles:true}));
    } catch (_) {}
  },45000);

  let seen=false;
  function clean(){
    let count=0;
    const selectors=['ytd-ad-slot-renderer','ytd-display-ad-renderer','ytd-promoted-video-renderer','ytd-promoted-sparkles-web-renderer','ytd-in-feed-ad-layout-renderer','ytm-promoted-sparkles-web-renderer','.ytp-ad-overlay-container','.ytp-ad-player-overlay'];
    selectors.forEach(sel=>document.querySelectorAll(sel).forEach(el=>{if(el.isConnected){el.remove();count++;}}));
    document.querySelectorAll('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-ad-skip-button-slot button').forEach(b=>{try{b.click();count++;}catch(_){}});

    // Fallback only: if YouTube still starts an ad after network/response filtering,
    // finish it quickly instead of leaving the viewer stuck on a black screen.
    const player=document.querySelector('.html5-video-player');
    if(player&&player.classList.contains('ad-showing')){
      const v=player.querySelector('video');
      if(v){
        try{
          v.muted=true;
          v.playbackRate=16;
          if(Number.isFinite(v.duration)&&v.duration>0)v.currentTime=Math.max(0,v.duration-.05);
        }catch(_){}
      }
      if(!seen){count++;seen=true;}
    }else seen=false;

    if(count)browser.runtime.sendMessage({type:'cosmeticBlock',count});
  }

  const observer=new MutationObserver(clean);
  observer.observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['class']});
  setInterval(clean,350);
  clean();
})();
