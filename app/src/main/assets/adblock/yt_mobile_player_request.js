// Adapted from TBK/yt-mobile-filters (GPL-3.0-or-later)
// Rewrites m.youtube.com player/get_watch requests before playback so the
// streaming session is created without the ad slot that otherwise causes ads/stalls.
function ytMobilePlayerRequest(strategy) {
    const strategies = String(strategy || 'lactmilli').split('+');
    const isTarget = (url) => /\/youtubei\/v1\/(?:player|get_watch)(?:\?|$)/.test(url);

    const edit = (obj) => {
        if (!obj || typeof obj !== 'object') return obj;
        const pr = obj.playerRequest || obj;
        pr.playbackContext = pr.playbackContext || {};
        pr.playbackContext.contentPlaybackContext = pr.playbackContext.contentPlaybackContext || {};
        const c = pr.playbackContext.contentPlaybackContext;
        for (const s of strategies) {
            switch (s) {
                case 'lactmilli':
                    pr.params = '8AUB';
                    c.lactMilliseconds = String(Date.now());
                    break;
                case 'instream':
                    c.adPlaybackContext = { adType: 'AD_TYPE_INSTREAM' };
                    break;
                case 'yahi':
                    pr.params = 'YAHI';
                    break;
                case 'channel':
                    if (obj.context && obj.context.client) obj.context.client.clientScreen = 'CHANNEL';
                    break;
                case 'reloadxhr':
                    c.referer = String(c.referer || location.href).replace(/(?:#reloadxhr)?$/, '#reloadxhr');
                    break;
            }
        }
        return obj;
    };

    const decoder = new TextDecoder();
    const gunzip = (bytes) =>
        new Response(new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'))).arrayBuffer();
    const gzip = (text) =>
        new Response(new Blob([text]).stream().pipeThrough(new CompressionStream('gzip'))).arrayBuffer();

    const rewriteBody = async (bytes) => {
        const u8 = new Uint8Array(bytes);
        const isGzip = u8.length > 2 && u8[0] === 0x1f && u8[1] === 0x8b;
        const text = decoder.decode(isGzip ? await gunzip(u8) : u8);
        const out = JSON.stringify(edit(JSON.parse(text)));
        return isGzip ? gzip(out) : out;
    };

    const realFetch = window.fetch;
    const wrapped = function (resource, init) {
        try {
            const url = typeof resource === 'string' ? resource : (resource && resource.url) || '';
            if (!isTarget(url)) return realFetch.call(this, resource, init);
            if (init && typeof init.body === 'string') {
                const body = JSON.stringify(edit(JSON.parse(init.body)));
                return realFetch.call(this, resource, Object.assign({}, init, { body }));
            }
            if (resource instanceof Request && (!init || init.body === undefined)) {
                return resource.clone().arrayBuffer()
                    .then(rewriteBody)
                    .then((body) => realFetch.call(this, new Request(resource, Object.assign({}, init || {}, { body }))))
                    .catch(() => realFetch.call(this, resource, init));
            }
        } catch (e) {}
        return realFetch.call(this, resource, init);
    };
    try {
        Object.defineProperty(wrapped, 'name', { value: 'fetch' });
        wrapped.toString = () => 'function fetch() { [native code] }';
    } catch (e) {}
    window.fetch = wrapped;
}
