(()=>{
  'use strict';

  const IS_YOUTUBE = /(^|\.)youtube\.com$/.test(location.hostname) || /(^|\.)youtube-nocookie\.com$/.test(location.hostname);
  if (!IS_YOUTUBE) return;

  // Same core technique used by Mozilla's open-source Video Background Play Fix:
  // keep Page Visibility API reporting the document as visible and prevent
  // visibilitychange handlers from pausing playback when Android backgrounds it.
  try {
    Object.defineProperties(document, {
      hidden: { configurable: true, get: () => false },
      visibilityState: { configurable: true, get: () => 'visible' }
    });
  } catch (_) {}

  try {
    const originalHasFocus = document.hasFocus && document.hasFocus.bind(document);
    Object.defineProperty(document, 'hasFocus', {
      configurable: true,
      value: () => true
    });
    void originalHasFocus;
  } catch (_) {}

  window.addEventListener('visibilitychange', event => event.stopImmediatePropagation(), true);
  window.addEventListener('pagehide', event => event.stopImmediatePropagation(), true);
  window.addEventListener('freeze', event => event.stopImmediatePropagation(), true);

  // Some YouTube variants track inactivity. Keep the page's activity timestamp alive
  // without touching the actual video timeline or advertisement state.
  const keepAlive = () => {
    try {
      window.dispatchEvent(new Event('mousemove'));
      document.dispatchEvent(new Event('mousemove', { bubbles: true }));
    } catch (_) {}
  };
  setInterval(keepAlive, 30000);
})();
