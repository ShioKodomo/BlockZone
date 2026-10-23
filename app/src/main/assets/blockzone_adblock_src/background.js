import { WebExtensionBlocker } from '@ghostery/adblocker-webextension';

const hardBlocks = [
  'doubleclick.net',
  'googleadservices.com',
  'googlesyndication.com',
  'pagead2.googlesyndication.com',
  'adservice.google.com',
  'googleads.g.doubleclick.net',
  'youtube.com/api/stats/ads',
  'youtube.com/pagead/',
  'youtube.com/ptracking',
  'youtube.com/get_midroll_info',
  'youtube.com/api/stats/qoe?adformat='
];

const playerAdKeys = new Set([
  'adPlacements',
  'playerAds',
  'adSlots',
  'adBreakHeartbeatParams',
  'adBreakParams'
]);

function notify(count = 1) {
  try {
    browser.runtime.sendNativeMessage('blockzone', { type: 'blocked', count });
  } catch (_) {}
}

function stripPlayerAds(value) {
  let removed = 0;
  if (!value || typeof value !== 'object') return removed;

  if (Array.isArray(value)) {
    for (const item of value) removed += stripPlayerAds(item);
    return removed;
  }

  for (const key of Object.keys(value)) {
    if (playerAdKeys.has(key)) {
      delete value[key];
      removed++;
      continue;
    }
    removed += stripPlayerAds(value[key]);
  }
  return removed;
}

function attachPlayerResponseFilter(details) {
  if (!browser.webRequest.filterResponseData) return;

  try {
    const filter = browser.webRequest.filterResponseData(details.requestId);
    const chunks = [];

    filter.ondata = event => {
      chunks.push(new Uint8Array(event.data));
    };

    filter.onstop = () => {
      try {
        let size = 0;
        for (const chunk of chunks) size += chunk.byteLength;

        const joined = new Uint8Array(size);
        let offset = 0;
        for (const chunk of chunks) {
          joined.set(chunk, offset);
          offset += chunk.byteLength;
        }

        const decoder = new TextDecoder('utf-8');
        const encoder = new TextEncoder();
        const original = decoder.decode(joined);

        let prefix = '';
        let body = original;
        if (body.startsWith(")]}'")) {
          const nl = body.indexOf('\n');
          if (nl >= 0) {
            prefix = body.slice(0, nl + 1);
            body = body.slice(nl + 1);
          }
        }

        const json = JSON.parse(body);
        const removed = stripPlayerAds(json);
        if (removed > 0) {
          notify(removed);
          filter.write(encoder.encode(prefix + JSON.stringify(json)));
        } else {
          filter.write(joined);
        }
      } catch (_) {
        try {
          let size = 0;
          for (const chunk of chunks) size += chunk.byteLength;
          const joined = new Uint8Array(size);
          let offset = 0;
          for (const chunk of chunks) {
            joined.set(chunk, offset);
            offset += chunk.byteLength;
          }
          filter.write(joined);
        } catch (_) {}
      } finally {
        try { filter.close(); } catch (_) {}
      }
    };

    filter.onerror = () => {
      try { filter.close(); } catch (_) {}
    };
  } catch (_) {}
}

async function start() {
  try {
    const blocker = await WebExtensionBlocker.fromPrebuiltAdsAndTracking();
    blocker.enableBlockingInBrowser(browser);
  } catch (e) {
    console.error('BlockZone engine failed', e);
  }

  browser.webRequest.onBeforeRequest.addListener(
    details => {
      const u = (details.url || '').toLowerCase();

      if (u.includes('/youtubei/v1/player')) {
        attachPlayerResponseFilter(details);
      }

      if (hardBlocks.some(x => u.includes(x))) {
        notify();
        return { cancel: true };
      }

      return {};
    },
    { urls: ['<all_urls>'] },
    ['blocking']
  );

  browser.runtime.onMessage.addListener(msg => {
    if (msg && msg.type === 'cosmeticBlock') notify(msg.count || 1);
  });
}

start();
